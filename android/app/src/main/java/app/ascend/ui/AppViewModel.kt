package app.ascend.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AppStart {
    data object Loading : AppStart
    data class Ready(val onboarded: Boolean) : AppStart
}

@HiltViewModel
class AppViewModel @Inject constructor(
    repo: ProfileRepository,
) : ViewModel() {
    val start: StateFlow<AppStart> =
        repo.profile
            .map<UserProfile, AppStart> { AppStart.Ready(it.onboarded) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AppStart.Loading)
}
