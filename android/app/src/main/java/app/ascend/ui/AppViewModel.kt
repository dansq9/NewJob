package app.ascend.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.analytics.AnalyticsTracker
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.UserProfile
import app.ascend.monetization.MonetizationManager
import app.ascend.monetization.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppStart {
    data object Loading : AppStart
    data class Ready(val onboarded: Boolean) : AppStart
}

@HiltViewModel
class AppViewModel @Inject constructor(
    repo: ProfileRepository,
    analytics: AnalyticsTracker,
    monetization: MonetizationManager,
    billing: BillingManager,
) : ViewModel() {
    val start: StateFlow<AppStart> =
        repo.profile
            .map<UserProfile, AppStart> { AppStart.Ready(it.onboarded) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppStart.Loading)

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
    }
}
