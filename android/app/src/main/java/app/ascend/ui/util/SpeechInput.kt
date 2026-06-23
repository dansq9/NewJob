package app.ascend.ui.util

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Locale

/**
 * Voice-to-text via the OS speech recognizer (RecognizerIntent — on-device /
 * Google recognition). Returns a lambda; invoke it to start dictation. The
 * recognised text is delivered to [onResult]. Used by the Mock interview
 * "Speak" mode; the Live Copilot uses continuous SpeechRecognizer to transcribe
 * the interviewer (see CopilotViewModel).
 */
@Composable
fun rememberVoiceInput(onResult: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(onResult)
        }
    }
    return remember(launcher) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Answer out loud…")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            runCatching { launcher.launch(intent) }
        }
    }
}
