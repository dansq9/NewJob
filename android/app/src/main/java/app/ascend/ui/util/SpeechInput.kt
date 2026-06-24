package app.ascend.ui.util

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(onResult)
        }
        // RESULT_CANCELED (user backed out / denied mic / no speech) is a no-op: keep the typed answer.
    }
    return remember(launcher, context) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Answer out loud…")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            // No speech recognizer (e.g. some devices without Google app) → guide the user to type.
            if (launcher.runCatching { launch(intent) }.isFailure) {
                Toast.makeText(context, "Voice input isn't available — you can type your answer instead.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
