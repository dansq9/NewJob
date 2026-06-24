package app.ascend

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.ascend.monetization.MonetizationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AscendApp : Application() {

    @Inject lateinit var monetization: MonetizationManager

    override fun onCreate() {
        super.onCreate()
        // Drive the app-open ad off process foreground/background. The first ON_START
        // (cold start) is ignored inside the manager — app-open shows only on a real
        // return from background, gated by every suppression rule (CLAUDE.md rules 1-4).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { monetization.onForeground() }
            override fun onStop(owner: LifecycleOwner) { monetization.onBackground() }
        })
    }
}
