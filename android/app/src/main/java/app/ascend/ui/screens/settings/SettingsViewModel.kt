package app.ascend.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.BuildConfig
import app.ascend.data.billing.EntitlementRepository
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.TrackStage
import app.ascend.data.model.UserProfile
import app.ascend.data.repo.ResumeRepository
import app.ascend.data.repo.TrackerRepository
import app.ascend.monetization.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Profile stats shown in the header row (applied / saved / best ATS score). */
data class ProfileStats(val applied: Int = 0, val saved: Int = 0, val ats: Int? = null)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val consent: app.ascend.monetization.consent.ConsentManager,
    private val billing: BillingManager,
    entitlements: EntitlementRepository,
    tracker: TrackerRepository,
    resumes: ResumeRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile> =
        repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    /** Drives the Upgrade-to-Pro vs Pro-active card. */
    val isPro: StateFlow<Boolean> =
        entitlements.isPro.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Header stats derived from the tracker pipeline + saved resumes. */
    val stats: StateFlow<ProfileStats> =
        combine(tracker.tracked, resumes.library) { tracked, res ->
            ProfileStats(
                applied = tracked.count { it.stage != TrackStage.SAVED },
                saved = tracked.size,
                ats = res.mapNotNull { it.atsScore }.maxOrNull(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileStats())

    val versionName: String = BuildConfig.VERSION_NAME
    val versionCode: Int = BuildConfig.VERSION_CODE

    /** Show the "Privacy options" entry only where required (EEA/UK/CH). */
    fun privacyOptionsRequired(): Boolean = consent.privacyOptionsRequired()

    /** Re-present the UMP consent form so the user can change ad-consent choices. */
    fun showPrivacyOptions(activity: android.app.Activity) = consent.showPrivacyOptions(activity)

    /** Re-query Play Billing so restored subscriptions reflect immediately. */
    fun restore() { viewModelScope.launch { runCatching { billing.syncEntitlement() } } }

    fun save(name: String, role: String, location: String) {
        viewModelScope.launch {
            repo.update { it.copy(name = name.trim(), targetRole = role.trim(), location = location.trim()) }
        }
    }

    fun resetOnboarding(onDone: () -> Unit) {
        viewModelScope.launch { repo.resetOnboarding(); onDone() }
    }
}
