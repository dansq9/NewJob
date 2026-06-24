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

    // Native frequency (job list)
    const val NATIVE_JOB_LIST_FREQUENCY = "ads.native.job_list.frequency"

    // App-open
    const val APPOPEN_MIN_SESSION = "ads.appopen.resume.min_session"
    const val APPOPEN_REQUIRE_ACTIVATION_S2 = "ads.appopen.resume.require_activation_for_session_2"
    const val APPOPEN_COOLDOWN_MINUTES = "ads.appopen.resume.cooldown_minutes"
    const val APPOPEN_MAX_PER_SESSION = "ads.appopen.resume.max_per_session"
    const val APPOPEN_MAX_PER_DAY = "ads.appopen.resume.max_per_day"
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
    RcKeys.REWARD_LOAD_TIMEOUT_MS to 4000L,
    RcKeys.NATIVE_LOAD_TIMEOUT_MS to 0L,
    RcKeys.APPOPEN_LOAD_TIMEOUT_MS to 1200L,

    RcKeys.INTER_COOLDOWN_SECONDS to 180L,
    RcKeys.INTER_MAX_PER_SESSION to 1L,

    RcKeys.NATIVE_JOB_LIST_FREQUENCY to 8L,

    RcKeys.APPOPEN_MIN_SESSION to 3L,
    RcKeys.APPOPEN_REQUIRE_ACTIVATION_S2 to true,
    RcKeys.APPOPEN_COOLDOWN_MINUTES to 45L,
    RcKeys.APPOPEN_MAX_PER_SESSION to 1L,
    RcKeys.APPOPEN_MAX_PER_DAY to 2L,
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

    fun bool(key: String): Boolean = remote(key)?.asBoolean() ?: (DEFAULTS[key] as? Boolean ?: false)
    fun long(key: String): Long = remote(key)?.asLong() ?: (DEFAULTS[key] as? Long ?: 0L)
    fun string(key: String): String =
        remote(key)?.asString()?.takeIf { it.isNotEmpty() } ?: (DEFAULTS[key] as? String ?: "")

    /** The fetched remote value for [key], or null if RC hasn't delivered one (→ use default). */
    private fun remote(key: String): FirebaseRemoteConfigValue? {
        val v = rc?.getValue(key) ?: return null
        return if (v.source == FirebaseRemoteConfig.VALUE_SOURCE_REMOTE) v else null
    }

    private companion object { const val TAG = "RemoteConfig" }
}
