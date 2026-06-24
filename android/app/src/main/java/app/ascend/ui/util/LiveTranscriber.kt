package app.ascend.ui.util

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Controller for live, continuous speech-to-text (Live Copilot). [available] is
 * false when the device has no recognition service — callers should hide the
 * live-transcription UI and fall back to manual entry in that case.
 */
class LiveTranscriberController internal constructor(
    val available: Boolean,
    private val listeningState: MutableState<Boolean>,
    private val onToggle: () -> Unit,
) {
    /** True while actively transcribing. */
    val listening: Boolean get() = listeningState.value
    /** Start (requesting mic permission if needed) or stop transcription. */
    fun toggle() = onToggle()
}

/**
 * Continuous transcription via the native [SpeechRecognizer]. Partial results
 * stream to [onPartial]; finalized utterances to [onFinal]. Handles the
 * RECORD_AUDIO permission request, recognizer-unavailable, and auto-restarts
 * after each utterance/timeout so it keeps listening through a call.
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

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { active.value = true; runCatching { recognizer?.startListening(listenIntent()) } }
    }

    val toggle: () -> Unit = {
        when {
            !available || recognizer == null -> Unit
            active.value -> { active.value = false; listening.value = false; runCatching { recognizer?.stopListening() } }
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                active.value = true; runCatching { recognizer?.startListening(listenIntent()) }
            }
            else -> runCatching { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        }
    }

    return remember(available) { LiveTranscriberController(available, listening, toggle) }
}
