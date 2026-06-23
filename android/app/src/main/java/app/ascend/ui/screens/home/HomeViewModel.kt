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
    private val ads: app.ascend.monetization.ads.AdsManager,
    profileRepo: ProfileRepository,
) : ViewModel() {

    init {
        // Interstitial after splash, once per process (Pro users are skipped).
        if (!interstitialShown) {
            interstitialShown = true
            viewModelScope.launch { ads.showInterstitial(app.ascend.monetization.ads.AdPlacement.SPLASH_INTERSTITIAL) }
        }
    }

    private companion object { var interstitialShown = false }

    val profile: StateFlow<UserProfile> =
        profileRepo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile())

    private val _topMatches = MutableStateFlow<Resource<List<Job>>>(Resource.Loading)
    val topMatches: StateFlow<Resource<List<Job>>> = _topMatches.asStateFlow()

    init {
        // (Re)load top matches whenever the target role / location changes.
        viewModelScope.launch {
            profileRepo.profile
                .map { it.targetRole to it.location }
                .distinctUntilChanged()
                .collectLatest { (role, location) ->
                    if (role.isBlank()) { _topMatches.value = Resource.Success(emptyList()); return@collectLatest }
                    _topMatches.value = Resource.Loading
                    val res = jobs.search(query = role, location = location.ifBlank { null })
                    _topMatches.value = when (res) {
                        is Resource.Success -> Resource.Success(res.data.take(4))
                        else -> res
                    }
                }
        }
    }

    fun select(job: Job) = selectedJob.select(job)
}
