package app.ascend.monetization

import app.ascend.analytics.AdPrecision
import app.ascend.monetization.config.RcKeys

/** Ad format families. NATIVE is inline (collapses on suppress); the rest are full-screen. */
enum class AdFormat(val schemaName: String) {
    NATIVE("native"), INTERSTITIAL("interstitial"), REWARDED("rewarded"), APP_OPEN("app_open")
}

/**
 * SDK-agnostic mirror of a Google Mobile Ads ILRD paid callback (`AdValue` +
 * `ResponseInfo`). The real [app.ascend.monetization.ads.AdsManager] impl builds
 * one of these inside each format's `setOnPaidEventListener` and forwards it to
 * [MonetizationManager.onAdPaid], which logs `ad_impression` (rule 7). Keeping it
 * SDK-agnostic means no screen or analytics code depends on the ads SDK.
 */
data class AdPaidEvent(
    val placementId: String,
    val format: AdFormat,
    val valueMicros: Long,        // AdValue.getValueMicros()
    val currencyCode: String,     // AdValue.getCurrencyCode()
    val precision: AdPrecision,   // AdPrecision.fromAdMob(AdValue.getPrecisionType())
    val adSource: String?,        // responseInfo.loadedAdapterResponseInfo?.adSourceName
    val adUnitId: String?,        // responseInfo.responseId / configured unit id
)

/**
 * The canonical ad placements (monetization-spec "Ad placements" table). Each
 * carries its `placement_id` (the SAME snake_case string used in Remote Config
 * and analytics — rule 9), its format, the Remote Config enable key, and the
 * first session it becomes eligible.
 *
 * `ad_rewarded_copilot_session` is deliberately omitted: Copilot is Pro-only and
 * has NO rewarded path (rule 6 / spec).
 */
