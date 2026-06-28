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
import app.ascend.data.repo.TrackerRepository
import app.ascend.ui.SelectedJobStore
import app.ascend.ui.util.PickedFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResumeUi {
    data object Idle : ResumeUi
    data object Loading : ResumeUi
    /** [fixesApplied] gates the optimized score + download behind the rewarded Apply-fixes step. */
    data class Result(val data: OptimizeResponse, val fixesApplied: Boolean = false) : ResumeUi
    data class Error(@StringRes val messageRes: Int) : ResumeUi
}

/** Library + selection state for the resume screen. */
data class ResumeLibraryState(
    val resumes: List<ResumeRecord> = emptyList(),
    val selectedId: String? = null,
) {
    val selected: ResumeRecord? get() = resumes.firstOrNull { it.id == selectedId }
}

/**
 * The job the resume is optimized against. [title] null means a general (not job-specific) score.
 * A target can come from the opened job, the tracker, or a manually-typed title+company.
 */
data class OptimizeTarget(
    val title: String? = null,
    val company: String? = null,
    val jobId: String? = null,
) {
    val isGeneral: Boolean get() = title.isNullOrBlank()
}

/** A pickable job for the JD-attach sheet (from the user's tracker). */
data class TargetOption(val jobId: String, val title: String, val company: String)

@HiltViewModel
class ResumeViewModel @Inject constructor(
    private val api: AscendApi,
    private val selectedJob: SelectedJobStore,
    private val resumes: ResumeRepository,
    tracker: TrackerRepository,
    private val lastActions: ResumeLastActionStore,
    private val monetization: app.ascend.monetization.MonetizationManager,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    /** Record which resume action the user entered (for the hub's continue chip). */
    fun markEntered(action: ResumeAction) = lastActions.mark(action)

    // Seed the target from the job the user opened (if any), so the Job-Detail hot path pre-targets.
    private val _target = MutableStateFlow(
        selectedJob.selected.value?.let { OptimizeTarget(it.title, it.company, it.id) } ?: OptimizeTarget()
    )
    val target: StateFlow<OptimizeTarget> = _target.asStateFlow()

    /** Jobs the user is tracking — the "from your tracker" options in the JD-attach sheet. */
    val trackerOptions: StateFlow<List<TargetOption>> =
        tracker.tracked.map { list -> list.map { TargetOption(it.job.id, it.job.title, it.job.company) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGeneralTarget() { _target.value = OptimizeTarget() }
    fun setTrackerTarget(option: TargetOption) { _target.value = OptimizeTarget(option.title, option.company, option.jobId) }
    fun setManualTarget(title: String, company: String) {
        _target.value = OptimizeTarget(title.trim().ifBlank { null }, company.trim().ifBlank { null }, jobId = null)
    }

    val library: StateFlow<ResumeLibraryState> =
        combine(resumes.library, resumes.selectedResumeId) { list, sel ->
            ResumeLibraryState(list, sel ?: list.firstOrNull()?.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumeLibraryState())

    private val _ui = MutableStateFlow<ResumeUi>(ResumeUi.Idle)
    val ui: StateFlow<ResumeUi> = _ui.asStateFlow()

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
    fun rename(id: String, title: String?) { viewModelScope.launch { resumes.rename(id, title) } }
    fun duplicate(id: String) { viewModelScope.launch { resumes.duplicate(id) } }
    /** Re-insert a record removed via the Edit screen's delete → Undo. */
    fun restore(record: ResumeRecord) { viewModelScope.launch { resumes.restore(record) } }
    fun clearSnackbar() { _snackbar.value = null }

    /**
     * Analyze the resume for FREE — score + issues, no ad. The rewarded gate sits on Apply-fixes
     * and Download (monetization-spec: "score free; apply/download via rewarded").
     */
    fun optimize() {
        val t = _target.value
        val resumeId = library.value.selectedId
        // Manual targets have no jobId, so pass the title+company as a job description for grounding.
        val jobDescription = if (t.jobId == null && !t.isGeneral)
            listOfNotNull(t.title, t.company).joinToString(" · ") else null
        viewModelScope.launch {
            analytics.resumeOptimizeStart(hasTargetJob = !t.isGeneral)
            _ui.value = ResumeUi.Loading
            _ui.value = try {
                val res = api.optimizeResume(OptimizeRequest(resumeId = resumeId, jobId = t.jobId, jobDescription = jobDescription))
                ResumeUi.Result(res, fixesApplied = false)
            } catch (t2: Throwable) {
                analytics.resumeOptimizeFailed(app.ascend.analytics.errorTypeOf(t2))
                if (!t2.isOffline()) analytics.recordError(t2, mapOf("op" to "resume_optimize", "jobId" to t.jobId))
                ResumeUi.Error(if (t2.isOffline()) R.string.error_offline else R.string.error_optimize_failed)
            }
            // ad_inter_after_resume_score — after the value moment (free score shown). Manager gates it.
            if (_ui.value is ResumeUi.Result) {
                monetization.requestInterstitial(app.ascend.monetization.Placement.INTER_AFTER_RESUME_SCORE)
            }
        }
    }

    /**
     * Apply AI fixes — rewarded-gated (ad_rewarded_resume_optimize). On the earned grant, reveal the
     * optimized score + unlock download; record the improved score. Never grants on no-fill/close (rule 5).
     */
    fun applyFixes() {
        val current = _ui.value as? ResumeUi.Result ?: return
        if (current.fixesApplied) return
        val resumeId = library.value.selectedId
        val jobId = _target.value.jobId
        viewModelScope.launch {
            val outcome = monetization.showRewarded(app.ascend.monetization.Placement.REWARDED_RESUME_OPTIMIZE)
            if (outcome is app.ascend.monetization.RewardOutcome.NotGranted) return@launch  // keep the free score
            val gatedBy = app.ascend.monetization.gatedByOf(outcome)
            val data = current.data
            if (resumeId != null) {
                resumes.recordAtsScore(resumeId, data.optimizedScore ?: data.atsScore, jobId)
            }
            analytics.resumeOptimizeComplete(
                scoreBand = app.ascend.analytics.bandOf(data.optimizedScore ?: data.atsScore),
                gatedBy = gatedBy,
            )
            _ui.value = ResumeUi.Result(data, fixesApplied = true)
        }
    }

    fun reset() { _ui.value = ResumeUi.Idle }
}
