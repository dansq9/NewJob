package app.ascend.analytics

import android.util.Log
import app.ascend.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/** Logs to Logcat in debug; no-op in release. Replace binding with Firebase+Sentry. */
@Singleton
class NoopAnalytics @Inject constructor() : Analytics {
    override fun log(event: String, params: Map<String, Any?>) {
        if (BuildConfig.DEBUG) Log.d("Analytics", "$event ${if (params.isEmpty()) "" else params}")
    }
    override fun screen(name: String) { if (BuildConfig.DEBUG) Log.d("Analytics", "screen:$name") }
    override fun setUserProperty(key: String, value: String?) { if (BuildConfig.DEBUG) Log.d("Analytics", "prop:$key=$value") }

    override fun recordError(throwable: Throwable, context: Map<String, Any?>) {
        // Debug: surface to Logcat with diagnostics. Release: no-op until a real
        // crash backend (Sentry/Crashlytics) is bound — swap this impl, not the call sites.
        if (BuildConfig.DEBUG) {
            Log.w("Analytics", "non-fatal: ${context + Diagnostics.base()}", throwable)
        }
    }
}
