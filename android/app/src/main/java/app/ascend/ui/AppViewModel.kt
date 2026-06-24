package app.ascend.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.analytics.AnalyticsTracker
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.UserProfile
import app.ascend.monetization.MonetizationManager
import app.ascend.monetization.billing.BillingManager
import app.ascend.monetization.consent.ConsentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

sealed interface AppStart {
    data object Loading : AppStart
    data class Ready(val onboarded: Boolean) : AppStart
}

@HiltViewModel
class AppViewModel @Inject constructor(
    repo: ProfileRepository,
    analytics: AnalyticsTracker,
    private val monetization: MonetizationManager,
    billing: BillingManager,
    consent: ConsentManager,
) : ViewModel() {
    val start: StateFlow<AppStart> =
        repo.profile
            .map<UserProfile, AppStart> { AppStart.Ready(it.onboarded) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppStart.Loading)

    /** True while the branded splash-interstitial transition should overlay the app. */
    val splashTransition: StateFlow<Boolean> = monetization.splashTransition

    init {
        // Once per cold start: establish identity + user properties and fire
        // session_start_enriched. The single AnalyticsTracker is the only spine
        // (CLAUDE.md rule 8); it degrades to Logcat until google-services.json lands.
        viewModelScope.launch { analytics.startSession() }
        // Fetch Remote Config so all ad caps/cooldowns/toggles are current before
        // any placement is evaluated. Fails safe to the in-app spec defaults.
        viewModelScope.launch { monetization.refreshConfig() }
        // Resolve the entitlement against Play Billing. Until this completes the
        // state is entitlement_unknown → no forced ads (spec IAP states).
        viewModelScope.launch { billing.syncEntitlement() }
        // Splash/session-start interstitial: run once the start destination is known
        // and UMP consent has resolved. The manager owns every gate (never session 1,
        // session-2 activation, paid, App-Open mutex, RC default OFF, fail open).
        viewModelScope.launch {
            start.first { it is AppStart.Ready }
            withTimeoutOrNull(2500) { consent.canRequestAds.first { it } }   // let consent resolve (capped)
            monetization.runSplashInterstitial()
        }
    }
}
