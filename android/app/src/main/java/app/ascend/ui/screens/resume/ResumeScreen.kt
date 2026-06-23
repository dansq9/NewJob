package app.ascend.ui.screens.resume

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.data.remote.platform.OptimizeResponse
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono

@Composable
fun ResumeScreen(nav: NavController, vm: ResumeViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar("Resume Optimizer", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = AscendColors.Ink, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("TAILOR FOR A JOB", fontFamily = JetBrainsMono, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = AscendColors.Violet2)
                    Spacer(Modifier.height(8.dp))
                    Text("Beat the ATS for the exact role you want", color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Ascend's AI rewrites your resume to match this role's keywords and scoring.",
                        fontSize = 13.5.sp, color = androidx.compose.ui.graphics.Color(0xFFB8B8C4), lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            SectionLabel("Target role")
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
                Text(vm.targetTitle, Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
            }
            Spacer(Modifier.height(22.dp))

            when (val s = ui) {
                ResumeUi.Idle -> PrimaryButton("Analyze & optimize", Icons.Outlined.AutoFixHigh, vm::optimize)
                ResumeUi.Loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) }
                is ResumeUi.Result -> Results(s.data)
                is ResumeUi.Error -> ApiError(s.message, vm::reset)
            }
        }
    }
}

@Composable
private fun Results(data: OptimizeResponse) {
    Surface(shape = RoundedCornerShape(20.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { data.atsScore / 100f }, modifier = Modifier.size(72.dp),
                    color = AscendColors.Green, trackColor = AscendColors.Line, strokeWidth = 8.dp)
                Text("${data.atsScore}", fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AscendColors.Ink)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(data.verdict, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                Text(data.verdictDetail, fontSize = 13.sp, color = AscendColors.Muted, lineHeight = 18.sp)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    data.issues.forEach { issue ->
        Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(16.dp),
            color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line)) {
            Row(Modifier.padding(14.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = if (issue.resolved) AscendColors.Green else AscendColors.Amber)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(issue.title, fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.5.sp)
                    Text(issue.detail, fontSize = 12.5.sp, color = AscendColors.Muted, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
        Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun ApiError(message: String, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.AmberBg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Couldn't reach the Ascend API", fontWeight = FontWeight.Bold, color = AscendColors.Amber)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = AscendColors.Amber)
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss) { Text("Dismiss", color = AscendColors.Indigo) }
        }
    }
}
