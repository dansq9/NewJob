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
    private var pickedResume: PickedFile? = null
    var saving by mutableStateOf(false)
        private set

    fun onResumePicked(file: PickedFile) {
        pickedResume = file
        resumeName = file.name
    }

    fun clearResume() {
        pickedResume = null
        resumeName = null
    }

    fun finish(onDone: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
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
            onDone()
        }
    }
}
