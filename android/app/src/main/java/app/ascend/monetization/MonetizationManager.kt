package app.ascend.monetization

import android.os.SystemClock
import android.util.Log
import app.ascend.BuildConfig
import app.ascend.analytics.AnalyticsTracker
import app.ascend.data.billing.Entitlement
import app.ascend.data.billing.EntitlementRepository
import app.ascend.data.local.ProfileRepository
import app.ascend.monetization.ads.AdsManager
import app.ascend.monetization.ads.RewardedFeature
import app.ascend.monetization.config.RcKeys
import app.ascend.monetization.config.RemoteConfig
import app.ascend.monetization.consent.ConsentManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * The ONE owner of every ad decision (CLAUDE.md rule 2). Screens ASK this; they
 * never touch the ad SDK directly. It:
 *
 *  - reads all caps / cooldowns / placement toggles from [RemoteConfig] — a
 *    missing full-screen key defaults OFF (rule 4);
 *  - enforces a single full-screen mutex across {app-open, interstitial,
 *    rewarded, paywall, purchase dialog, permission dialog} (rule 3);
 *  - suppresses ALL forced ads for paid users (rule 6);
 *  - applies per-format load timeouts and FAILS OPEN — an ad never blocks the
 *    core loop (rule 4 / spec "blocking" column);
 *  - returns typed [AdDecision] / [ShowOutcome] / [RewardOutcome] so callers
 *    collapse cleanly with no blank containers.
 *
 * The actual SDK load/show is delegated to [AdsManager] (currently a no-op until
 * AdMob unit IDs land); this class owns only policy + arbitration.
 */
