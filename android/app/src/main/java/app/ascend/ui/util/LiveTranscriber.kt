package app.ascend.ui.util

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Controller for live, continuous speech-to-text (Live Copilot). [available] is
 * false when the device has no recognition service — callers should hide the
 * live-transcription UI and fall back to manual entry in that case. The caller
 * owns the RECORD_AUDIO permission flow and must only call [start] once granted.
 */
class LiveTranscriberController internal constructor(
    val available: Boolean,
    private val listeningState: MutableState<Boolean>,
    private val onStart: () -> Unit,
    private val onStop: () -> Unit,
) {
    /** True while actively transcribing. */
    val listening: Boolean get() = listeningState.value
    /** Begin transcription. Caller must already hold RECORD_AUDIO permission. */
    fun start() = onStart()
    /** Stop transcription. */
    fun stop() = onStop()
}

/**
 * Continuous transcription via the native [SpeechRecognizer]. Partial results
 * stream to [onPartial]; finalized utterances to [onFinal]. Auto-restarts after
 * each utterance/timeout so it keeps listening through a call. Recognizer-
 * unavailable is reported via [LiveTranscriberController.available]; the mic
 * permission flow is owned by the caller (so it can show a rationale first).
 */
@Composable
fun rememberLiveTranscriber(
    onPartial: (String) -> Unit,
    onFinal: (String) -> Unit,
): LiveTranscriberController {
    val context = LocalContext.current
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val listening = remember { mutableStateOf(false) }
    // active = the user's intent to keep listening (drives auto-restart).
    val active = remember { mutableStateOf(false) }
    val partial = rememberUpdatedState(onPartial)
    val final = rememberUpdatedState(onFinal)

    val recognizer = remember { if (available) SpeechRecognizer.createSpeechRecognizer(context) else null }

    fun listenIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onPartialResults(partialResults: Bundle) {
                partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { partial.value(it) }
            }

            override fun onResults(results: Bundle) {
                results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { final.value(it) }
                // Continuous: immediately listen for the next utterance.
                if (active.value) recognizer?.startListening(listenIntent()) else listening.value = false
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> { active.value = false; listening.value = false }
                    // Silence/no-match/busy are normal during a pause — keep going.
                    else -> if (active.value) recognizer?.startListening(listenIntent()) else listening.value = false
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            active.value = false
            listening.value = false
            runCatching { recognizer?.destroy() }
        }
    }

    val start: () -> Unit = {
        if (available && recognizer != null && !active.value) {
            active.value = true
            runCatching { recognizer.startListening(listenIntent()) }
        }
    }
    val stop: () -> Unit = {
        active.value = false
        listening.value = false
        runCatching { recognizer?.stopListening() }
    }

    return remember(available) { LiveTranscriberController(available, listening, start, stop) }
}
