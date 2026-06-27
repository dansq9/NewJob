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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the guided form collects. Kept as plain fields in v1 (repeating roles come later). */
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
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    private val _form = MutableStateFlow(BuilderForm())
    val form = _form.asStateFlow()

    private val _ui = MutableStateFlow<BuilderUi>(BuilderUi.Editing)
    val ui = _ui.asStateFlow()

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

    fun generate() {
        val f = _form.value
        viewModelScope.launch {
            _ui.value = BuilderUi.Generating
            _ui.value = try {
                val res = api.generateResume(
                    GenerateRequest(
                        method = "form",
                        fields = buildMap {
                            put("name", f.name); put("email", f.email); put("phone", f.phone)
                            put("location", f.location); put("summary", f.summary)
                            put("experience", f.experience); put("education", f.education)
                            put("skills", f.skills)
                            if (f.noExperienceYet) put("no_experience_yet", "true")
                        },
                    )
                )
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
