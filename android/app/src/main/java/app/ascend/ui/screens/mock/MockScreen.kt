package app.ascend.ui.screens.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.components.Pill
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import app.ascend.ui.screens.resume.ApiError
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import app.ascend.ui.util.rememberVoiceInput

@Composable
fun MockScreen(nav: NavController, vm: MockViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar("Mock Interview", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            when (val s = ui) {
                is MockUi.Setup -> Setup(s, vm)
                MockUi.Loading -> Box(Modifier.fillMaxWidth().padding(60.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) }
                is MockUi.Live -> Live(s, vm)
                is MockUi.Report -> Report(s, vm)
                is MockUi.Error -> ApiError(s.message, onRetry = vm::retry, onDismiss = vm::reset)
            }
        }
    }
}

@Composable
private fun Setup(s: MockUi.Setup, vm: MockViewModel) {
    Text("Practice with AI-generated questions and get instant feedback.", color = AscendColors.Muted, fontSize = 14.sp)
    Spacer(Modifier.height(22.dp))
    Text("Role to prep for", fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = s.role, onValueChange = vm::setRole, singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card),
    )
    Spacer(Modifier.height(22.dp))
    Text("Number of questions", fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(3, 5, 8, 10).forEach { n ->
            val sel = s.count == n
            Surface(onClick = { vm.setCount(n) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp), color = if (sel) AscendColors.ChipIndigo else AscendColors.Card,
                border = BorderStroke(1.5.dp, if (sel) AscendColors.Indigo else AscendColors.Line)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$n", fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                        color = if (sel) AscendColors.Indigo else AscendColors.Muted)
                }
            }
        }
    }
    Spacer(Modifier.height(28.dp))
    Button(onClick = vm::start, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
        Text("Start Mock Interview", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
private fun Live(s: MockUi.Live, vm: MockViewModel) {
    var text by remember(s.index) { mutableStateOf(s.answers[s.current.id].orEmpty()) }
    val startVoice = rememberVoiceInput { spoken ->
        text = if (text.isBlank()) spoken else "$text $spoken"
        vm.answer(text)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Question ${s.index + 1} of ${s.questions.size}", fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold, color = AscendColors.Muted2, fontSize = 13.sp)
    }
    Spacer(Modifier.height(10.dp))
    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth(),
        color = AscendColors.Indigo, trackColor = AscendColors.Line)
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Pill(s.current.tag, AscendColors.Indigo, AscendColors.ChipIndigo)
        Pill(s.current.difficulty, AscendColors.Amber, AscendColors.AmberBg)
    }
    Spacer(Modifier.height(14.dp))
    Text(s.current.prompt, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, lineHeight = 27.sp)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, enabled = false, leadingIcon = { Icon(Icons.Outlined.Keyboard, null, Modifier.size(18.dp)) }, label = { Text("Type") })
        AssistChip(onClick = startVoice, leadingIcon = { Icon(Icons.Outlined.Mic, null, Modifier.size(18.dp)) }, label = { Text("Speak") })
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = text, onValueChange = { text = it; vm.answer(it) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
        placeholder = { Text("Type your answer here…") }, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,   // multiline: Enter inserts newlines
        ),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card),
    )
    Spacer(Modifier.height(16.dp))
    val last = s.index == s.questions.lastIndex
    Button(onClick = { if (last) vm.finish() else vm.next() }, modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
        Text(if (last) "Finish & score" else "Next question", fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Report(s: MockUi.Report, vm: MockViewModel) {
    Text("Session report", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
    Spacer(Modifier.height(14.dp))
    Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.ChipIndigo, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${s.data.averageScore}%", fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, color = AscendColors.Indigo)
            Text("Average score", fontSize = 12.sp, color = AscendColors.Muted2)
        }
    }
    Spacer(Modifier.height(14.dp))
    s.data.areas.forEach {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(it.area, Modifier.width(120.dp), fontSize = 13.sp, color = AscendColors.Ink2)
            LinearProgressIndicator(progress = { it.score / 100f }, modifier = Modifier.weight(1f),
                color = AscendColors.Indigo, trackColor = AscendColors.Line)
            Spacer(Modifier.width(10.dp))
            Text("${it.score}", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = AscendColors.Indigo)
        }
    }
    Spacer(Modifier.height(20.dp))
    Button(onClick = vm::reset, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text("Practice again", fontWeight = FontWeight.ExtraBold) }
}
