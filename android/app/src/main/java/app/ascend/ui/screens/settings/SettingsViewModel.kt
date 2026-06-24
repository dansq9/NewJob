package app.ascend.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.BuildConfig
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: ProfileRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfile> =
        repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    val versionName: String = BuildConfig.VERSION_NAME
    val versionCode: Int = BuildConfig.VERSION_CODE

    fun save(name: String, role: String, location: String) {
        viewModelScope.launch {
            repo.update { it.copy(name = name.trim(), targetRole = role.trim(), location = location.trim()) }
        }
    }

    fun resetOnboarding(onDone: () -> Unit) {
        viewModelScope.launch { repo.resetOnboarding(); onDone() }
    }
}
