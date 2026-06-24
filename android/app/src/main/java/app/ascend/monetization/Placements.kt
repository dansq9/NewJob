package app.ascend.monetization

import app.ascend.monetization.config.RcKeys

/** Ad format families. NATIVE is inline (collapses on suppress); the rest are full-screen. */
enum class AdFormat { NATIVE, INTERSTITIAL, REWARDED, APP_OPEN }

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

    // --- Rewarded (user-initiated unlock; "no ad → free" for paid; reward on callback only) ---
    REWARDED_RESUME_OPTIMIZE("ad_rewarded_resume_optimize", AdFormat.REWARDED, "ads.reward.resume_optimize.enabled", 1),
    REWARDED_RESUME_DOWNLOAD("ad_rewarded_resume_download", AdFormat.REWARDED, "ads.reward.resume_download.enabled", 1),
    REWARDED_COVER_LETTER("ad_rewarded_cover_letter", AdFormat.REWARDED, "ads.reward.cover_letter.enabled", 1),
    REWARDED_MOCK_START("ad_rewarded_mock_start", AdFormat.REWARDED, "ads.reward.mock_start.enabled", 1),
    REWARDED_MOCK_SCORE("ad_rewarded_mock_score", AdFormat.REWARDED, "ads.reward.mock_score.enabled", 1),
    REWARDED_GAME_HINT("ad_rewarded_game_hint", AdFormat.REWARDED, "ads.reward.game_hint.enabled", 1),

    // --- App-open (full-screen on resume; suppressed for paid; fail-open) ---
    APPOPEN_RESUME("ad_appopen_resume", AdFormat.APP_OPEN, "ads.appopen.resume.enabled", 2);

    val isFullScreen: Boolean get() = format != AdFormat.NATIVE
}

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
    COOLDOWN,           // within the interstitial cooldown window
    SESSION_CAP,        // per-session interstitial cap reached
}

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

/** Outcome of a rewarded unlock attempt. Reward is granted ONLY on [Granted] (rule 5). */
sealed interface RewardOutcome {
    /** The user is Pro — the feature is unlocked free, no ad shown (rule 6). */
    data object ProBypass : RewardOutcome
    /** The earned-reward callback fired exactly once — grant the unlock. */
    data object Granted : RewardOutcome
    /** No reward (suppressed, no-fill, dismissed early, or offline). Show retry/upgrade. */
    data class NotGranted(val reason: SuppressReason?) : RewardOutcome
}

/** Non-ad full-screen surfaces that also share the mutex (rule 3). */
enum class FullScreenSurface { PAYWALL, PURCHASE_DIALOG, PERMISSION_DIALOG }
