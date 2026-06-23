package app.ascend.ui.screens.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.model.TrackStage
import app.ascend.data.repo.TrackedJob
import app.ascend.data.repo.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val tracker: TrackerRepository,
) : ViewModel() {

    val grouped: StateFlow<Map<TrackStage, List<TrackedJob>>> =
        tracker.tracked
            .map { list -> list.groupBy { it.stage } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun move(jobId: String, to: TrackStage) = viewModelScope.launch { tracker.setStage(jobId, to) }
    fun remove(jobId: String) = viewModelScope.launch { tracker.remove(jobId) }
}
