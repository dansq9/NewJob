package app.ascend.ui.screens.jobdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.model.TrackStage
import app.ascend.data.repo.TrackerRepository
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    selectedJob: SelectedJobStore,
    private val tracker: TrackerRepository,
) : ViewModel() {

    val job = selectedJob.selected

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
