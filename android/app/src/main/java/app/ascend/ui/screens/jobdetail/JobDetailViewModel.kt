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

    fun toggleSave() {
        val j = job.value ?: return
        viewModelScope.launch {
            if (saved.value) tracker.remove(j.id) else tracker.save(j, TrackStage.SAVED)
        }
    }

    fun markApplied() {
        val j = job.value ?: return
        viewModelScope.launch { tracker.save(j, TrackStage.APPLIED) }
    }
}
