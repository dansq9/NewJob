package app.ascend.ui.screens.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.data.remote.platform.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MockUi {
    data class Setup(val role: String = "Senior Product Manager", val count: Int = 5) : MockUi
    data object Loading : MockUi
    data class Live(
        val sessionId: String,
        val questions: List<MockQuestion>,
        val index: Int = 0,
        val answers: Map<String, String> = emptyMap(),
    ) : MockUi {
        val current get() = questions[index]
        val progress get() = (index + 1f) / questions.size
    }
    data class Report(val data: MockScoreResponse) : MockUi
    data class Error(val message: String) : MockUi
}

@HiltViewModel
class MockViewModel @Inject constructor(
    private val api: AscendApi,
    private val ads: app.ascend.monetization.ads.AdsManager,
) : ViewModel() {

    private val _ui = MutableStateFlow<MockUi>(MockUi.Setup())
    val ui: StateFlow<MockUi> = _ui.asStateFlow()

    fun setRole(r: String) = _ui.update { (it as? MockUi.Setup)?.copy(role = r) ?: it }
    fun setCount(c: Int) = _ui.update { (it as? MockUi.Setup)?.copy(count = c) ?: it }

    fun start() {
        val setup = _ui.value as? MockUi.Setup ?: return
        viewModelScope.launch {
            // Free users watch a rewarded ad to start a mock interview (Pro bypasses).
            if (!ads.showRewarded(app.ascend.monetization.ads.RewardedFeature.MOCK_INTERVIEW)) return@launch
            _ui.value = MockUi.Loading
            _ui.value = try {
                val r = api.startMock(MockStartRequest(role = setup.role, count = setup.count))
                MockUi.Live(r.sessionId, r.questions)
            } catch (t: Throwable) {
                MockUi.Error(t.message ?: "Couldn't start the interview. Check the Ascend API configuration.")
            }
        }
    }

    fun answer(text: String) = _ui.update { s ->
        (s as? MockUi.Live)?.let { it.copy(answers = it.answers + (it.current.id to text)) } ?: s
    }

    fun next() = _ui.update { s ->
        (s as? MockUi.Live)?.let { if (it.index < it.questions.lastIndex) it.copy(index = it.index + 1) else it } ?: s
    }

    fun finish() {
        val live = _ui.value as? MockUi.Live ?: return
        _ui.value = MockUi.Loading
        viewModelScope.launch {
            _ui.value = try {
                val answers = live.answers.map { MockAnswer(it.key, it.value) }
                MockUi.Report(api.scoreMock(MockScoreRequest(live.sessionId, answers)))
            } catch (t: Throwable) {
                MockUi.Error(t.message ?: "Couldn't score the session.")
            }
        }
    }

    fun reset() { _ui.value = MockUi.Setup() }
}