enum class Placement(
    val id: String,
    val format: AdFormat,
    val rcEnabledKey: String,
    val firstEligibleSession: Int,
    /** Rewarded only: free unlocks per day before an ad is required (spec table). */
    val freePerDay: Int = 0,
    /** Rewarded only: additional ad-gated unlocks per day (spec table; RC daily cap also applies). */
    val rewardedPerDay: Int = 0,
) {
    // --- Native (inline; collapse when suppressed; hidden for paid) ---
    NATIVE_LANGUAGE("ad_native_language", AdFormat.NATIVE, "ads.native.language.enabled", 1),
    NATIVE_ONBOARDING_FINAL("ad_native_onboarding_final", AdFormat.NATIVE, "ads.native.onboarding_final.enabled", 1),
    NATIVE_HOME_MID("ad_native_home_mid", AdFormat.NATIVE, "ads.native.home.enabled", 1),
    NATIVE_JOB_LIST("ad_native_job_list", AdFormat.NATIVE, "ads.native.job_list.enabled", 1),
    NATIVE_JOB_DETAIL_BOTTOM("ad_native_job_detail_bottom", AdFormat.NATIVE, "ads.native.job_detail.enabled", 1),
    NATIVE_RESUME_RESULT("ad_native_resume_result", AdFormat.NATIVE, "ads.native.resume_result.enabled", 1),
    NATIVE_TRACKER_EMPTY("ad_native_tracker_empty", AdFormat.NATIVE, "ads.native.tracker_empty.enabled", 1),
    NATIVE_GAMES_HUB("ad_native_games_hub", AdFormat.NATIVE, "ads.native.games_hub.enabled", 1),

    // --- Interstitial (full-screen; suppressed for paid; fail-open) ---
    INTER_AFTER_SEARCH_BATCH("ad_inter_after_search_batch", AdFormat.INTERSTITIAL, "ads.inter.search_batch.enabled", 2),
    INTER_AFTER_JOB_DETAIL_CLOSE("ad_inter_after_job_detail_close", AdFormat.INTERSTITIAL, "ads.inter.job_detail_close.enabled", 2),
    INTER_AFTER_RESUME_SCORE("ad_inter_after_resume_score", AdFormat.INTERSTITIAL, "ads.inter.resume_score.enabled", 2),
    INTER_AFTER_MOCK_REPORT("ad_inter_after_mock_report", AdFormat.INTERSTITIAL, "ads.inter.mock_report.enabled", 2),
    INTER_AFTER_COPILOT_END("ad_inter_after_copilot_end", AdFormat.INTERSTITIAL, "ads.inter.copilot_end.enabled", 2),
    INTER_AFTER_GAME_COMPLETE("ad_inter_after_game_complete", AdFormat.INTERSTITIAL, "ads.inter.game_complete.enabled", 2),
    // Splash/session-start interstitial — its own session gate (RC after_splash.*); never session 1.
    INTER_AFTER_SPLASH("ad_inter_after_splash", AdFormat.INTERSTITIAL, "ads.inter.after_splash.enabled", 2),

    // --- Rewarded (user-initiated unlock; "no ad → free" for paid; reward on callback only) ---
    // free/day + rewarded/day caps from the spec's "Rewarded unlocks" table.
    REWARDED_RESUME_OPTIMIZE("ad_rewarded_resume_optimize", AdFormat.REWARDED, "ads.reward.resume_optimize.enabled", 1, freePerDay = 0, rewardedPerDay = 3),
    REWARDED_RESUME_DOWNLOAD("ad_rewarded_resume_download", AdFormat.REWARDED, "ads.reward.resume_download.enabled", 1, freePerDay = 0, rewardedPerDay = 3),
    REWARDED_COVER_LETTER("ad_rewarded_cover_letter", AdFormat.REWARDED, "ads.reward.cover_letter.enabled", 1, freePerDay = 0, rewardedPerDay = 5),
    REWARDED_MOCK_START("ad_rewarded_mock_start", AdFormat.REWARDED, "ads.reward.mock_start.enabled", 1, freePerDay = 1, rewardedPerDay = 3),
    REWARDED_MOCK_SCORE("ad_rewarded_mock_score", AdFormat.REWARDED, "ads.reward.mock_score.enabled", 1, freePerDay = 0, rewardedPerDay = 3),
    REWARDED_GAME_HINT("ad_rewarded_game_hint", AdFormat.REWARDED, "ads.reward.game_hint.enabled", 1, freePerDay = 1, rewardedPerDay = 5),

    // --- App-open (full-screen on resume; suppressed for paid; fail-open) ---
    APPOPEN_RESUME("ad_appopen_resume", AdFormat.APP_OPEN, "ads.appopen.resume.enabled", 2);

    val isFullScreen: Boolean get() = format != AdFormat.NATIVE
}

/** Rewarded `reward_type` token for analytics (e.g. `resume_download`); spec-aligned. */
val Placement.rewardType: String
    get() = id.removePrefix("ad_rewarded_")

/** Per-format load timeout RC key (native = non-blocking 0). */
val Placement.loadTimeoutKey: String
    get() = when (format) {
        AdFormat.NATIVE -> RcKeys.NATIVE_LOAD_TIMEOUT_MS
        AdFormat.INTERSTITIAL -> RcKeys.INTER_LOAD_TIMEOUT_MS
        AdFormat.REWARDED -> RcKeys.REWARD_LOAD_TIMEOUT_MS
        AdFormat.APP_OPEN -> RcKeys.APPOPEN_LOAD_TIMEOUT_MS
    }

/** Why a placement was not shown. Native → UI collapses; full-screen → caller continues (fail-open). */
enum class SuppressReason {
    PAID_USER,          // Pro: zero forced ads (rule 6)
    CONSENT_NOT_READY,  // UMP gate still closed (rule 1)
    GLOBAL_DISABLED,    // ads_global_enabled = false
    RC_DISABLED,        // this placement's RC enable key is off / missing (rule 4)
    NOT_ELIGIBLE_YET,   // before the placement's first eligible session
    MUTEX_BUSY,         // another full-screen surface is showing (rule 3)
    COOLDOWN,           // within the interstitial / app-open cooldown window
    SESSION_CAP,        // per-session / per-day cap reached
    FIRST_LAUNCH,       // app-open: cold start, never on first foreground
    BACKGROUND_TOO_SHORT, // app-open: returned faster than min_background_seconds
    SUPPRESS_ZONE,      // app-open: within a suppress_* window or an active suppressed flow
    NOT_PRELOADED,      // app-open: no ready ad → continue immediately (fail open)
    ENTITLEMENT_UNKNOWN, // billing not yet resolved — no forced ads until restore resolves
    FIRST_SESSION,       // splash interstitial: session 1 (or below min_session) — never show
    NOT_ACTIVATED_SESSION_2, // splash interstitial: session 2 but no session-1 core action
    APPOPEN_ELIGIBLE,    // splash interstitial: App Open is eligible this foreground cycle — yield
    PROTECTED_FLOW,      // splash interstitial: a protected flow is active (resume/mock/copilot/billing)
    NOT_READY,           // splash interstitial: no ad ready within the load timeout — fail open
}

