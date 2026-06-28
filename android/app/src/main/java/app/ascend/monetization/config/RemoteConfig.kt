package app.ascend.monetization.config

import android.util.Log
import app.ascend.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote Config key names (canonical, snake/dot-case — the SAME strings used in
 * the Firebase console, the monetization spec, and analytics). The only place
 * these raw strings live (CLAUDE.md rules 4, 8-9).
 */
object RcKeys {
    // Global switches
    const val GLOBAL_ENABLED = "ads_global_enabled"
    const val SUPPRESS_FOR_PAID = "ads.suppress_for_paid_users"
    const val MUTEX_ENABLED = "ads.fullscreen.mutex_enabled"
    const val AGGRESSIVENESS_TIER = "ad_aggressiveness_tier"

    // Per-format load timeouts (ms). Native = 0 → non-blocking/collapse.
    const val INTER_LOAD_TIMEOUT_MS = "ads.inter.load_timeout_ms"
    const val REWARD_LOAD_TIMEOUT_MS = "ads.reward.load_timeout_ms"
    const val NATIVE_LOAD_TIMEOUT_MS = "ads.native.load_timeout_ms"
    const val APPOPEN_LOAD_TIMEOUT_MS = "ads.appopen.resume.load_timeout_ms"

    // Interstitial pacing
    const val INTER_COOLDOWN_SECONDS = "ads.inter.cooldown_seconds"
    const val INTER_MAX_PER_SESSION = "ads.inter.max_per_session"

    // Rewarded global daily cap (total ad-backed unlocks per day, across all placements)
    const val REWARD_DAILY_CAP = "ads.reward.daily_cap"

    // Splash / session-start interstitial (Apero-style; separate from App Open)
    const val AFTER_SPLASH_MIN_SESSION = "ads.inter.after_splash.min_session"
    const val AFTER_SPLASH_REQUIRE_ACTIVATION_S2 = "ads.inter.after_splash.require_activation_for_session_2"
    const val AFTER_SPLASH_COOLDOWN_SECONDS = "ads.inter.after_splash.cooldown_seconds"
    const val AFTER_SPLASH_LOAD_TIMEOUT_MS = "ads.inter.after_splash.load_timeout_ms"
    const val AFTER_SPLASH_TRANSITION_ENABLED = "ads.inter.after_splash.transition_enabled"
    const val AFTER_SPLASH_TRANSITION_DURATION_MS = "ads.inter.after_splash.transition_duration_ms"
    const val AFTER_SPLASH_SUPPRESS_IF_APPOPEN = "ads.inter.after_splash.suppress_if_appopen_eligible"

    // Onboarding-complete interstitial (fires after onboarding_complete, before first main screen)
    const val AFTER_ONB_MAX_PER_INSTALL = "ads.inter.after_onboarding_complete.max_per_install"
    const val AFTER_ONB_LOAD_TIMEOUT_MS = "ads.inter.after_onboarding_complete.load_timeout_ms"
    const val AFTER_ONB_TRANSITION_ENABLED = "ads.inter.after_onboarding_complete.transition_enabled"
    const val AFTER_ONB_TRANSITION_DURATION_MS = "ads.inter.after_onboarding_complete.transition_duration_ms"
    const val AFTER_ONB_SUPPRESS_IF_FS_ONB_SHOWN_SECONDS = "ads.inter.after_onboarding_complete.suppress_if_fullscreen_onboarding_ad_shown_seconds"
    const val AFTER_ONB_ALLOW_AGGRESSIVE_STACK = "ads.inter.after_onboarding_complete.allow_aggressive_stack"
    const val AFTER_ONB_SUPPRESS_NEXT_FULLSCREEN_SECONDS = "ads.inter.after_onboarding_complete.suppress_next_fullscreen_seconds"

