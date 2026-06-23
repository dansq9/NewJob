package app.ascend.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.core.Resource
import app.ascend.data.model.Job
import app.ascend.data.remote.jsearch.JSearchRepository
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobs: JSearchRepository,
    private val selectedJob: SelectedJobStore,
) : ViewModel() {

    // TODO: source role/location/name from the signed-in profile (platform API / DataStore).
    val userName = "Alex Morgan"
    val role = "Senior Product Manager"
    val location = "San Francisco"

    private val _topMatches = MutableStateFlow<Resource<List<Job>>>(Resource.Loading)
    val topMatches: StateFlow<Resource<List<Job>>> = _topMatches.asStateFlow()

    init {
        viewModelScope.launch {
            val res = jobs.search(query = role, location = location)
            _topMatches.update {
                when (res) {
                    is Resource.Success -> Resource.Success(res.data.take(4))
                    else -> res
                }
            }
        }
    }

    fun select(job: Job) = selectedJob.select(job)
}
