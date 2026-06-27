package app.ascend.ui.screens.resume

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.ui.components.AscendIconBadge
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import kotlinx.coroutines.launch

/**
 * Build-by-voice: on-device speech capture via the system recognizer (handles the mic permission
 * and listening UI itself). The transcript seeds the guided form. Always offers "Type instead",
 * and falls back to the form if no recognizer is available — the user is never stranded.
 */
@Composable
fun ResumeVoiceScreen(nav: NavController, vm: ResumeVoiceViewModel = hiltViewModel()) {
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.resume_voice_unavailable)
    val prompt = stringResource(R.string.resume_voice_prompt)

    fun goForm() = nav.navigate(Routes.RESUME_BUILD_FORM) {
        popUpTo(Routes.RESUME_BUILD_VOICE) { inclusive = true }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) vm.stash(text)
        }
        goForm()
    }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_voice_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            AscendIconBadge(Icons.Outlined.Mic, size = 72, radius = 22, tint = AscendColors.Indigo)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.resume_voice_heading), fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.resume_voice_sub), fontSize = 13.5.sp,
                color = AscendColors.Muted, textAlign = TextAlign.Center, lineHeight = 19.sp)
            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                    }
                    // No recognizer present (e.g. no Google app) → fail open to typing.
                    if (runCatching { launcher.launch(intent) }.isFailure) {
                        scope.launch { snackbarHost.showSnackbar(unavailable) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) {
                Icon(Icons.Outlined.Mic, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.resume_voice_start), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { goForm() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Keyboard, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.resume_voice_type_instead))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