    // Onboarding tour-guide + animations (onboarding UI only; must never block onboarding)
    const val ONB_TOUR_ENABLED = "onboarding.tour.enabled"
    const val ONB_TOUR_VARIANT = "onboarding.tour.variant"
    const val ONB_TOUR_MAX_CARDS = "onboarding.tour.max_cards"
    const val ONB_TOUR_FORCE_COMPLETION = "onboarding.tour.force_completion"
    const val ONB_TOUR_SHOW_SKIP = "onboarding.tour.show_skip"
    const val ONB_TOUR_PLACEMENT = "onboarding.tour.placement"
    const val ONB_TOUR_SUPPRESS_IF_RESUME = "onboarding.tour.suppress_if_resume_uploaded"
    const val ONB_TOUR_SUPPRESS_IF_RETURNING = "onboarding.tour.suppress_if_returning_user"
    const val ONB_TOUR_ONCE_PER_INSTALL = "onboarding.tour.once_per_install"
    const val ONB_ANIM_ENABLED = "onboarding.animations.enabled"
    const val ONB_ANIM_VARIANT = "onboarding.animations.variant"
    const val ONB_ANIM_DURATION_MS = "onboarding.animations.duration_ms"
    const val ONB_ANIM_REDUCE_MOTION_RESPECT = "onboarding.animations.reduce_motion_respect_system"
    const val ONB_ANIM_SPLASH_BRAND_DURATION_MS = "onboarding.animations.splash_brand_duration_ms"

    // Native frequency (job list)
    const val NATIVE_JOB_LIST_FREQUENCY = "ads.native.job_list.frequency"

    // App-open
    const val APPOPEN_MIN_SESSION = "ads.appopen.resume.min_session"
    const val APPOPEN_REQUIRE_ACTIVATION_S2 = "ads.appopen.resume.require_activation_for_session_2"
    const val APPOPEN_COOLDOWN_MINUTES = "ads.appopen.resume.cooldown_minutes"
    const val APPOPEN_MAX_PER_SESSION = "ads.appopen.resume.max_per_session"
    const val APPOPEN_MAX_PER_DAY = "ads.appopen.resume.max_per_day"
    const val APPOPEN_MIN_BACKGROUND_SECONDS = "ads.appopen.resume.min_background_seconds"

    // App-open suppression windows (seconds since the event) and active-flow guards
    const val APPOPEN_SUPPRESS_AFTER_EXTERNAL_LINK = "ads.appopen.resume.suppress_after_external_link_seconds"
    const val APPOPEN_SUPPRESS_AFTER_REWARDED = "ads.appopen.resume.suppress_after_rewarded_seconds"
    const val APPOPEN_SUPPRESS_AFTER_PERMISSION = "ads.appopen.resume.suppress_after_permission_seconds"
    const val APPOPEN_SUPPRESS_AFTER_FULLSCREEN = "ads.appopen.resume.suppress_after_fullscreen_ad_seconds"
    const val APPOPEN_SUPPRESS_DURING_RESUME = "ads.appopen.resume.suppress_during_resume_flow"
    const val APPOPEN_SUPPRESS_DURING_MOCK = "ads.appopen.resume.suppress_during_mock_flow"
    const val APPOPEN_SUPPRESS_DURING_COPILOT = "ads.appopen.resume.suppress_during_copilot_flow"
    const val APPOPEN_SUPPRESS_DURING_BILLING = "ads.appopen.resume.suppress_during_billing_flow"
}

/**
 * In-app defaults = the spec's **missing-key** column (the FAIL-SAFE fallback),
 * NOT the normal "default" column. This guarantees that when Remote Config has
 * not delivered a value, behavior fails closed: every full-screen toggle is OFF,
 * `ads_global_enabled` is false, and pacing caps are at their most conservative
 * (CLAUDE.md rule 4: "a missing full-screen key defaults OFF").
 *
 * Per-placement `*.enabled` keys are intentionally absent here: [RemoteConfig.bool]
 * returns false for any unknown key, so an un-provisioned placement is OFF.
 */
