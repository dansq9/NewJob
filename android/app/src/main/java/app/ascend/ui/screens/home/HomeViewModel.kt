package app.ascend.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.core.Resource
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.Job
import app.ascend.data.model.UserProfile
import app.ascend.data.remote.jsearch.JSearchRepository
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobs: JSearchRepository,
    private val selectedJob: SelectedJobStore,
    profileRepo: ProfileRepository,
) : ViewModel() {

    // Note: there is no sanctioned home-open interstitial placement in the
    // monetization spec, so the screen requests no full-screen ad here. All ad
    // decisions live in MonetizationManager (rule 2); screens never call the SDK.

    val profile: StateFlow<UserProfile> =
        profileRepo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    private val _topMatches = MutableStateFlow<Resource<List<Job>>>(Resource.Loading)
    val topMatches: StateFlow<Resource<List<Job>>> = _topMatches.asStateFlow()

    @Volatile private var lastRole = ""
    @Volatile private var lastLocation = ""

    init {
        // (Re)load top matches whenever the target role / location changes.
        viewModelScope.launch {
            profileRepo.profile
                .map { it.targetRole to it.location }
                .distinctUntilChanged()
                .collectLatest { (role, location) ->
                    lastRole = role; lastLocation = location
                    load(role, location)
                }
        }
    }

    private suspend fun load(role: String, location: String) {
        if (role.isBlank()) { _topMatches.value = Resource.Success(emptyList()); return }
        _topMatches.value = Resource.Loading
        val res = jobs.search(query = role, location = location.ifBlank { null })
        _topMatches.value = when (res) {
            is Resource.Success -> Resource.Success(res.data.take(4))
            else -> res
        }
    }

    /** Retry the top-matches fetch (e.g. after an error / reconnect). */
    fun retry() {
        viewModelScope.launch { load(lastRole, lastLocation) }
    }

    fun select(job: Job) = selectedJob.select(job)
}
