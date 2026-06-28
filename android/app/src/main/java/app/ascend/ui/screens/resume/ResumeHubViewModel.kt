package app.ascend.ui.screens.resume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.analytics.AnalyticsTracker
import app.ascend.data.repo.AddResumeResult
import app.ascend.data.repo.ResumeRepository
import app.ascend.ui.util.PickedFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the Resume hub — just enough state to drive the Edit empty-state and the upload shortcut. */
@HiltViewModel
class ResumeHubViewModel @Inject constructor(
    private val resumes: ResumeRepository,
    private val lastActions: ResumeLastActionStore,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    /** Number of saved resumes — drives whether "Edit" is a live path or routes to Build. */
    val savedCount: StateFlow<Int> =
        resumes.library.map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** The action the user last entered this session, or null — powers the "Continue" chip. */
    val lastAction: ResumeAction? get() = lastActions.last

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()
    fun clearSnackbar() { _snackbar.value = null }

    /** Add an uploaded file to the library and, on success, run [onAdded] (jump to the optimizer). */
    fun addResumeThenOptimize(file: PickedFile, onAdded: () -> Unit) {
        viewModelScope.launch {
            when (val r = resumes.add(file)) {
                is AddResumeResult.Success -> {
                    analytics.resumeUpload(
                        type = app.ascend.analytics.fileTypeOf(r.record.name),
                        sizeBand = app.ascend.analytics.sizeBandOf(r.record.sizeBytes),
                    )
                    analytics.coreActionDone(app.ascend.analytics.CoreAction.UPLOAD)
                    onAdded()
                }
                is AddResumeResult.Rejected -> {
                    analytics.resumeUploadFailed(app.ascend.analytics.ErrorType.UNSUPPORTED_FILE)
                    _snackbar.value = r.reason
                }
            }
        }
    }
}
