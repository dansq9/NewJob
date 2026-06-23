package app.ascend.ui.screens.resume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.remote.platform.AscendApi
import app.ascend.data.remote.platform.OptimizeRequest
import app.ascend.data.remote.platform.OptimizeResponse
import app.ascend.ui.SelectedJobStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResumeUi {
    data object Idle : ResumeUi
    data object Loading : ResumeUi
    data class Result(val data: OptimizeResponse) : ResumeUi
    data class Error(val message: String) : ResumeUi
}

@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val api: AscendApi,
    private val selectedJob: SelectedJobStore,
) : ViewModel() {

    val targetTitle = selectedJob.selected.value?.title ?: "Senior Product Manager · Northwind"

    private val _ui = MutableStateFlow<ResumeUi>(ResumeUi.Idle)
    val ui: StateFlow<ResumeUi> = _ui.asStateFlow()

    fun optimize() {
        val jobId = selectedJob.selected.value?.id ?: "demo"
        _ui.value = ResumeUi.Loading
        viewModelScope.launch {
            _ui.value = try {
                ResumeUi.Result(api.optimizeResume(OptimizeRequest(jobId = jobId)))
            } catch (t: Throwable) {
                ResumeUi.Error(t.message ?: "Optimization failed. Check the Ascend API configuration.")
            }
        }
    }

    fun reset() { _ui.value = ResumeUi.Idle }
}
