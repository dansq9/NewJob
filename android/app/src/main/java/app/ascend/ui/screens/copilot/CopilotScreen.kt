package app.ascend.ui.screens.copilot

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono

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
        topBar = { app.ascend.ui.components.AscendTopBar("AI Interview Copilot", onBack = onBack) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(28.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(AscendColors.ChipIndigo), Alignment.Center) {
                Icon(Icons.Outlined.Bolt, null, tint = AscendColors.Indigo, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Live Interview Navigator", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                "Get real-time, in-your-voice answers during interviews. The live copilot is an Ascend Pro feature.",
                fontSize = 14.sp, color = AscendColors.Muted, lineHeight = 21.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onUpgrade, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) { Text("Unlock with Pro", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun SetupView(vm: CopilotViewModel, nav: NavController) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { app.ascend.ui.components.AscendTopBar("AI Interview Copilot", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = AscendColors.Ink, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("REAL-TIME COPILOT", fontFamily = JetBrainsMono, color = Lilac, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Set the context once. During your call we transcribe each question and draft an answer in your voice.",
                        color = Color(0xFFD8D8E2), fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Label("Job title")
            Field(s.role, vm::setRole)
            Spacer(Modifier.height(16.dp))
            Label("Company · optional")
            Field(s.company, vm::setCompany)
            Spacer(Modifier.height(28.dp))
            Button(onClick = vm::launch, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Icon(Icons.Outlined.GraphicEq, null); Spacer(Modifier.width(8.dp))
                Text("Launch Copilot", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Keep this open in split-screen during your video call.", fontSize = 11.5.sp,
                color = AscendColors.Muted2, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun LiveView(vm: CopilotViewModel, nav: NavController) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(AscendColors.Dark).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.end(); nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text("${s.role} @ ${s.company}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text("Live · listening", color = Color(0xFF34D17F), fontSize = 11.sp)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            OutlinedTextField(
                value = s.question, onValueChange = vm::setQuestion, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste or type the interviewer's question…", color = Color(0xFF8A8A99)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkCard, unfocusedContainerColor = DarkCard,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = AscendColors.Indigo, unfocusedBorderColor = DarkLine,
                ),
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = vm::ask, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Icon(Icons.Outlined.Bolt, null); Spacer(Modifier.width(6.dp)); Text("Draft answer", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            when {
                s.loading -> Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator(color = Lilac) }
                s.error != null -> Text(s.error!!, color = Color(0xFFFFB4B4), fontSize = 13.sp)
                s.answer != null -> AnswerCard(s.answer!!)
                else -> Text("Tip: keep your phone near the computer speaker.", color = Color(0xFF8A8A99), fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun AnswerCard(answer: app.ascend.data.remote.platform.CopilotAnswerResponse) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF1A1838), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A3470)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bolt, null, tint = Lilac, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("COPILOT ANSWER", color = Lilac, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
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

@Composable private fun Label(t: String) {
    Text(t.uppercase(), fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AscendColors.Muted2)
    Spacer(Modifier.height(8.dp))
}

@Composable private fun Field(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card))
}
