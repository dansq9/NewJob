package app.ascend.ui.screens.resume

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.R
import app.ascend.analytics.AnalyticsTracker
import app.ascend.core.isOffline
import app.ascend.data.remote.platform.AscendApi
import app.ascend.data.remote.platform.OptimizeRequest
import app.ascend.data.remote.platform.OptimizeResponse
import app.ascend.data.repo.AddResumeResult
import app.ascend.data.repo.ResumeRecord
import app.ascend.data.repo.ResumeRepository
import app.ascend.ui.SelectedJobStore
import app.ascend.ui.util.PickedFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResumeUi {
    data object Idle : ResumeUi
    data object Loading : ResumeUi
    data class Result(val data: OptimizeResponse) : ResumeUi
    data class Error(@StringRes val messageRes: Int) : ResumeUi
}

/** Library + selection state for the resume screen. */
data class ResumeLibraryState(
    val resumes: List<ResumeRecord> = emptyList(),
    val selectedId: String? = null,
) {
    val selected: ResumeRecord? get() = resumes.firstOrNull { it.id == selectedId }
}

@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val api: AscendApi,
    private val selectedJob: SelectedJobStore,
    private val resumes: ResumeRepository,
    private val monetization: app.ascend.monetization.MonetizationManager,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    /** The job we're tailoring for, or null for a general (not job-specific) optimization. */
    val targetTitle: String? = selectedJob.selected.value?.title

    val library: StateFlow<ResumeLibraryState> =
        combine(resumes.library, resumes.selectedResumeId) { list, sel ->
            ResumeLibraryState(list, sel ?: list.firstOrNull()?.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumeLibraryState())

    private val _ui = MutableStateFlow<ResumeUi>(ResumeUi.Idle)
    val ui: StateFlow<ResumeUi> = _ui.asStateFlow()

    /** Transient one-shot message (rejected upload, added confirmation, etc.). */
    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    fun addResume(file: PickedFile) {
        viewModelScope.launch {
            when (val r = resumes.add(file)) {
                is AddResumeResult.Success -> {
                    _snackbar.value = "Added ${r.record.name}"
                    analytics.resumeUpload(
                        type = app.ascend.analytics.fileTypeOf(r.record.name),
                        sizeBand = app.ascend.analytics.sizeBandOf(r.record.sizeBytes),
                    )
                    analytics.coreActionDone(app.ascend.analytics.CoreAction.UPLOAD)   // activation
                }
                is AddResumeResult.Rejected -> {
                    analytics.resumeUploadFailed(app.ascend.analytics.ErrorType.UNSUPPORTED_FILE)
                    _snackbar.value = r.reason
                }
            }
        }
    }

    fun select(id: String) { viewModelScope.launch { resumes.select(id) } }
    fun remove(id: String) { viewModelScope.launch { resumes.remove(id) } }
    fun clearSnackbar() { _snackbar.value = null }

    fun optimize() {
        // null jobId = general optimization (no specific job selected). Never send a fake "demo" id.
        val jobId = selectedJob.selected.value?.id
        val resumeId = library.value.selectedId
        viewModelScope.launch {
            // Free users watch a rewarded ad to unlock one optimization; Pro bypasses.
            // The reward is granted only on the earned-reward callback (rule 5).
            val outcome = monetization.showRewarded(app.ascend.monetization.Placement.REWARDED_RESUME_OPTIMIZE)
            if (outcome is app.ascend.monetization.RewardOutcome.NotGranted) return@launch  // no reward → keep screen
            val gatedBy = app.ascend.monetization.gatedByOf(outcome)
            analytics.resumeOptimizeStart(hasTargetJob = jobId != null)
            _ui.value = ResumeUi.Loading
            _ui.value = try {
                val res = api.optimizeResume(OptimizeRequest(resumeId = resumeId, jobId = jobId))
                if (resumeId != null) {
                    resumes.recordAtsScore(resumeId, res.optimizedScore ?: res.atsScore, jobId)
                }
                analytics.resumeOptimizeComplete(
                    scoreBand = app.ascend.analytics.bandOf(res.optimizedScore ?: res.atsScore),
                    gatedBy = gatedBy,
                )
                ResumeUi.Result(res)
            } catch (t: Throwable) {
                analytics.resumeOptimizeFailed(app.ascend.analytics.errorTypeOf(t))
                // Record only metadata (op + jobId) — never resume content. Skip offline (expected).
                if (!t.isOffline()) analytics.recordError(t, mapOf("op" to "resume_optimize", "jobId" to jobId))
                ResumeUi.Error(if (t.isOffline()) R.string.error_offline else R.string.error_optimize_failed)
            }
            // ad_inter_after_resume_score — after the value moment (result shown). The manager
            // gates it (paid/consent/RC/cap 1·session/cooldown/first-eligible session 2).
            if (_ui.value is ResumeUi.Result) {
                monetization.requestFullScreen(app.ascend.monetization.Placement.INTER_AFTER_RESUME_SCORE)
            }
        }
    }

    fun reset() { _ui.value = ResumeUi.Idle }
}
