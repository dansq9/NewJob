package app.ascend.ui.screens.jobdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.model.Job
import app.ascend.data.model.TrackStage
import app.ascend.data.repo.TrackerRepository
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    selectedJob: SelectedJobStore,
    private val tracker: TrackerRepository,
    private val monetization: app.ascend.monetization.MonetizationManager,
) : ViewModel() {

    private val jobId: String? = savedState["jobId"]

    // Fast path: the in-memory job tapped from a list. If it's missing or stale
    // (e.g. process death / deep link), recover from the local tracker (Room).
    private val _job = MutableStateFlow(selectedJob.selected.value?.takeIf { jobId == null || it.id == jobId })
    val job: StateFlow<Job?> = _job.asStateFlow()

    init {
        detailViewCount++   // session-scoped count of job-detail opens (drives close interstitial)
        if (_job.value == null && jobId != null) {
            viewModelScope.launch { _job.value = tracker.get(jobId)?.job }
        }
    }

    /**
     * ad_inter_after_job_detail_close — call when leaving the detail. Only eligible
     * after the user has viewed ≥2 details this session (per the spec trigger); the
     * manager still owns the real gate (paid/consent/RC/cap/cooldown/session 2).
     */
    fun onClose() {
        if (detailViewCount >= 2) monetization.requestFullScreen(app.ascend.monetization.Placement.INTER_AFTER_JOB_DETAIL_CLOSE)
    }

    private companion object { var detailViewCount = 0 }

    val saved: StateFlow<Boolean> = combine(job, tracker.tracked) { j, list ->
        j != null && list.any { it.job.id == j.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Current pipeline stage for this job, or null if it isn't tracked yet. */
    val stage: StateFlow<TrackStage?> = combine(job, tracker.tracked) { j, list ->
        if (j == null) null else list.firstOrNull { it.job.id == j.id }?.stage
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleSave() {
        val j = job.value ?: return
        viewModelScope.launch {
            if (saved.value) tracker.remove(j.id) else tracker.save(j, TrackStage.SAVED)
        }
    }

    fun markApplied() {
        val j = job.value ?: return
        viewModelScope.launch {
            // Preserve any existing notes/dates if already tracked — only advance the stage.
            if (tracker.stageOf(j.id) == null) tracker.save(j, TrackStage.APPLIED)
            else tracker.setStage(j.id, TrackStage.APPLIED)
        }
    }

    /** Set the pipeline stage; saves the job into the tracker first if needed. */
    fun setStage(stage: TrackStage) {
        val j = job.value ?: return
        viewModelScope.launch {
            if (tracker.stageOf(j.id) == null) tracker.save(j, stage)
            else tracker.setStage(j.id, stage)
        }
    }
}
