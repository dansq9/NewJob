package app.ascend.ui.screens.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.core.Resource
import app.ascend.data.local.ProfileRepository
import app.ascend.data.model.Job
import app.ascend.data.model.TrackStage
import app.ascend.data.remote.jsearch.JSearchRepository
import app.ascend.data.repo.TrackerRepository
import app.ascend.monetization.AdDecision
import app.ascend.monetization.MonetizationManager
import app.ascend.monetization.Placement
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JobFilters(
    val datePosted: String = "all",            // all | today | 3days | week | month
    val employmentTypes: Set<String> = emptySet(), // FULLTIME, PARTTIME, CONTRACTOR, INTERN
    val remoteOnly: Boolean = false,
) {
    val activeCount: Int
        get() = (if (datePosted != "all") 1 else 0) + employmentTypes.size + (if (remoteOnly) 1 else 0)
}

data class JobsUiState(
    val query: String = "",
    val location: String = "",
    val filters: JobFilters = JobFilters(),
    val jobs: List<Job> = emptyList(),
    val status: Resource<Unit> = Resource.Loading,   // first-page status
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val savedIds: Set<String> = emptySet(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobs: JSearchRepository,
    private val tracker: TrackerRepository,
    private val selectedJob: SelectedJobStore,
    private val profileRepo: ProfileRepository,
    private val monetization: MonetizationManager,
    private val analytics: app.ascend.analytics.AnalyticsTracker,
) : ViewModel() {

    private val _state = MutableStateFlow(JobsUiState())
    val state: StateFlow<JobsUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var page = 1
    private val seenIds = mutableSetOf<String>()

    // ad_inter_after_search_batch: count search batches (new search + load more); attempt an
    // interstitial after the 2nd, then on alternate batches. MonetizationManager owns the real
    // gate (paid/consent/RC/cap/cooldown/first-eligible session 2), so this only marks intent.
    private var searchBatches = 0

    private fun maybeShowSearchInterstitial() {
        searchBatches++
        if (searchBatches >= 2 && searchBatches % 2 == 0) {
            monetization.requestFullScreen(Placement.INTER_AFTER_SEARCH_BATCH)
        }
    }

    /**
     * Whether the native job-list ad may render — decided by MonetizationManager
     * (Pro → hidden, consent gate, RC toggle). Starts false so the slot collapses
     * by default and never flashes a blank container (rule 4).
     */
    val nativeAdAllowed: StateFlow<Boolean> =
        monetization.nativeAd(Placement.NATIVE_JOB_LIST)
            .map { it is AdDecision.Show }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Organic rows between native ads (RC-controlled; "first 4, then every N"). */
    val nativeFrequency: Int get() = monetization.nativeListFrequency()

    init {
        viewModelScope.launch {
            tracker.tracked.collect { list -> _state.update { it.copy(savedIds = list.map { t -> t.job.id }.toSet()) } }
        }
        viewModelScope.launch {
            val p = profileRepo.profile.first()
            _state.update { it.copy(query = p.targetRole.ifBlank { "Product Manager" }, location = p.location) }
            queryFlow.value = _state.value.query
        }
        // Debounced auto-search as the user types.
        viewModelScope.launch {
            queryFlow.debounce(350).distinctUntilChanged().collectLatest { reload() }
        }
    }

    fun onQueryChange(q: String) { _state.update { it.copy(query = q) }; queryFlow.value = q }
    fun onLocationChange(l: String) = _state.update { it.copy(location = l) }
    fun search() = reload()

    fun setFilters(filters: JobFilters) { _state.update { it.copy(filters = filters) }; reload() }
    fun toggleRemote() = setFilters(_state.value.filters.copy(remoteOnly = !_state.value.filters.remoteOnly))

    private fun reload() {
        page = 1; seenIds.clear()
        _state.update { it.copy(status = Resource.Loading, jobs = emptyList(), endReached = false, loadingMore = false) }
        viewModelScope.launch {
            when (val res = fetch(page)) {
                is Resource.Success -> {
                    val fresh = res.data.dedup()
                    _state.update { it.copy(jobs = fresh, status = Resource.Success(Unit), endReached = res.data.isEmpty()) }
                    analytics.coreActionDone(app.ascend.analytics.CoreAction.SEARCH)   // activation (drives session-2 gate)
                    maybeShowSearchInterstitial()
                }
                is Resource.Error -> _state.update { it.copy(status = res) }
                Resource.Loading -> Unit
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loadingMore || s.endReached || s.status !is Resource.Success || page >= MAX_PAGES) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            when (val res = fetch(page + 1)) {
                is Resource.Success -> {
                    page += 1
                    val more = res.data.dedup()
                    _state.update { it.copy(jobs = it.jobs + more, loadingMore = false, endReached = res.data.isEmpty()) }
                }
                is Resource.Error -> _state.update { it.copy(loadingMore = false, endReached = true) }
                Resource.Loading -> Unit
            }
        }
    }

    private suspend fun fetch(p: Int): Resource<List<Job>> {
        val s = _state.value
        return jobs.search(
            query = s.query, location = s.location.ifBlank { null }, page = p,
            remoteOnly = s.filters.remoteOnly, employmentTypes = s.filters.employmentTypes.toList(),
            datePosted = s.filters.datePosted,
        )
    }

    /** Drop duplicates already shown (JSearch repeats across pages). */
    private fun List<Job>.dedup(): List<Job> = filter { seenIds.add(it.id) }

    fun select(job: Job) = selectedJob.select(job)

    fun toggleSave(job: Job) {
        viewModelScope.launch {
            if (_state.value.savedIds.contains(job.id)) {
                tracker.remove(job.id)
            } else {
                tracker.save(job, TrackStage.SAVED)
                analytics.coreActionDone(app.ascend.analytics.CoreAction.SAVE)   // activation
            }
        }
    }

    private companion object { const val MAX_PAGES = 10 }
}
