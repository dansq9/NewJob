package app.ascend.ui.screens.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.analytics.Analytics
import app.ascend.core.isOffline
import app.ascend.core.userMessage
import app.ascend.data.remote.platform.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    /** [phase] tells the screen which op to re-run on retry. */
    data class Error(val message: String, val phase: Phase) : MockUi

    enum class Phase { START, SCORE }
}

@HiltViewModel
class MockViewModel @Inject constructor(
    private val api: AscendApi,
    private val ads: app.ascend.monetization.ads.AdsManager,
    private val analytics: Analytics,
    private val profile: app.ascend.data.local.ProfileRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow<MockUi>(MockUi.Setup())
    val ui: StateFlow<MockUi> = _ui.asStateFlow()

    // Snapshots so a failed op can be retried from the Error state.
    private var lastSetup = MockUi.Setup()
    private var lastLive: MockUi.Live? = null

    init {
        // Default the role to the user's target role (Setup only — don't disturb an active session).
        viewModelScope.launch {
            val role = profile.profile.first().targetRole
            if (role.isNotBlank()) {
                _ui.update { (it as? MockUi.Setup)?.copy(role = role) ?: it }
                lastSetup = (_ui.value as? MockUi.Setup) ?: lastSetup
            }
        }
    }

    fun setRole(r: String) = _ui.update { (it as? MockUi.Setup)?.copy(role = r) ?: it }
    fun setCount(c: Int) = _ui.update { (it as? MockUi.Setup)?.copy(count = c) ?: it }

    fun start() {
        val setup = _ui.value as? MockUi.Setup ?: lastSetup
        lastSetup = setup
        viewModelScope.launch {
            // Free users watch a rewarded ad to start a mock interview (Pro bypasses).
            if (!ads.showRewarded(app.ascend.monetization.ads.RewardedFeature.MOCK_INTERVIEW)) return@launch
            _ui.value = MockUi.Loading
            _ui.value = try {
                val r = api.startMock(MockStartRequest(role = setup.role, count = setup.count))
                MockUi.Live(r.sessionId, r.questions)
            } catch (t: Throwable) {
                if (!t.isOffline()) analytics.recordError(t, mapOf("op" to "mock_start"))
                MockUi.Error(t.userMessage("Couldn't start the interview. Check the Ascend API configuration."), MockUi.Phase.START)
            }
        }
    }

    /** Re-run the operation that failed. */
    fun retry() {
        when ((_ui.value as? MockUi.Error)?.phase) {
            MockUi.Phase.START -> start()
            MockUi.Phase.SCORE -> lastLive?.let { score(it) }
            null -> Unit
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
        lastLive = live
        score(live)
    }

    private fun score(live: MockUi.Live) {
        _ui.value = MockUi.Loading
        viewModelScope.launch {
            _ui.value = try {
                val answers = live.answers.map { MockAnswer(it.key, it.value) }
                MockUi.Report(api.scoreMock(MockScoreRequest(live.sessionId, answers)))
            } catch (t: Throwable) {
                // Metadata only — never the answer text.
                if (!t.isOffline()) analytics.recordError(t, mapOf("op" to "mock_score", "answers" to live.answers.size))
                MockUi.Error(t.userMessage("Couldn't score the session."), MockUi.Phase.SCORE)
            }
        }
    }

    fun reset() { _ui.value = MockUi.Setup() }
}
