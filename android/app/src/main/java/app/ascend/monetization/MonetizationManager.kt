package app.ascend.monetization

import android.os.SystemClock
import android.util.Log
import app.ascend.BuildConfig
import app.ascend.analytics.AnalyticsTracker
import app.ascend.data.billing.EntitlementRepository
import app.ascend.data.local.ProfileRepository
import app.ascend.monetization.ads.AdsManager
import app.ascend.monetization.ads.RewardedFeature
import app.ascend.monetization.config.RcKeys
import app.ascend.monetization.config.RemoteConfig
import app.ascend.monetization.consent.ConsentManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

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
) {
    // The single full-screen slot. Holds the id of whatever surface is on screen.
    private val fullScreenOwner = AtomicReference<String?>(null)

    // Interstitial pacing, session-scoped (resets each cold start).
    private val interstitialsShown = AtomicInteger(0)
    @Volatile private var lastFullScreenAtMs: Long = Long.MIN_VALUE

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
        combine(entitlements.isPro, consent.canRequestAds) { pro, canRequest ->
            decideNative(placement, pro, canRequest)
        }

    private fun decideNative(placement: Placement, pro: Boolean, canRequestAds: Boolean): AdDecision {
        if (!canRequestAds) return AdDecision.Suppressed(placement, SuppressReason.CONSENT_NOT_READY)
        if (pro && suppressForPaid()) return AdDecision.Suppressed(placement, SuppressReason.PAID_USER)
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return AdDecision.Suppressed(placement, SuppressReason.GLOBAL_DISABLED)
        if (!rc.bool(placement.rcEnabledKey)) return AdDecision.Suppressed(placement, SuppressReason.RC_DISABLED)
        return AdDecision.Show(placement, loadTimeoutMs = 0L)   // native is non-blocking
    }

    /** How many organic rows between native job-list ads (RC `ads.native.job_list.frequency`). */
    fun nativeListFrequency(): Int = rc.long(RcKeys.NATIVE_JOB_LIST_FREQUENCY).toInt().coerceAtLeast(2)

    // ---- Full-screen decision (interstitial / app-open) ----

    /** Pure-policy decision for a full-screen placement; the present path re-checks the mutex atomically. */
    suspend fun decide(placement: Placement): AdDecision {
        val pro = entitlements.isPro.first()
        val canRequestAds = consent.canRequestAds.value
        val session = profile.currentSessionNumber()

        if (!canRequestAds) return AdDecision.Suppressed(placement, SuppressReason.CONSENT_NOT_READY)
        if (pro && suppressForPaid()) return AdDecision.Suppressed(placement, SuppressReason.PAID_USER)
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return AdDecision.Suppressed(placement, SuppressReason.GLOBAL_DISABLED)
        if (!rc.bool(placement.rcEnabledKey)) return AdDecision.Suppressed(placement, SuppressReason.RC_DISABLED)
        if (session < placement.firstEligibleSession) return AdDecision.Suppressed(placement, SuppressReason.NOT_ELIGIBLE_YET)

        if (placement.isFullScreen && isFullScreenBusy()) return AdDecision.Suppressed(placement, SuppressReason.MUTEX_BUSY)
        if (placement.format == AdFormat.INTERSTITIAL) {
            if (interstitialsShown.get() >= rc.long(RcKeys.INTER_MAX_PER_SESSION)) {
                return AdDecision.Suppressed(placement, SuppressReason.SESSION_CAP)
            }
            if (withinCooldown()) return AdDecision.Suppressed(placement, SuppressReason.COOLDOWN)
        }
        return AdDecision.Show(placement, loadTimeoutMs = rc.long(placement.loadTimeoutKey))
    }

    /**
     * Try to present a full-screen interstitial / app-open. Acquires the mutex,
     * bounds the SDK load by the per-format timeout, FAILS OPEN (returns Skipped,
     * never throws or blocks) on suppression, no-fill, or timeout.
     */
    suspend fun presentFullScreen(placement: Placement): ShowOutcome {
        require(placement.isFullScreen) { "presentFullScreen called with native placement $placement" }
        when (val d = decide(placement)) {
            is AdDecision.Suppressed -> { logDecision(placement, d.reason); return ShowOutcome.Skipped(d.reason) }
            is AdDecision.Show -> Unit
        }
        if (!tryAcquireFullScreen(placement.id)) return ShowOutcome.Skipped(SuppressReason.MUTEX_BUSY)
        return try {
            val timeout = rc.long(placement.loadTimeoutKey).takeIf { it > 0 } ?: DEFAULT_FULLSCREEN_TIMEOUT_MS
            val shown = withTimeoutOrNull(timeout) {
                ads.showInterstitial()   // SDK seam; real impl loads+shows here
                true
            } ?: false                   // timeout → fail open
            if (shown) recordFullScreenShown(placement)
            if (shown) ShowOutcome.Shown else ShowOutcome.Skipped(null)
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
        if (entitlements.isPro.first()) return RewardOutcome.ProBypass   // feature is free for Pro (rule 6)
        if (!consent.canRequestAds.value) return RewardOutcome.NotGranted(SuppressReason.CONSENT_NOT_READY)
        if (!rc.bool(RcKeys.GLOBAL_ENABLED)) return RewardOutcome.NotGranted(SuppressReason.GLOBAL_DISABLED)
        if (!rc.bool(placement.rcEnabledKey)) return RewardOutcome.NotGranted(SuppressReason.RC_DISABLED)
        if (!tryAcquireFullScreen(placement.id)) return RewardOutcome.NotGranted(SuppressReason.MUTEX_BUSY)
        return try {
            val timeout = rc.long(RcKeys.REWARD_LOAD_TIMEOUT_MS).takeIf { it > 0 } ?: DEFAULT_REWARD_TIMEOUT_MS
            // withTimeoutOrNull bounds the LOAD; a real impl must not time out an in-progress watch.
            val granted = withTimeoutOrNull(timeout) { ads.showRewarded(placement.toFeature()) } ?: false
            if (granted) recordFullScreenShown(placement)
            if (granted) RewardOutcome.Granted else RewardOutcome.NotGranted(null)
        } finally {
            releaseFullScreen(placement.id)
        }
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
