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

/** One work-experience entry — the builder supports a repeating list of these. */
@Serializable
data class ExperienceEntry(
    val role: String = "",
    val company: String = "",
    val dates: String = "",
    val detail: String = "",
) {
    val isBlank: Boolean get() = role.isBlank() && company.isBlank() && dates.isBlank() && detail.isBlank()
    val heading: String get() = listOf(role, company).filter { it.isNotBlank() }.joinToString(" · ")
}

/** What the guided form collects. */
@Serializable
data class BuilderForm(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val summary: String = "",
    val experiences: List<ExperienceEntry> = listOf(ExperienceEntry()),
    val education: String = "",
    val skills: String = "",
    /** Lets a no-experience user lead with Education/Projects/Skills instead of work history. */
    val noExperienceYet: Boolean = false,
)

/** The wizard steps, in order. REVIEW is the live-preview + generate step. */
enum class BuildStep { CONTACT, SUMMARY, EXPERIENCE, EDUCATION, SKILLS, REVIEW }

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

    /** Current wizard step index into [BuildStep.entries]. */
    private val _step = MutableStateFlow(0)
    val step = _step.asStateFlow()
    val steps: List<BuildStep> = BuildStep.entries

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy = _aiBusy.asStateFlow()

    private var editingId: String? = null
    private var loaded = false

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
            draftStore.consumeTranscript()?.takeIf { it.isNotBlank() }?.let { t ->
                _form.value = _form.value.copy(experiences = listOf(ExperienceEntry(detail = t)))
            }
        }
    }

    fun update(transform: (BuilderForm) -> BuilderForm) { _form.value = transform(_form.value) }

    // ---- Step navigation ----
    fun nextStep() { if (_step.value < steps.lastIndex) _step.value += 1 }
    fun prevStep() { if (_step.value > 0) _step.value -= 1 }
    fun goToStep(index: Int) { _step.value = index.coerceIn(0, steps.lastIndex) }
    val isLastStep: Boolean get() = _step.value == steps.lastIndex

    // ---- Repeating experience entries ----
    fun addExperience() { _form.value = _form.value.copy(experiences = _form.value.experiences + ExperienceEntry()) }
    fun removeExperience(index: Int) {
        val list = _form.value.experiences.toMutableList()
        if (index in list.indices) list.removeAt(index)
        _form.value = _form.value.copy(experiences = list.ifEmpty { listOf(ExperienceEntry()) })
    }
    fun updateExperience(index: Int, transform: (ExperienceEntry) -> ExperienceEntry) {
        val list = _form.value.experiences.toMutableList()
        if (index in list.indices) { list[index] = transform(list[index]); _form.value = _form.value.copy(experiences = list) }
    }

    /** A friendly, fixable "resume strength" 0–100 derived from completeness. */
    val strength: Int
        get() {
            val f = _form.value
            var score = 0
            if (f.name.isNotBlank()) score += 12
            if (f.email.isNotBlank()) score += 10
            if (f.location.isNotBlank()) score += 8
            if (f.summary.length >= 40) score += 25 else if (f.summary.isNotBlank()) score += 10
            val filledExp = f.experiences.count { !it.isBlank }
            if (filledExp > 0) score += 20 + (filledExp - 1).coerceAtMost(2) * 4
            if (f.education.isNotBlank()) score += 12
            if (f.skills.isNotBlank()) score += 13
            return score.coerceIn(0, 100)
        }

    val canGenerate: Boolean
        get() = _form.value.name.isNotBlank() &&
            (_form.value.summary.isNotBlank() || _form.value.experiences.any { !it.isBlank })

    private fun experienceText(f: BuilderForm): String =
        f.experiences.filterNot { it.isBlank }.joinToString("\n") { e ->
            listOfNotNull(
                e.heading.ifBlank { null },
                e.dates.ifBlank { null },
                e.detail.ifBlank { null },
            ).joinToString(" · ")
        }

    private fun fieldsOf(f: BuilderForm): Map<String, String> = buildMap {
        put("name", f.name); put("email", f.email); put("phone", f.phone)
        put("location", f.location); put("summary", f.summary)
        put("experience", experienceText(f)); put("education", f.education)
        put("skills", f.skills)
        if (f.noExperienceYet) put("no_experience_yet", "true")
    }

    /** AI-write the summary from what the user entered (rephrase only; never fabricates). */
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
                runCatching {
                    val content = json.encodeToString(BuilderForm.serializer(), f)
                    val title = f.name.ifBlank { "My resume" }
                    val id = editingId
                    if (id != null) resumes.updateBuilt(id, title, content)
                    else editingId = resumes.saveBuilt(title, content).id
                }
                analytics.coreActionDone(app.ascend.analytics.CoreAction.UPLOAD)
                BuilderUi.Done(res)
            } catch (t: Throwable) {
                if (!t.isOffline()) analytics.recordError(t, mapOf("op" to "resume_generate", "method" to "form"))
                BuilderUi.Error(if (t.isOffline()) R.string.error_offline else R.string.error_optimize_failed)
            }
        }
    }

    fun backToEditing() { _ui.value = BuilderUi.Editing }
}