/** Low-cardinality token for the `ad_suppressed` diagnostic event (event-schema §8). */
val SuppressReason.diag: String
    get() = when (this) {
        SuppressReason.PAID_USER -> "paid"
        SuppressReason.CONSENT_NOT_READY -> "consent"
        SuppressReason.GLOBAL_DISABLED -> "global_off"
        SuppressReason.RC_DISABLED -> "rc_off"
        SuppressReason.NOT_ELIGIBLE_YET -> "not_eligible"
        SuppressReason.MUTEX_BUSY -> "mutex"
        SuppressReason.COOLDOWN -> "cooldown"
        SuppressReason.SESSION_CAP -> "cap"
        SuppressReason.FIRST_LAUNCH -> "first_launch"
        SuppressReason.BACKGROUND_TOO_SHORT -> "bg_too_short"
        SuppressReason.SUPPRESS_ZONE -> "suppress_zone"
        SuppressReason.NOT_PRELOADED -> "not_preloaded"
        SuppressReason.ENTITLEMENT_UNKNOWN -> "entitlement_unknown"
        SuppressReason.FIRST_SESSION -> "first_session"
        SuppressReason.NOT_ACTIVATED_SESSION_2 -> "not_activated_session_2"
        SuppressReason.APPOPEN_ELIGIBLE -> "appopen_eligible"
        SuppressReason.PROTECTED_FLOW -> "protected_flow"
        SuppressReason.NOT_READY -> "not_ready"
    }

/**
 * Active user flows that suppress the app-open ad while in progress (spec
 * `suppress_during_*`). Reported by screens via MonetizationManager.enterFlow/exitFlow.
 */
enum class AdFlow { RESUME, MOCK, COPILOT, BILLING, LEGAL }

/** Decision returned by [MonetizationManager.decide]. */
sealed interface AdDecision {
    /** Show this placement; [loadTimeoutMs] is the fail-open budget (0 = non-blocking native). */
    data class Show(val placement: Placement, val loadTimeoutMs: Long) : AdDecision
    /** Do not show. Native callers collapse the container; full-screen callers no-op. */
    data class Suppressed(val placement: Placement, val reason: SuppressReason) : AdDecision
}

/** Outcome of attempting a full-screen present. */
sealed interface ShowOutcome {
    data object Shown : ShowOutcome
    /** Not shown — either suppressed by policy or no-fill/timeout. Core loop continues regardless. */
    data class Skipped(val reason: SuppressReason?) : ShowOutcome
}

/** Outcome of a rewarded unlock attempt. An AD-backed reward is granted ONLY on [Granted] (rule 5). */
sealed interface RewardOutcome {
    /** The user is Pro — the feature is unlocked free, no ad shown (rule 6). */
    data object ProBypass : RewardOutcome
    /** A free daily allowance was used — unlocked without showing an ad. */
    data object FreeGranted : RewardOutcome
    /** The earned-reward callback fired exactly once — ad-backed unlock granted. */
    data object Granted : RewardOutcome
    /**
     * No reward. [reason] == SESSION_CAP means the daily cap is reached → route to the
     * paywall; any other reason (no-fill, dismissed early, offline) → show retry/upgrade.
     * Never grants on close/fail/offline (rule 5).
     */
    data class NotGranted(val reason: SuppressReason?) : RewardOutcome
}

/** Non-ad full-screen surfaces that also share the mutex (rule 3). */
enum class FullScreenSurface { PAYWALL, PURCHASE_DIALOG, PERMISSION_DIALOG }
