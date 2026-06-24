package app.ascend.ui.screens.copilot

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MicOff
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.annotation.StringRes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import app.ascend.ui.util.rememberLiveTranscriber

private val DarkCard = Color(0xFF16161F)
private val DarkLine = Color(0xFF23232F)
private val Lilac = Color(0xFFA89BFF)

@Composable
fun CopilotScreen(nav: NavController, vm: CopilotViewModel = hiltViewModel()) {
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    if (!isPro) {
        ProLock(onUpgrade = { nav.navigate(app.ascend.ui.navigation.Routes.PAYWALL) }, onBack = { nav.popBackStack() })
        return
    }
    val s by vm.state.collectAsStateWithLifecycle()
    if (!s.live) SetupView(vm, nav) else LiveView(vm, nav)
}

@Composable
private fun ProLock(onUpgrade: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { app.ascend.ui.components.AscendTopBar(stringResource(R.string.copilot_title), onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(28.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(AscendColors.ChipIndigo), Alignment.Center) {
                Icon(Icons.Outlined.Bolt, null, tint = AscendColors.Indigo, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.copilot_prolock_title), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.copilot_prolock_desc),
                fontSize = 14.sp, color = AscendColors.Muted, lineHeight = 21.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onUpgrade, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) { Text(stringResource(R.string.copilot_unlock_pro), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun SetupView(vm: CopilotViewModel, nav: NavController) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { app.ascend.ui.components.AscendTopBar(stringResource(R.string.copilot_title), onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = AscendColors.Ink, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.copilot_setup_eyebrow), fontFamily = JetBrainsMono, color = Lilac, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.copilot_setup_intro),
                        color = Color(0xFFD8D8E2), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Label(stringResource(R.string.copilot_job_title))
            Field(s.role, vm::setRole)
            Spacer(Modifier.height(16.dp))
            Label(stringResource(R.string.copilot_company_optional))
            Field(s.company, vm::setCompany)
            Spacer(Modifier.height(28.dp))
            Button(onClick = vm::launch, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Icon(Icons.Outlined.GraphicEq, null); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.copilot_launch), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.copilot_split_screen_hint), fontSize = 11.5.sp,
                color = AscendColors.Muted2, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun LiveView(vm: CopilotViewModel, nav: NavController) {
    val s by vm.state.collectAsStateWithLifecycle()
    // Live transcription via the native speech recognizer; partial + final text fill the question.
    val transcriber = rememberLiveTranscriber(
        onPartial = { vm.setQuestion(it) },
        onFinal = { vm.setQuestion(it) },
    )
    val status = when {
        !transcriber.available -> stringResource(R.string.copilot_status_manual)
        transcriber.listening -> stringResource(R.string.copilot_status_listening)
        else -> stringResource(R.string.copilot_status_tap_mic)
    }
    val statusColor = if (transcriber.listening) Color(0xFF34D17F) else Color(0xFF8A8A99)

    // Mic permission flow (owned here so we can show a rationale before the system prompt).
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var showRationale by remember { mutableStateOf(false) }
    var showDenied by remember { mutableStateOf(false) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when {
            granted -> transcriber.start()
            // granted=false + no rationale after asking ⇒ "don't ask again" / permanently denied.
            activity == null || !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.RECORD_AUDIO) -> showDenied = true
        }
    }
    val onMicTap = onMicTap@{
        when {
            transcriber.listening -> { transcriber.stop(); return@onMicTap }
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED -> transcriber.start()
            else -> showRationale = true
        }
    }

    // Stop the mic when the app is backgrounded (don't hold the recorder while away).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) transcriber.stop()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxSize().background(AscendColors.Dark).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.end(); nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.copilot_back), tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.copilot_role_at_company, s.role, s.company), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text(status, color = statusColor, fontSize = 11.sp)
            }
            TextButton(onClick = { vm.end(); nav.popBackStack() }) { Text(stringResource(R.string.copilot_end), color = Lilac, fontWeight = FontWeight.Bold) }
        }
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            OutlinedTextField(
                value = s.question, onValueChange = vm::setQuestion, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.copilot_question_placeholder), color = Color(0xFF8A8A99)) },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (s.question.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuestion("") }) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.copilot_clear_question), tint = Color(0xFF8A8A99))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { vm.ask() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCard, unfocusedContainerColor = DarkCard,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = AscendColors.Indigo, unfocusedBorderColor = DarkLine,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (transcriber.available) {
                    OutlinedButton(
                        onClick = onMicTap,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (transcriber.listening) Color(0xFF34D17F) else DarkLine),
                    ) {
                        Icon(if (transcriber.listening) Icons.Outlined.MicOff else Icons.Outlined.Mic, null,
                            tint = if (transcriber.listening) Color(0xFF34D17F) else Lilac)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (transcriber.listening) R.string.copilot_stop else R.string.copilot_listen), color = Color.White)
                    }
                }
                Button(onClick = vm::ask, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                    Icon(Icons.Outlined.Bolt, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.copilot_draft_answer), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            when {
                s.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Lilac) }
                s.error != null -> CopilotError(s.error!!, onRetry = vm::ask)
                s.answer != null -> AnswerCard(s.answer!!)
                else -> Text(stringResource(R.string.copilot_tip), color = Color(0xFF8A8A99), fontSize = 12.5.sp)
            }
        }
    }

    if (showRationale) AlertDialog(
        onDismissRequest = { showRationale = false },
        title = { Text(stringResource(R.string.copilot_mic_rationale_title)) },
        text = {
            Text(stringResource(R.string.copilot_mic_rationale_body))
        },
        confirmButton = {
            TextButton(onClick = {
                showRationale = false
                micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
            }) { Text(stringResource(R.string.action_continue)) }
        },
        dismissButton = { TextButton(onClick = { showRationale = false }) { Text(stringResource(R.string.copilot_mic_not_now)) } },
    )

    if (showDenied) AlertDialog(
        onDismissRequest = { showDenied = false },
        title = { Text(stringResource(R.string.copilot_mic_denied_title)) },
        text = { Text(stringResource(R.string.copilot_mic_denied_body)) },
        confirmButton = {
            TextButton(onClick = {
                showDenied = false
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }) { Text(stringResource(R.string.copilot_mic_open_settings)) }
        },
        dismissButton = { TextButton(onClick = { showDenied = false }) { Text(stringResource(R.string.copilot_mic_manual_entry)) } },
    )
}

@Composable
private fun AnswerCard(answer: app.ascend.data.remote.platform.CopilotAnswerResponse) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1A1838), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3470)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, null, tint = Lilac, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.copilot_answer_eyebrow), color = Lilac, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            answer.sections.forEach { sec ->
                Text(sec.label.uppercase(), color = Color(0xFF8B78EC), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Text(sec.text, color = Color(0xFFE6E6F0), fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}

@Composable
private fun CopilotError(@StringRes message: Int, onRetry: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF2A1A22), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(message), color = Color(0xFFFFB4B4), fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun Label(t: String) {
    Text(t.uppercase(), fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AscendColors.Muted2)
    Spacer(Modifier.height(8.dp))
}

@Composable private fun Field(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card))
}
