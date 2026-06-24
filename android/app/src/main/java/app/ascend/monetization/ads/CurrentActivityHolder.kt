package app.ascend.monetization.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the currently-resumed [Activity]. Full-screen AdMob formats (interstitial, app-open,
 * rewarded) require an Activity to show over, but [AdsManager]'s show methods are Activity-free
 * (the manager owns policy, not UI). This holder bridges that gap without leaking the Activity:
 * it keeps only a [WeakReference], updated from Application lifecycle callbacks.
 *
 * Registered once in [app.ascend.AscendApp.onCreate]. If no Activity is resumed (e.g. app in
 * background), [current] is null and the ad show simply fails open.
 */
object CurrentActivityHolder {
    @Volatile private var ref: WeakReference<Activity>? = null

    /** The resumed Activity, or null if none is currently in the foreground. */
    val current: Activity? get() = ref?.get()

    fun register(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) { ref = WeakReference(activity) }
            override fun onActivityPaused(activity: Activity) { if (ref?.get() === activity) ref = null }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
