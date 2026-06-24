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
) : ViewModel() {
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
            }.isSuccess
            // Always clear the busy flag so the user can never get stuck on a failed save.
            saving = false
            if (ok) onDone() else saveFailed = true
        }
    }
}
