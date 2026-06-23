package app.ascend.ui.screens.copilot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.remote.platform.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CopilotViewModel @Inject constructor(
    private val api: AscendApi,
    entitlements: app.ascend.data.billing.EntitlementRepository,
) : ViewModel() {

    // Interview Navigator is a Pro feature.
    val isPro: kotlinx.coroutines.flow.StateFlow<Boolean> =
        entitlements.isPro.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    data class State(
        val live: Boolean = false,
        val role: String = "Senior Product Manager",
        val company: String = "Northwind",
        val question: String = "",
        val loading: Boolean = false,
        val answer: CopilotAnswerResponse? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setRole(r: String) = _state.update { it.copy(role = r) }
    fun setCompany(c: String) = _state.update { it.copy(company = c) }
    fun setQuestion(q: String) = _state.update { it.copy(question = q) }
    fun launch() = _state.update { it.copy(live = true) }
    fun end() = _state.value.let { _state.value = State() }

    // Live transcription: wire android.speech.SpeechRecognizer in continuous mode
    // (RECOGNIZER_INTENT free-form, partial results) to feed detected questions
    // here automatically. Manual entry is supported as a fallback / for testing.
    fun ask() {
        val s = _state.value
        if (s.question.isBlank()) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val ctx = CopilotContext(role = s.role, company = s.company)
                val ans = api.copilotAnswer(CopilotAnswerRequest(ctx, s.question))
                _state.update { it.copy(loading = false, answer = ans) }
            } catch (t: Throwable) {
                _state.update { it.copy(loading = false, error = t.message ?: "Couldn't reach the Copilot API.") }
            }
        }
    }
}