private val DEFAULTS: Map<String, Any> = mapOf(
    RcKeys.GLOBAL_ENABLED to false,
    RcKeys.SUPPRESS_FOR_PAID to true,
    RcKeys.MUTEX_ENABLED to true,
    RcKeys.AGGRESSIVENESS_TIER to "balanced",

    RcKeys.INTER_LOAD_TIMEOUT_MS to 1000L,
    // This bounds the WHOLE rewarded show in MonetizationManager (load + the user watching),
    // since the earned-reward signal only arrives at the end of playback. It must comfortably
    // exceed a full rewarded view or a real ad could never grant. AdMobAdsManager fails open
    // fast on no-fill internally, so this large cap only matters once an ad is actually playing.
    RcKeys.REWARD_LOAD_TIMEOUT_MS to 90_000L,
    RcKeys.NATIVE_LOAD_TIMEOUT_MS to 0L,
    RcKeys.APPOPEN_LOAD_TIMEOUT_MS to 1200L,

    RcKeys.INTER_COOLDOWN_SECONDS to 180L,
    RcKeys.INTER_MAX_PER_SESSION to 1L,

    RcKeys.REWARD_DAILY_CAP to 3L,   // missing-key (fail-safe) value; spec default is 5

    // Splash interstitial — fail-safe (missing-key) values; OFF until configured.
    RcKeys.AFTER_SPLASH_MIN_SESSION to 3L,
    RcKeys.AFTER_SPLASH_REQUIRE_ACTIVATION_S2 to true,
    RcKeys.AFTER_SPLASH_COOLDOWN_SECONDS to 300L,
    RcKeys.AFTER_SPLASH_LOAD_TIMEOUT_MS to 1000L,
    RcKeys.AFTER_SPLASH_TRANSITION_ENABLED to true,
    RcKeys.AFTER_SPLASH_TRANSITION_DURATION_MS to 1500L,
    RcKeys.AFTER_SPLASH_SUPPRESS_IF_APPOPEN to true,

    // Onboarding-complete interstitial — fail-safe (missing-key) values; OFF until configured.
    RcKeys.AFTER_ONB_MAX_PER_INSTALL to 0L,
    RcKeys.AFTER_ONB_LOAD_TIMEOUT_MS to 1200L,
    RcKeys.AFTER_ONB_TRANSITION_ENABLED to true,
    RcKeys.AFTER_ONB_TRANSITION_DURATION_MS to 700L,
    RcKeys.AFTER_ONB_SUPPRESS_IF_FS_ONB_SHOWN_SECONDS to 60L,
    RcKeys.AFTER_ONB_ALLOW_AGGRESSIVE_STACK to false,
    RcKeys.AFTER_ONB_SUPPRESS_NEXT_FULLSCREEN_SECONDS to 180L,

    // Onboarding tour + animations — fail-safe (missing-key) values: tour OFF, minimal motion.
    RcKeys.ONB_TOUR_ENABLED to false,
    RcKeys.ONB_TOUR_VARIANT to "none",
    RcKeys.ONB_TOUR_MAX_CARDS to 0L,
    RcKeys.ONB_TOUR_FORCE_COMPLETION to false,
    RcKeys.ONB_TOUR_SHOW_SKIP to true,
    RcKeys.ONB_TOUR_PLACEMENT to "after_location",
    RcKeys.ONB_TOUR_SUPPRESS_IF_RESUME to true,
    RcKeys.ONB_TOUR_SUPPRESS_IF_RETURNING to true,
    RcKeys.ONB_TOUR_ONCE_PER_INSTALL to true,
    RcKeys.ONB_ANIM_ENABLED to true,
    RcKeys.ONB_ANIM_VARIANT to "none",
    RcKeys.ONB_ANIM_DURATION_MS to 500L,
    RcKeys.ONB_ANIM_REDUCE_MOTION_RESPECT to true,
    RcKeys.ONB_ANIM_SPLASH_BRAND_DURATION_MS to 800L,

    RcKeys.NATIVE_JOB_LIST_FREQUENCY to 8L,

    RcKeys.APPOPEN_MIN_SESSION to 3L,
    RcKeys.APPOPEN_REQUIRE_ACTIVATION_S2 to true,
    RcKeys.APPOPEN_COOLDOWN_MINUTES to 45L,
    RcKeys.APPOPEN_MAX_PER_SESSION to 1L,
    RcKeys.APPOPEN_MAX_PER_DAY to 2L,
    RcKeys.APPOPEN_MIN_BACKGROUND_SECONDS to 30L,

    RcKeys.APPOPEN_SUPPRESS_AFTER_EXTERNAL_LINK to 300L,
    RcKeys.APPOPEN_SUPPRESS_AFTER_REWARDED to 300L,
    RcKeys.APPOPEN_SUPPRESS_AFTER_PERMISSION to 120L,
    RcKeys.APPOPEN_SUPPRESS_AFTER_FULLSCREEN to 300L,
    RcKeys.APPOPEN_SUPPRESS_DURING_RESUME to true,
    RcKeys.APPOPEN_SUPPRESS_DURING_MOCK to true,
    RcKeys.APPOPEN_SUPPRESS_DURING_COPILOT to true,
    RcKeys.APPOPEN_SUPPRESS_DURING_BILLING to true,
)