@Singleton
class MonetizationManager @Inject constructor(
    private val rc: RemoteConfig,
    private val ads: AdsManager,
    private val consent: ConsentManager,
    private val entitlements: EntitlementRepository,
    private val profile: ProfileRepository,
    private val analytics: AnalyticsTracker,
    private val rewards: RewardLedger,
) {
    // Serializes reward cap check+increment so a double-tap can't double-spend (rule 5).
    private val unlockMutex = Mutex()
    // The single full-screen slot. Holds the id of whatever surface is on screen.
    private val fullScreenOwner = AtomicReference<String?>(null)

    // Interstitial pacing, session-scoped (resets each cold start).
    private val interstitialsShown = AtomicInteger(0)
    @Volatile private var lastFullScreenAtMs: Long = Long.MIN_VALUE

    // ---- App-open state (suppression zones + pacing). Timestamps are elapsedRealtime(). ----
    @Volatile private var lastExternalLinkAtMs: Long = Long.MIN_VALUE
    @Volatile private var lastRewardedAtMs: Long = Long.MIN_VALUE
    @Volatile private var lastPermissionAtMs: Long = Long.MIN_VALUE
    @Volatile private var lastAppOpenAtMs: Long = Long.MIN_VALUE
    @Volatile private var backgroundedAtMs: Long = Long.MIN_VALUE
    private val firstForeground = java.util.concurrent.atomic.AtomicBoolean(true)
    private val appOpensThisSession = AtomicInteger(0)
    private val activeFlows = java.util.concurrent.ConcurrentHashMap.newKeySet<AdFlow>()

    // ---- Splash / session-start interstitial state ----
    private val splashShownThisSession = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile private var lastSplashAtMs: Long = Long.MIN_VALUE
    private val _splashTransition = MutableStateFlow(false)
    /** True while a branded pre-ad transition surface (splash OR onboarding-complete) should overlay the app. */
    val splashTransition: StateFlow<Boolean> = _splashTransition.asStateFlow()

    // ---- Onboarding-complete interstitial state ----
    // When it last showed (forward-suppresses the next forced full-screen for an RC window).
    @Volatile private var lastOnboardingInterstitialAtMs: Long = Long.MIN_VALUE

    // App-lifetime scope so a full-screen present survives the caller's lifecycle
    // (e.g. an interstitial requested as a screen pops). The real ad shows over the
    // Activity, independent of any ViewModel scope.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Fire-and-forget interstitial present on the manager's own scope (for close/exit triggers). */
    fun requestInterstitial(placement: Placement) {
        scope.launch { presentInterstitial(placement) }
    }

    init {
        // Register the single ILRD sink. The real AdsManager attaches an
        // OnPaidEventListener to EVERY format and forwards each paid callback here
        // → one ad_impression per impression (rule 7).
        ads.setPaidListener(::onAdPaid)
    }

    /** Logs `ad_impression` for one ILRD paid callback (any format). */
    fun onAdPaid(event: AdPaidEvent) {
        analytics.adImpression(
            valueMicros = event.valueMicros,
            currency = event.currencyCode,
            adFormat = event.format.schemaName,
            adSource = event.adSource,
            adUnit = event.adUnitId,
            placementId = event.placementId,
            precision = event.precision,
        )
    }

    /** Fetch + activate Remote Config once per cold start (call at app start). */
    suspend fun refreshConfig() = rc.refresh()

    // ---- Native (inline) decisions, reactive for Compose ----

    /**
     * Decision for an inline native placement, recomputed when Pro status or the
     * consent gate changes. UIs render the ad on [AdDecision.Show] and COLLAPSE
     * the slot on [AdDecision.Suppressed] — never a blank container (rule 4).
     */
    fun nativeAd(placement: Placement): Flow<AdDecision> =
        combine(entitlements.entitlement, consent.canRequestAds) { ent, canRequest ->
            decideNative(placement, ent, canRequest)
        }

    private fun decideNative(placement: Placement, ent: Entitlement, canRequestAds: Boolean): AdDecision {
        if (!canRequestAds) return AdDecision.Suppressed(placement, SuppressReason.CONSENT_NOT_READY)
        if (ent.isUnknown) return AdDecision.Suppressed(placement, SuppressReason.ENTITLEMENT_UNKNOWN)  // no forced ads until resolved
        if (ent.isPro && suppressForPaid()) return AdDecision.Suppressed(placement, SuppressReason.PAID_USER)
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return AdDecision.Suppressed(placement, SuppressReason.GLOBAL_DISABLED)
        if (!rc.bool(placement.rcEnabledKey)) return AdDecision.Suppressed(placement, SuppressReason.RC_DISABLED)
        return AdDecision.Show(placement, loadTimeoutMs = 0L)   // native is non-blocking
    }

    /** How many organic rows between native job-list ads (RC `ads.native.job_list.frequency`). */
    fun nativeListFrequency(): Int = rc.long(RcKeys.NATIVE_JOB_LIST_FREQUENCY).toInt().coerceAtLeast(2)

    // ---- Full-screen decision (interstitial / app-open) ----

    /** Pure-policy decision for a full-screen placement; the present path re-checks the mutex atomically. */
    suspend fun decide(placement: Placement): AdDecision {
        val ent = entitlements.entitlement.first()
        val canRequestAds = consent.canRequestAds.value
        val session = profile.currentSessionNumber()

        if (!canRequestAds) return AdDecision.Suppressed(placement, SuppressReason.CONSENT_NOT_READY)
        if (ent.isUnknown) return AdDecision.Suppressed(placement, SuppressReason.ENTITLEMENT_UNKNOWN)
        if (ent.isPro && suppressForPaid()) return AdDecision.Suppressed(placement, SuppressReason.PAID_USER)
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return AdDecision.Suppressed(placement, SuppressReason.GLOBAL_DISABLED)
        if (!rc.bool(placement.rcEnabledKey)) return AdDecision.Suppressed(placement, SuppressReason.RC_DISABLED)
        if (session < placement.firstEligibleSession) return AdDecision.Suppressed(placement, SuppressReason.NOT_ELIGIBLE_YET)

        if (placement.isFullScreen && isFullScreenBusy()) return AdDecision.Suppressed(placement, SuppressReason.MUTEX_BUSY)
        // Forward-suppress: yield for a window after the onboarding-complete interstitial showed.
        if (withinOnboardingForwardSuppress()) return AdDecision.Suppressed(placement, SuppressReason.RECENT_ONBOARDING_INTERSTITIAL)
        if (placement.format == AdFormat.INTERSTITIAL) {
            if (interstitialsShown.get() >= rc.long(RcKeys.INTER_MAX_PER_SESSION)) {
                return AdDecision.Suppressed(placement, SuppressReason.SESSION_CAP)
            }
            if (withinCooldown()) return AdDecision.Suppressed(placement, SuppressReason.COOLDOWN)
        }
        return AdDecision.Show(placement, loadTimeoutMs = rc.long(placement.loadTimeoutKey))
    }

    /**
     * Present an INTERSTITIAL placement only (format-specific — App Open and rewarded
     * have their own show paths; never route those or native here). Acquires the mutex,
     * bounds the SDK load by the per-format timeout, FAILS OPEN (returns Skipped, never
     * throws or blocks) on suppression, no-fill, or timeout.
     */
    suspend fun presentInterstitial(placement: Placement): ShowOutcome {
        require(placement.format == AdFormat.INTERSTITIAL) {
            "presentInterstitial requires an INTERSTITIAL placement, got ${placement.format} ($placement)"
        }
        when (val d = decide(placement)) {
            is AdDecision.Suppressed -> { logDecision(placement, d.reason); return ShowOutcome.Skipped(d.reason) }
            is AdDecision.Show -> Unit
        }
        val fmt = placement.format.schemaName
        if (!tryAcquireFullScreen(placement.id)) {
            analytics.adShowFailed(placement.id, "mutex")
            return ShowOutcome.Skipped(SuppressReason.MUTEX_BUSY)
        }
        return try {
            analytics.adRequest(placement.id, fmt)
            analytics.adShowAttempt(placement.id, fmt)
            val timeout = rc.long(placement.loadTimeoutKey).takeIf { it > 0 } ?: DEFAULT_FULLSCREEN_TIMEOUT_MS
            val shown = withTimeoutOrNull(timeout) {
                ads.showInterstitial(placement)   // SDK seam; real impl picks the unit from placement.id
                true
            } ?: false                   // timeout → fail open
            if (shown) {
                recordFullScreenShown(placement)
                analytics.adDismissed(placement.id, fmt)
                ShowOutcome.Shown
            } else {
                analytics.adLoadFailed(placement.id, fmt, "no_fill")   // no preloaded ad / timed out
                ShowOutcome.Skipped(null)
            }
        } finally {
            releaseFullScreen(placement.id)
        }
    }

    // ---- Rewarded unlocks (user-initiated) ----

    /**
     * Attempt a rewarded unlock. Pro users bypass the ad entirely ([RewardOutcome.ProBypass]).
     * The reward is reported as [RewardOutcome.Granted] ONLY when the earned-reward
     * callback fires (rule 5) — never on dismiss, no-fill, or offline.
     */
    suspend fun showRewarded(placement: Placement): RewardOutcome {
        require(placement.format == AdFormat.REWARDED) { "showRewarded called with non-rewarded $placement" }
        if (entitlements.isPro.first()) return RewardOutcome.ProBypass   // Pro: free feature, no ad (rule 6)

        // 1) Free daily allowance — unlock without an ad (serialized so a double-tap
        //    can't double-spend it). Most placements have freePerDay = 0 (always ad-gated).
        val freeUsed = unlockMutex.withLock {
            val s = rewards.snapshot()
            if (rewards.freeToday(s, placement.id) < placement.freePerDay) {
                rewards.incrementFree(placement.id); true
            } else false
        }
        if (freeUsed) return RewardOutcome.FreeGranted

        // 2) Ad-backed path — gate first; never show an ad that couldn't grant.
        if (!consent.canRequestAds.value) return RewardOutcome.NotGranted(SuppressReason.CONSENT_NOT_READY)
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return RewardOutcome.NotGranted(SuppressReason.GLOBAL_DISABLED)
        if (!rc.bool(placement.rcEnabledKey)) return RewardOutcome.NotGranted(SuppressReason.RC_DISABLED)
        if (!withinRewardCap(placement)) return RewardOutcome.NotGranted(SuppressReason.SESSION_CAP)  // daily cap → paywall

        // 3) One rewarded ad at a time: a double-tap loses the CAS → MUTEX_BUSY, no second show.
        if (!tryAcquireFullScreen(placement.id)) return RewardOutcome.NotGranted(SuppressReason.MUTEX_BUSY)
        return try {
            val timeout = rc.long(RcKeys.REWARD_LOAD_TIMEOUT_MS).takeIf { it > 0 } ?: DEFAULT_REWARD_TIMEOUT_MS
            // The boolean IS the SDK earned-reward signal. NO grant on close/fail/offline/timeout (rule 5).
            val earned = withTimeoutOrNull(timeout) { ads.showRewarded(placement.toFeature()) } ?: false
            lastRewardedAtMs = SystemClock.elapsedRealtime()   // engaged a rewarded ad → suppress app-open after
            if (!earned) return RewardOutcome.NotGranted(null)

            analytics.adRewardEarned(placement.id, placement.rewardType)   // earned — logged before granting
            // Grant exactly once, re-checking the cap atomically before incrementing.
            val granted = unlockMutex.withLock {
                val s = rewards.snapshot()
                if (rewards.rewardedToday(s, placement.id) < placement.rewardedPerDay &&
                    rewards.totalRewardedToday(s) < rc.long(RcKeys.REWARD_DAILY_CAP)
                ) {
                    rewards.incrementRewarded(placement.id); true
                } else false
            }
            if (!granted) return RewardOutcome.NotGranted(SuppressReason.SESSION_CAP)
            recordFullScreenShown(placement)
            analytics.adRewardGranted(placement.id, placement.rewardType)  // granted — exactly once
            RewardOutcome.Granted
        } finally {
            releaseFullScreen(placement.id)
        }
    }

    /** Within both the per-placement and the global RC daily rewarded caps. */
    private suspend fun withinRewardCap(placement: Placement): Boolean {
        val s = rewards.snapshot()
        return rewards.rewardedToday(s, placement.id) < placement.rewardedPerDay &&
            rewards.totalRewardedToday(s) < rc.long(RcKeys.REWARD_DAILY_CAP)
    }

    // ---- App-open ad (return-from-background) + suppression zones (rules 1-4 / spec) ----

    /** Record a jump to an external link / ad click (suppresses app-open briefly). */
    fun noteExternalLinkOpened() { lastExternalLinkAtMs = SystemClock.elapsedRealtime() }

    /** Record that a runtime-permission / legal prompt was shown. */
    fun notePermissionPrompt() { lastPermissionAtMs = SystemClock.elapsedRealtime() }

    /** Mark a user flow active so app-open is suppressed while it runs (spec suppress_during_*). */
    fun enterFlow(flow: AdFlow) { activeFlows.add(flow) }
    fun exitFlow(flow: AdFlow) { activeFlows.remove(flow) }

    /** ProcessLifecycle ON_STOP — app moved to background. */
    fun onBackground() { backgroundedAtMs = SystemClock.elapsedRealtime() }

    /** ProcessLifecycle ON_START — app returned to foreground; evaluate + maybe show app-open. */
    fun onForeground() {
        if (firstForeground.compareAndSet(true, false)) return  // cold start — never on first launch
        scope.launch { maybeShowAppOpen() }
    }

    private suspend fun maybeShowAppOpen() {
        val p = Placement.APPOPEN_RESUME
        val fmt = p.format.schemaName
        // Read-only gate check; ALL mutation (caps, timestamps, shown-marks) stays below, after show.
        val reason = appOpenSuppressionSnapshot()
        if (reason != null) { logDecision(p, reason); return }
        if (!ads.isAppOpenAdAvailable()) {                            // not preloaded → continue (fail open)
            analytics.adLoadFailed(p.id, fmt, "not_preloaded")
            return
        }
        if (!tryAcquireFullScreen(p.id)) { analytics.adShowFailed(p.id, "mutex"); return }
        try {
            analytics.adRequest(p.id, fmt)
            analytics.adShowAttempt(p.id, fmt)
            ads.showAppOpen()
            val now = SystemClock.elapsedRealtime()
            lastAppOpenAtMs = now
            lastFullScreenAtMs = now
            appOpensThisSession.incrementAndGet()
            rewards.incrementAppOpen()
            analytics.adDismissed(p.id, fmt)
        } finally {
            releaseFullScreen(p.id)
        }
    }

    /**
     * Pure, SIDE-EFFECT-FREE snapshot: would App Open run in this foreground cycle?
     * Used by the splash interstitial to yield priority WITHOUT touching App Open's
     * show pipeline — it never marks App Open shown, consumes caps, mutates
     * foreground/cooldown state, or starts a load (CLAUDE.md "pure eligibility helpers").
     */
    suspend fun isAppOpenEligibleSnapshot(): Boolean = appOpenSuppressionSnapshot() == null

    /**
     * READ-ONLY SNAPSHOT of all app-open gates; returns the blocking reason, or null if
     * eligible. The `Snapshot` suffix is the contract: this method ONLY evaluates state
     * (entitlement/consent/RC/session/timestamps/caps) and MUST NEVER mutate anything —
     * it does not mark App Open shown, consume caps, touch timestamps, or start a load/show.
     * That makes it safe for cross-placement suppression: the splash interstitial calls it
     * (via [isAppOpenEligibleSnapshot]) to yield priority without running App Open's pipeline.
     * All App Open mutation lives in [maybeShowAppOpen], strictly after the actual show.
     */
    private suspend fun appOpenSuppressionSnapshot(): SuppressReason? {
        val p = Placement.APPOPEN_RESUME
        val ent = entitlements.entitlement.first()
        if (ent.isUnknown) return SuppressReason.ENTITLEMENT_UNKNOWN                // no forced ads until resolved
        if (ent.isPro) return SuppressReason.PAID_USER                             // rule 6: zero forced ads
        if (!consent.canRequestAds.value) return SuppressReason.CONSENT_NOT_READY   // rule 1
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return SuppressReason.GLOBAL_DISABLED
        if (!rc.bool(p.rcEnabledKey)) return SuppressReason.RC_DISABLED             // rule 4: missing → OFF

        // Session eligibility: s2 only if a core action happened in s1, else s3+.
        val session = profile.currentSessionNumber()
        val minSession = rc.long(RcKeys.APPOPEN_MIN_SESSION).toInt()
        if (session < minSession) return SuppressReason.NOT_ELIGIBLE_YET
        if (session == minSession && rc.bool(RcKeys.APPOPEN_REQUIRE_ACTIVATION_S2) && !profile.activatedOnce()) {
            return SuppressReason.NOT_ELIGIBLE_YET
        }

        // Background-time threshold — a brief app switch isn't a real resume.
        val now = SystemClock.elapsedRealtime()
        if (backgroundedAtMs == Long.MIN_VALUE ||
            now - backgroundedAtMs < rc.long(RcKeys.APPOPEN_MIN_BACKGROUND_SECONDS) * 1000
        ) {
            return SuppressReason.BACKGROUND_TOO_SHORT
        }

        if (isFullScreenBusy()) return SuppressReason.MUTEX_BUSY                    // rule 3
        if (withinOnboardingForwardSuppress()) return SuppressReason.RECENT_ONBOARDING_INTERSTITIAL
        if (suppressedByFlow()) return SuppressReason.SUPPRESS_ZONE                 // resume/mock/copilot/billing/legal

        // Post-event suppression windows (external link & ad clicks, rewarded, permission, any full-screen ad).
        if (within(lastExternalLinkAtMs, RcKeys.APPOPEN_SUPPRESS_AFTER_EXTERNAL_LINK)) return SuppressReason.SUPPRESS_ZONE
        if (within(lastRewardedAtMs, RcKeys.APPOPEN_SUPPRESS_AFTER_REWARDED)) return SuppressReason.SUPPRESS_ZONE
        if (within(lastPermissionAtMs, RcKeys.APPOPEN_SUPPRESS_AFTER_PERMISSION)) return SuppressReason.SUPPRESS_ZONE
        if (within(lastFullScreenAtMs, RcKeys.APPOPEN_SUPPRESS_AFTER_FULLSCREEN)) return SuppressReason.SUPPRESS_ZONE

        // App-open cooldown.
        if (lastAppOpenAtMs != Long.MIN_VALUE &&
            now - lastAppOpenAtMs < rc.long(RcKeys.APPOPEN_COOLDOWN_MINUTES) * 60_000
        ) {
            return SuppressReason.COOLDOWN
        }

        // Session + daily caps.
        if (appOpensThisSession.get() >= rc.long(RcKeys.APPOPEN_MAX_PER_SESSION)) return SuppressReason.SESSION_CAP
        if (rewards.appOpenToday(rewards.snapshot()) >= rc.long(RcKeys.APPOPEN_MAX_PER_DAY)) return SuppressReason.SESSION_CAP

        return null
    }

    private fun suppressedByFlow(): Boolean =
        (AdFlow.RESUME in activeFlows && rc.bool(RcKeys.APPOPEN_SUPPRESS_DURING_RESUME)) ||
            (AdFlow.MOCK in activeFlows && rc.bool(RcKeys.APPOPEN_SUPPRESS_DURING_MOCK)) ||
            (AdFlow.COPILOT in activeFlows && rc.bool(RcKeys.APPOPEN_SUPPRESS_DURING_COPILOT)) ||
            (AdFlow.BILLING in activeFlows && rc.bool(RcKeys.APPOPEN_SUPPRESS_DURING_BILLING)) ||
            (AdFlow.LEGAL in activeFlows)   // consent / privacy form always suppresses

    private fun within(eventAtMs: Long, secondsKey: String): Boolean {
        if (eventAtMs == Long.MIN_VALUE) return false
        return SystemClock.elapsedRealtime() - eventAtMs < rc.long(secondsKey) * 1000
    }

    // ---- Splash / session-start interstitial (Apero-style; separate from App Open) ----

    /**
     * Run the splash/session-start interstitial at cold start (call once, after the
     * start destination + UMP consent + session number are known). Never on session 1.
     *
     * The 3-second branded transition is a BRANDED transition, not an ad-load wait:
     * an ad must be ready within `load_timeout_ms` first (fail open if not — no 3s
     * hold), and only then is the transition shown for its full duration before the
     * ad. All decisions go through this manager; the UI just observes [splashTransition].
     */
    suspend fun runSplashInterstitial(): ShowOutcome {
        val p = Placement.INTER_AFTER_SPLASH
        val fmt = p.format.schemaName
        val reason = splashSuppression()
        if (reason != null) { logDecision(p, reason); return ShowOutcome.Skipped(reason) }

        // Ad-load gate (NOT the branded duration): wait at most load_timeout for a ready ad.
        analytics.adRequest(p.id, fmt)
        val timeout = rc.long(RcKeys.AFTER_SPLASH_LOAD_TIMEOUT_MS).takeIf { it > 0 } ?: DEFAULT_FULLSCREEN_TIMEOUT_MS
        val ready = withTimeoutOrNull(timeout) {
            while (!ads.isInterstitialReady()) delay(50)
            true
        } ?: false
        if (!ready) {                                   // no ad ready → fail open, never hold 3s
            analytics.adLoadFailed(p.id, fmt, "not_ready")
            return ShowOutcome.Skipped(SuppressReason.NOT_READY)
        }

        // Ad is ready → show the branded transition for its full duration, then the ad.
        // The transition CANNOT extend a failed ad wait: readiness is gated above and fails
        // open (returns NOT_READY) BEFORE we ever reach here, so this branded hold only runs
        // once an ad is confirmed ready and will show. This is the documented Apero behavior —
        // a transition duration, NOT a load wait — so it is intentionally not clamped to the
        // load timeout (unlike the onboarding interstitial, where transition + wait overlap).
        if (rc.bool(RcKeys.AFTER_SPLASH_TRANSITION_ENABLED)) {
            _splashTransition.value = true
            try {
                delay(rc.long(RcKeys.AFTER_SPLASH_TRANSITION_DURATION_MS))
            } finally {
                _splashTransition.value = false
            }
        }
        if (!tryAcquireFullScreen(p.id)) { analytics.adShowFailed(p.id, "mutex"); return ShowOutcome.Skipped(SuppressReason.MUTEX_BUSY) }
        return try {
            analytics.adShowAttempt(p.id, fmt)
            ads.showInterstitial(p)   // INTERSTITIAL show path (canonical placement → splash ad unit)
            recordFullScreenShown(p)
            splashShownThisSession.set(true)
            lastSplashAtMs = SystemClock.elapsedRealtime()
            analytics.adDismissed(p.id, fmt)
            ShowOutcome.Shown
        } finally {
            releaseFullScreen(p.id)
        }
    }

    private suspend fun splashSuppression(): SuppressReason? {
        val p = Placement.INTER_AFTER_SPLASH
        val ent = entitlements.entitlement.first()
        if (ent.isUnknown) return SuppressReason.ENTITLEMENT_UNKNOWN
        if (ent.isPro) return SuppressReason.PAID_USER                              // rule 6
        if (!consent.canRequestAds.value) return SuppressReason.CONSENT_NOT_READY   // rule 1
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return SuppressReason.GLOBAL_DISABLED
        if (!rc.bool(p.rcEnabledKey)) return SuppressReason.RC_DISABLED             // remote_config_off; missing → OFF

        // Session gate: never session 1; session == min only if activated in session 1; else min+.
        val session = profile.currentSessionNumber()
        val minSession = rc.long(RcKeys.AFTER_SPLASH_MIN_SESSION).toInt()
        if (session < minSession) return SuppressReason.FIRST_SESSION
        if (session == minSession && rc.bool(RcKeys.AFTER_SPLASH_REQUIRE_ACTIVATION_S2) && !profile.activatedOnce()) {
            return SuppressReason.NOT_ACTIVATED_SESSION_2
        }

        // Never both splash + App Open in the same foreground cycle. Uses a pure,
        // side-effect-free snapshot — splash must NOT run App Open's show pipeline.
        if (rc.bool(RcKeys.AFTER_SPLASH_SUPPRESS_IF_APPOPEN) && isAppOpenEligibleSnapshot()) {
            return SuppressReason.APPOPEN_ELIGIBLE
        }
        if (activeFlows.isNotEmpty()) return SuppressReason.PROTECTED_FLOW          // resume/mock/copilot/billing/legal
        if (isFullScreenBusy()) return SuppressReason.MUTEX_BUSY                    // rule 3
        if (splashShownThisSession.get()) return SuppressReason.SESSION_CAP         // max 1 / session
        if (within(lastSplashAtMs, RcKeys.AFTER_SPLASH_COOLDOWN_SECONDS)) return SuppressReason.COOLDOWN
        return null
    }

    // ---- Onboarding-complete interstitial (after onboarding_complete, before first main screen) ----

    /**
     * Run the onboarding-complete interstitial. Call AFTER onboarding_complete is logged +
     * persisted and BEFORE navigating to the first main destination. Caps at once per install,
     * defaults OFF, fails open, and on show forward-suppresses the next forced full-screen.
     *
     * The branded transition runs CONCURRENTLY with the ad-readiness wait: the user is never
     * held past load_timeout for a not-ready ad, and a short transition only completes its
     * minimum once an ad is actually ready.
     */
    suspend fun runOnboardingInterstitial(): ShowOutcome {
        val p = Placement.INTER_AFTER_ONBOARDING_COMPLETE
        val fmt = p.format.schemaName
        val reason = onboardingInterstitialSuppression()
        if (reason != null) { logDecision(p, reason); return ShowOutcome.Skipped(reason) }

        analytics.adRequest(p.id, fmt)
        val timeout = rc.long(RcKeys.AFTER_ONB_LOAD_TIMEOUT_MS).takeIf { it > 0 } ?: DEFAULT_FULLSCREEN_TIMEOUT_MS
        val transitionEnabled = rc.bool(RcKeys.AFTER_ONB_TRANSITION_ENABLED)
        // Clamp: the branded transition runs CONCURRENTLY with the readiness wait, so its
        // minimum hold can never exceed `timeout`. If RC sets transition_duration_ms (e.g. 1200)
        // larger than load_timeout_ms (e.g. 800), the decorative transition must NOT extend the
        // wait — total user delay stays ≤ load_timeout_ms. A not-ready ad still fails open below.
        val configuredTransitionMs = if (transitionEnabled) rc.long(RcKeys.AFTER_ONB_TRANSITION_DURATION_MS) else 0L
        val transitionMin = min(configuredTransitionMs, timeout)
        val start = SystemClock.elapsedRealtime()
        if (transitionEnabled) _splashTransition.value = true
        try {
            // Concurrent: branded transition is visible WHILE we await readiness (≤ load_timeout).
            val ready = withTimeoutOrNull(timeout) {
                while (!ads.isInterstitialReady()) delay(50)
                true
            } ?: false
            if (!ready) {                                  // not ready by timeout → fail open, no decorative hold
                analytics.adLoadFailed(p.id, fmt, "not_ready")
                return ShowOutcome.Skipped(SuppressReason.NOT_READY)
            }
            // Ad ready → let the short branded transition reach its minimum (never beyond timeout).
            val elapsed = SystemClock.elapsedRealtime() - start
            if (transitionMin > elapsed) delay(transitionMin - elapsed)
        } finally {
            _splashTransition.value = false
        }
        if (!tryAcquireFullScreen(p.id)) { analytics.adShowFailed(p.id, "mutex"); return ShowOutcome.Skipped(SuppressReason.MUTEX_BUSY) }
        return try {
            analytics.adShowAttempt(p.id, fmt)
            ads.showInterstitial(p)   // INTERSTITIAL show path (canonical placement → onboarding ad unit)
            recordFullScreenShown(p)
            profile.markOnboardingInterstitialShown()                        // per-install cap
            lastOnboardingInterstitialAtMs = SystemClock.elapsedRealtime()   // forward-suppress window
            analytics.adDismissed(p.id, fmt)
            ShowOutcome.Shown
        } finally {
            releaseFullScreen(p.id)
        }
    }

    private suspend fun onboardingInterstitialSuppression(): SuppressReason? {
        val p = Placement.INTER_AFTER_ONBOARDING_COMPLETE
        val ent = entitlements.entitlement.first()
        if (ent.isUnknown) return SuppressReason.ENTITLEMENT_UNKNOWN
        if (ent.isPro) return SuppressReason.PAID_USER                              // rule 6
        if (!consent.canRequestAds.value) return SuppressReason.CONSENT_NOT_READY   // rule 1
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return SuppressReason.GLOBAL_DISABLED
        if (!rc.bool(p.rcEnabledKey)) return SuppressReason.RC_DISABLED             // remote_config_off; missing → OFF
        if (!profile.profile.first().onboarded) return SuppressReason.ONBOARDING_INCOMPLETE
        // Per-install cap (max_per_install; default/missing 0 → effectively off).
        if (profile.onboardingInterstitialShown()) return SuppressReason.ALREADY_SHOWN_THIS_INSTALL
        if (rc.long(RcKeys.AFTER_ONB_MAX_PER_INSTALL) < 1) return SuppressReason.ALREADY_SHOWN_THIS_INSTALL
        if (isFullScreenBusy()) return SuppressReason.MUTEX_BUSY                    // rule 3
        if (activeFlows.isNotEmpty()) return SuppressReason.PROTECTED_FLOW
        // Don't stack right after another full-screen onboarding ad unless RC allows it.
        if (!rc.bool(RcKeys.AFTER_ONB_ALLOW_AGGRESSIVE_STACK) &&
            within(lastFullScreenAtMs, RcKeys.AFTER_ONB_SUPPRESS_IF_FS_ONB_SHOWN_SECONDS)
        ) {
            return SuppressReason.FULLSCREEN_ONBOARDING_AD_RECENT
        }
        return null
    }

    /** True while the post-onboarding-interstitial forward-suppression window is active. */
    private fun withinOnboardingForwardSuppress(): Boolean {
        if (lastOnboardingInterstitialAtMs == Long.MIN_VALUE) return false
        val windowMs = rc.long(RcKeys.AFTER_ONB_SUPPRESS_NEXT_FULLSCREEN_SECONDS) * 1000
        return SystemClock.elapsedRealtime() - lastOnboardingInterstitialAtMs < windowMs
    }

    // ---- Full-screen mutex (also used by paywall / permission / purchase dialogs, rule 3) ----

    /** Non-ad full-screen surfaces acquire the same slot so nothing overlaps an ad. */
    fun tryEnterFullScreen(surface: FullScreenSurface): Boolean = tryAcquireFullScreen("surface_${surface.name}")
    fun exitFullScreen(surface: FullScreenSurface) = releaseFullScreen("surface_${surface.name}")
    fun isFullScreenBusy(): Boolean = fullScreenOwner.get() != null

    private fun tryAcquireFullScreen(owner: String): Boolean {
        if (!rc.bool(RcKeys.MUTEX_ENABLED)) return true   // mutex disabled by RC (degenerate; default ON)
        val acquired = fullScreenOwner.compareAndSet(null, owner)
        if (acquired && BuildConfig.DEBUG) Log.d(TAG, "fullscreen acquired by $owner")
        return acquired
    }

    private fun releaseFullScreen(owner: String) {
        fullScreenOwner.compareAndSet(owner, null)
    }

    // ---- internals ----

    private fun suppressForPaid(): Boolean = rc.bool(RcKeys.SUPPRESS_FOR_PAID)

    private fun withinCooldown(): Boolean {
        if (lastFullScreenAtMs == Long.MIN_VALUE) return false
        val cooldownMs = rc.long(RcKeys.INTER_COOLDOWN_SECONDS) * 1000
        return SystemClock.elapsedRealtime() - lastFullScreenAtMs < cooldownMs
    }

    private fun recordFullScreenShown(placement: Placement) {
        lastFullScreenAtMs = SystemClock.elapsedRealtime()
        if (placement.format == AdFormat.INTERSTITIAL) interstitialsShown.incrementAndGet()
    }

    private fun logDecision(placement: Placement, reason: SuppressReason) {
        analytics.adSuppressed(placement.id, reason.diag)   // ad_suppressed diagnostic (DEBUG funnel)
        if (BuildConfig.DEBUG) Log.d(TAG, "suppress ${placement.id}: $reason")
    }

    /** Maps a rewarded placement to the SDK's coarse reward feature (cosmetic until the real SDK lands). */
    private fun Placement.toFeature(): RewardedFeature = when (this) {
        Placement.REWARDED_MOCK_START, Placement.REWARDED_MOCK_SCORE -> RewardedFeature.MOCK_INTERVIEW
        Placement.REWARDED_RESUME_DOWNLOAD, Placement.REWARDED_COVER_LETTER -> RewardedFeature.RESUME_GENERATE
        else -> RewardedFeature.RESUME_OPTIMIZE
    }

    private companion object {
        const val TAG = "MonetizationManager"
        const val DEFAULT_FULLSCREEN_TIMEOUT_MS = 1000L
        const val DEFAULT_REWARD_TIMEOUT_MS = 4000L
    }
}
