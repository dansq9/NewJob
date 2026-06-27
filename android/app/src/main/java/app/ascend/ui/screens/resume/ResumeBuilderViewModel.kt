package app.ascend.ui.screens.resume

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.R
import app.ascend.analytics.AnalyticsTracker
import app.ascend.core.isOffline
import app.ascend.data.remote.platform.AscendApi
import app.ascend.data.remote.platform.GenerateRequest
import app.ascend.data.remote.platform.GenerateResponse
import app.ascend.data.repo.ResumeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** What the guided form collects. Kept as plain fields in v1 (repeating roles come later). */
@Serializable
data class BuilderForm(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val summary: String = "",
    val experience: String = "",
    val education: String = "",
    val skills: String = "",
    /** Lets a no-experience user lead with Education/Projects/Skills instead of work history. */
    val noExperienceYet: Boolean = false,
)

sealed interface BuilderUi {
    data object Editing : BuilderUi
    data object Generating : BuilderUi
    data class Done(val data: GenerateResponse) : BuilderUi
    data class Error(@StringRes val messageRes: Int) : BuilderUi
}

@HiltViewModel
class ResumeBuilderViewModel @Inject constructor(
    private val api: AscendApi,
    private val resumes: ResumeRepository,
    private val draftStore: ResumeDraftStore,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _form = MutableStateFlow(BuilderForm())
    val form = _form.asStateFlow()

    private val _ui = MutableStateFlow<BuilderUi>(BuilderUi.Editing)
    val ui = _ui.asStateFlow()

    /** True while an AI-write call for the summary is in flight (drives the button spinner). */
    private val _aiBusy = MutableStateFlow(false)
    val aiBusy = _aiBusy.asStateFlow()

    /** Set when editing an existing built resume (Edit flow); null when building a new one. */
    private var editingId: String? = null
    private var loaded = false

    /**
     * Initialize the form once: from a saved built resume (Edit flow) when [resumeId] is non-null,
     * otherwise from a pending voice transcript (Build-by-voice), otherwise blank (new resume).
     */
    fun start(resumeId: String?) {
        if (loaded) return
        loaded = true
        if (resumeId != null) {
            editingId = resumeId
            viewModelScope.launch {
                val content = resumes.get(resumeId)?.content ?: return@launch
                runCatching { json.decodeFromString(BuilderForm.serializer(), content) }
                    .getOrNull()?.let { _form.value = it }
            }
        } else {
            draftStore.consumeTranscript()?.takeIf { it.isNotBlank() }
                ?.let { _form.value = _form.value.copy(experience = it) }
        }
    }

    fun update(transform: (BuilderForm) -> BuilderForm) { _form.value = transform(_form.value) }

    /**
     * A friendly, fixable "resume strength" 0–100 derived from completeness — never a pass/fail
     * grade. Contact + summary are the backbone; experience/education/skills round it out.
     */
    val strength: Int
        get() {
            val f = _form.value
            var score = 0
            if (f.name.isNotBlank()) score += 12
            if (f.email.isNotBlank()) score += 10
            if (f.location.isNotBlank()) score += 8
            if (f.summary.length >= 40) score += 25 else if (f.summary.isNotBlank()) score += 10
            if (f.experience.length >= 40) score += 20 else if (f.experience.isNotBlank()) score += 8
            if (f.education.isNotBlank()) score += 12
            if (f.skills.isNotBlank()) score += 13
            return score.coerceIn(0, 100)
        }

    /** Enough to generate once the user has a name and at least a summary or some experience. */
    val canGenerate: Boolean
        get() = _form.value.name.isNotBlank() &&
            (_form.value.summary.isNotBlank() || _form.value.experience.isNotBlank())

    private fun fieldsOf(f: BuilderForm): Map<String, String> = buildMap {
        put("name", f.name); put("email", f.email); put("phone", f.phone)
        put("location", f.location); put("summary", f.summary)
        put("experience", f.experience); put("education", f.education)
        put("skills", f.skills)
        if (f.noExperienceYet) put("no_experience_yet", "true")
    }

    /**
     * AI-write the summary from what the user has entered. Honesty contract: rephrase/structure
     * only — on a non-offline failure we leave the user's text untouched. Never blocks the form.
     */
    fun aiWriteSummary() {
        if (_aiBusy.value) return
        val f = _form.value
        viewModelScope.launch {
            _aiBusy.value = true
            runCatching { api.generateResume(GenerateRequest(method = "summary", fields = fieldsOf(f))) }
                .getOrNull()?.summary?.takeIf { it.isNotBlank() }
                ?.let { _form.value = _form.value.copy(summary = it) }
            _aiBusy.value = false
        }
    }

    fun generate() {
        val f = _form.value
        viewModelScope.launch {
            _ui.value = BuilderUi.Generating
            _ui.value = try {
                val res = api.generateResume(GenerateRequest(method = "form", fields = fieldsOf(f)))
                // Persist the built resume to the library so it shows up in Edit (source = built).
                runCatching {
                    val content = json.encodeToString(BuilderForm.serializer(), f)
                    val title = f.name.ifBlank { "My resume" }
                    val id = editingId
                    if (id != null) resumes.updateBuilt(id, title, content)
                    else editingId = resumes.saveBuilt(title, content).id
                }
                // Generating a resume counts as activation (a core action), like upload/optimize.
                analytics.coreActionDone(app.ascend.analytics.CoreAction.UPLOAD)
                BuilderUi.Done(res)
            } catch (t: Throwable) {
                // Record only operation metadata — never resume content (rule 8). Skip offline (expected).
                if (!t.isOffline()) analytics.recordError(t, mapOf("op" to "resume_generate", "method" to "form"))
                BuilderUi.Error(if (t.isOffline()) R.string.error_offline else R.string.error_optimize_failed)
            }
        }
    }

    fun backToEditing() { _ui.value = BuilderUi.Editing }
}