/**
 * Single source of Remote Config values for all monetization decisions.
 *
 * Backed by [FirebaseRemoteConfig], which only delivers server values once the
 * google-services.json + plugin are added by the human engineer. Until then
 * FirebaseApp is absent and every read returns the fail-safe [DEFAULTS] above —
 * so the app compiles and runs with ads effectively OFF, never with a wrong-open
 * full-screen placement.
 *
 * A remote value is honored ONLY when it has actually been fetched+activated
 * (source == REMOTE); an un-fetched key falls back to the spec default. Reads are
 * cheap snapshots — [refresh] is called once at startup.
 */
@Singleton
class RemoteConfig @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val rc: FirebaseRemoteConfig? by lazy {
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseRemoteConfig.getInstance() else null
    }

    /** Fetch + activate once per cold start. No-op (defaults only) when Firebase is absent. */
    suspend fun refresh() {
        val config = rc ?: run {
            if (BuildConfig.DEBUG) Log.d(TAG, "Firebase absent — using in-app spec defaults only")
            return
        }
        runCatching {
            config.fetchAndActivate().await()
            if (BuildConfig.DEBUG) Log.d(TAG, "Remote Config activated")
        }.onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "RC fetch failed; using cache/defaults", it) }
    }

    fun bool(key: String): Boolean = forcedBool(key) ?: (remote(key)?.asBoolean() ?: (DEFAULTS[key] as? Boolean ?: false))
    fun long(key: String): Long = forcedLong(key) ?: (remote(key)?.asLong() ?: (DEFAULTS[key] as? Long ?: 0L))
    fun string(key: String): String =
        remote(key)?.asString()?.takeIf { it.isNotEmpty() } ?: (DEFAULTS[key] as? String ?: "")

    // ---- Debug-only force-ads override (BuildConfig.DEBUG_FORCE_ADS) ----
    // Lets a tester see Google TEST ads in Android Studio WITHOUT a Firebase Remote Config
    // fetch (RC defaults leave every placement OFF). Strictly gated by BuildConfig.DEBUG &&
    // DEBUG_FORCE_ADS, so release behavior is 100% unchanged — Remote Config stays the only
    // production switch. It flips placement enables on and relaxes the early-session gates so
    // splash/onboarding/app-open are reachable on a fresh debug install.
    private val forceAds = BuildConfig.DEBUG && BuildConfig.DEBUG_FORCE_ADS

    private fun forcedBool(key: String): Boolean? {
        if (!forceAds) return null
        return when {
            key == RcKeys.GLOBAL_ENABLED -> true
            key.endsWith(".enabled") -> true                       // every placement enable key
            key == RcKeys.AFTER_SPLASH_REQUIRE_ACTIVATION_S2 -> false
            key == RcKeys.APPOPEN_REQUIRE_ACTIVATION_S2 -> false
            else -> null
        }
    }

    private fun forcedLong(key: String): Long? {
        if (!forceAds) return null
        return when (key) {
            RcKeys.AFTER_SPLASH_MIN_SESSION -> 1L          // reachable on first debug launch
            RcKeys.APPOPEN_MIN_SESSION -> 1L
            RcKeys.AFTER_ONB_MAX_PER_INSTALL -> 1L         // onboarding interstitial allowed once
            RcKeys.APPOPEN_MIN_BACKGROUND_SECONDS -> 0L
            // So a tester actually SEES every interstitial trigger fire (not just the first):
            // lift the per-session cap and drop the cooldowns. Production keeps the real caps.
            RcKeys.INTER_MAX_PER_SESSION -> 99L
            RcKeys.INTER_COOLDOWN_SECONDS -> 0L
            RcKeys.AFTER_SPLASH_COOLDOWN_SECONDS -> 0L
            RcKeys.APPOPEN_COOLDOWN_MINUTES -> 0L
            else -> null
        }
    }

    /** The fetched remote value for [key], or null if RC hasn't delivered one (→ use default). */
    private fun remote(key: String): FirebaseRemoteConfigValue? {
        val v = rc?.getValue(key) ?: return null
        return if (v.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) v else null
    }

    private companion object { const val TAG = "RemoteConfig" }
}
