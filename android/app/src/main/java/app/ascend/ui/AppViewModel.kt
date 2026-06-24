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

    /** True while the branded full-screen-ad transition (splash or onboarding) should overlay the app. */
    val brandedAdTransition: StateFlow<Boolean> = monetization.brandedAdTransition

    init {
        // ONE bounded startup orchestration for the splash-interstitial ad path. Each external
        // dependency (RC, billing, UMP) is awaited with its own timeout so a slow/failed step
        // degrades to a clean suppression instead of losing a race or hanging the app. Non-ad
        // startup stays fail-open; nothing here blocks the UI (the start StateFlow drives that).
        viewModelScope.launch {
            // 1) Identity + session_start_enriched, AWAITED first so the session number is
            //    persisted before any ad eligibility is evaluated (acceptance: session known).
            analytics.startSession()
            // 2) Kick RC refresh + billing entitlement sync concurrently; await each (bounded) below.
            val rcRefresh = launch { monetization.refreshConfig() }
            val billingSync = launch { billing.syncEntitlement() }
            // 3) Start destination ready (profile/session readable, onboarded flag known).
            start.first { it is AppStart.Ready }
            // 4) Attempt RC refresh BEFORE evaluating placements; on timeout we proceed on the
            //    in-app spec defaults (fail-safe), never blocking.
            withTimeoutOrNull(RC_REFRESH_TIMEOUT_MS) { rcRefresh.join() }
            // 5) Attempt billing sync BEFORE forced-ad eval; if it can't resolve in time the
            //    entitlement stays unknown → splash suppresses via ENTITLEMENT_UNKNOWN (acceptable),
            //    rather than losing the race to parallel coroutine ordering.
            withTimeoutOrNull(BILLING_SYNC_TIMEOUT_MS) { billingSync.join() }
            // 6) Let UMP consent resolve (capped); if it never resolves, splash suppresses cleanly
            //    via CONSENT_NOT_READY inside the manager.
            withTimeoutOrNull(CONSENT_TIMEOUT_MS) { consent.canRequestAds.first { it } }
            // 7) The manager owns every remaining gate (never session 1, session-2 activation, paid,
            //    App-Open mutex, RC default OFF) and fails open. Safe even if a step above timed out.
            monetization.runSplashInterstitial()
        }
    }

    private companion object {
        const val RC_REFRESH_TIMEOUT_MS = 3000L
        const val BILLING_SYNC_TIMEOUT_MS = 3000L
        const val CONSENT_TIMEOUT_MS = 2500L
    }
}
