package app.ascend.ui.screens.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {
    var name by mutableStateOf("")
    var role by mutableStateOf("")
    var location by mutableStateOf("")
    var resumeName by mutableStateOf<String?>(null)
    var saving by mutableStateOf(false)
        private set

    fun finish(onDone: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
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
