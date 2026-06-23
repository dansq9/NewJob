package app.ascend.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.core.Resource
import app.ascend.data.model.Job
import app.ascend.data.model.TrackStage
import app.ascend.data.remote.jsearch.JSearchRepository
import app.ascend.data.repo.TrackerRepository
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobsUiState(
    val query: String = "Product Manager",
    val location: String = "San Francisco · Remote",
    val remoteOnly: Boolean = false,
    val results: Resource<List<Job>> = Resource.Loading,
    val savedIds: Set<String> = emptySet(),
)

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobs: JSearchRepository,
    private val tracker: TrackerRepository,
    private val selectedJob: SelectedJobStore,
    private val profileRepo: app.ascend.data.local.ProfileRepository,
    entitlements: app.ascend.data.billing.EntitlementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JobsUiState(query = "", location = ""))
    val state: StateFlow<JobsUiState> = _state.asStateFlow()

    /** Native ad cards (every 5 listings) show only for non-Pro users. */
    val adsEnabled: StateFlow<Boolean> =
        entitlements.isPro.map { !it }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch {
            tracker.tracked.collect { list -> _state.update { it.copy(savedIds = list.map { t -> t.job.id }.toSet()) } }
        }
        viewModelScope.launch {
            val p = profileRepo.profile.first()
            _state.update {
                it.copy(
                    query = it.query.ifBlank { p.targetRole.ifBlank { "Product Manager" } },
                    location = it.location.ifBlank { p.location },
                )
            }
            search()
        }
    }

    fun onQueryChange(q: String) = _state.update { it.copy(query = q) }
    fun onLocationChange(l: String) = _state.update { it.copy(location = l) }
    fun toggleRemote() { _state.update { it.copy(remoteOnly = !it.remoteOnly) }; search() }

    fun search() {
        val s = _state.value
        _state.update { it.copy(results = Resource.Loading) }
        viewModelScope.launch {
            val res = jobs.search(query = s.query, location = s.location, remoteOnly = s.remoteOnly)
            _state.update { it.copy(results = res) }
        }
    }

    fun select(job: Job) = selectedJob.select(job)

    fun toggleSave(job: Job) {
        viewModelScope.launch {
            if (_state.value.savedIds.contains(job.id)) tracker.remove(job.id)
            else tracker.save(job, TrackStage.SAVED)
        }
    }
}
