package app.ascend.ui.screens.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.UserProfile
import app.ascend.data.repo.ResumeRepository
import app.ascend.ui.util.PickedFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val resumes: ResumeRepository,
    private val analytics: app.ascend.analytics.AnalyticsTracker,
    private val monetization: app.ascend.monetization.MonetizationManager,
    private val onboardingConfigProvider: OnboardingConfigProvider,
) : ViewModel() {

    /** RC-controlled tour + animation config (validated, clamped, fail-open). Loaded once. */
    val onboardingConfig: OnboardingConfig = onboardingConfigProvider.load()

    // Loaded from persistence in init; default to the suppressing value until loaded.
    private var returningUser by mutableStateOf(true)
    private var tourAlreadyShown by mutableStateOf(true)
    /** False until the persisted suppression flags are read — gate tour triggers on this. */
    var tourStateLoaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            returningUser = repo.onboardedOnce()
            tourAlreadyShown = repo.tourShown()
            tourStateLoaded = true
        }
    }

    /** Eligible tour-card count at the trigger moment (0 = skip). [resumeUploaded] is read live. */
    fun eligibleTourCards(resumeUploaded: Boolean): Int =
        onboardingConfigProvider.eligibleCardCount(onboardingConfig, resumeUploaded, returningUser, tourAlreadyShown)

    fun onTourView(cardIndex: Int) =
        analytics.onboardingTourView(onboardingConfig.tourVariant, cardIndex, onboardingConfig.placement)
    fun onTourSkip(cardIndex: Int) {
        analytics.onboardingTourSkip(onboardingConfig.tourVariant, cardIndex, onboardingConfig.placement)
        if (onboardingConfig.oncePerInstall) viewModelScope.launch { repo.markTourShown() }
    }
    fun onTourComplete(cardsSeen: Int) {
        analytics.onboardingTourComplete(onboardingConfig.tourVariant, cardsSeen, onboardingConfig.placement)
        if (onboardingConfig.oncePerInstall) viewModelScope.launch { repo.markTourShown() }
    }
    fun onAnimationVariantApplied(placement: String) =
        analytics.onboardingAnimationVariant(onboardingConfig.animationVariant, placement)

    /** onboarding_step — the user advanced past [step] (skipped = field left blank). */
    fun logStep(step: app.ascend.analytics.OnboardingStep, skipped: Boolean) = analytics.onboardingStep(step, skipped)
    var name by mutableStateOf("")
    var role by mutableStateOf("")
    var location by mutableStateOf("")
    var resumeName by mutableStateOf<String?>(null)
    var resumeError by mutableStateOf<String?>(null)
        private set
    private var pickedResume: PickedFile? = null
    var saving by mutableStateOf(false)
        private set
    var saveFailed by mutableStateOf(false)
        private set

    fun onResumePicked(file: PickedFile) {
        // Validate up front so the user gets a clear rejection instead of a silent drop.
        val reason = resumes.reasonToReject(file)
        if (reason != null) {
            resumeError = reason; pickedResume = null; resumeName = null
        } else {
            resumeError = null; pickedResume = file; resumeName = file.name
        }
    }

    fun clearResume() {
        pickedResume = null
        resumeName = null
        resumeError = null
    }

    fun finish(onDone: () -> Unit) {
        if (saving) return
        saving = true
        saveFailed = false
        viewModelScope.launch {
            val ok = runCatching {
                pickedResume?.let { resumes.add(it) }
                repo.save(
                    UserProfile(
                        name = name.trim(),
                        targetRole = role.trim(),
                        location = location.trim(),
                        resumeName = resumeName,
                        onboarded = true,
                    )
                )
                repo.markOnboardedOnce()   // returning-user suppression on any future re-onboarding
            }.isSuccess
            // Always clear the busy flag so the user can never get stuck on a failed save.
            saving = false
            if (ok) {
                analytics.onboardingComplete(
                    rolePresent = role.isNotBlank(),
                    roleCategory = app.ascend.analytics.roleCategoryOf(role),
                    location = app.ascend.analytics.LocationType.UNKNOWN,   // onboarding captures a city, not remote/onsite
                    resumeUploaded = resumeName != null,
                )
                // After onboarding_complete is persisted+logged, before the first main screen:
                // run the onboarding-complete interstitial (RC default OFF; fails open; never blocks nav).
                monetization.runOnboardingInterstitial()
                onDone()
            } else {
                saveFailed = true
            }
        }
    }
}
