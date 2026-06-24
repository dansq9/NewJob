package app.ascend.analytics

import android.os.Build
import app.ascend.BuildConfig

/**
 * Non-sensitive diagnostic context attached to every recorded error: app
 * version/build and device/OS. Deliberately contains NO user data (no resume
 * content, interview answers, names, locations) so it is safe to ship to a
 * crash backend.
 */
object Diagnostics {
    fun base(): Map<String, Any?> = mapOf(
        "app_version" to BuildConfig.VERSION_NAME,
        "app_build" to BuildConfig.VERSION_CODE,
        "build_type" to (if (BuildConfig.DEBUG) "debug" else "release"),
        "os" to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
    )
}
