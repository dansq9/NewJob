package app.ascend.ui.screens.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.model.TrackStage
import app.ascend.data.repo.TrackedJob
import app.ascend.data.repo.TrackerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TrackerSort(val label: String) {
    RECENT("Recent"), COMPANY("Company"), STAGE("Stage")
}

data class TrackerUiState(
    val query: String = "",
    val sort: TrackerSort = TrackerSort.RECENT,
    val grouped: Map<TrackStage, List<TrackedJob>> = emptyMap(),
    val total: Int = 0,
) {
    val active: Int get() = grouped.filterKeys { it != TrackStage.CLOSED }.values.sumOf { it.size }
}

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val tracker: TrackerRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(TrackerSort.RECENT)

    val state: StateFlow<TrackerUiState> =
        combine(tracker.tracked, query, sort) { list, q, s ->
            val filtered =
                if (q.isBlank()) list
                else list.filter {
                    it.job.title.contains(q, true) ||
                        it.job.company.contains(q, true) ||
                        it.job.location.contains(q, true)
                }
            val sorted = when (s) {
                TrackerSort.RECENT -> filtered.sortedByDescending { it.updatedAt }
                TrackerSort.COMPANY -> filtered.sortedBy { it.job.company.lowercase() }
                TrackerSort.STAGE -> filtered.sortedBy { it.stage.ordinal }
            }
            TrackerUiState(
                query = q,
                sort = s,
                grouped = sorted.groupBy { it.stage },
                total = list.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackerUiState())

    fun setQuery(q: String) { query.value = q }
    fun setSort(s: TrackerSort) { sort.value = s }

    fun move(jobId: String, to: TrackStage) = viewModelScope.launch { tracker.setStage(jobId, to) }
    fun remove(jobId: String) = viewModelScope.launch { tracker.remove(jobId) }
    fun saveNotes(jobId: String, notes: String?) = viewModelScope.launch { tracker.setNotes(jobId, notes) }
    fun saveSchedule(jobId: String, interviewDate: Long?, reminderAt: Long?) =
        viewModelScope.launch { tracker.setSchedule(jobId, interviewDate, reminderAt) }
    fun close(jobId: String, reason: String?) = viewModelScope.launch { tracker.close(jobId, reason) }
}
