package app.ascend.ui.screens.resume

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.data.remote.platform.OptimizeResponse
import app.ascend.data.repo.ResumeRecord
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import app.ascend.ui.util.rememberResumePicker

@Composable
fun ResumeScreen(nav: NavController, vm: ResumeViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lib by vm.library.collectAsStateWithLifecycle()
    val snackbar by vm.snackbar.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val pickResume = rememberResumePicker { vm.addResume(it) }

    LaunchedEffect(snackbar) {
        snackbar?.let { snackbarHost.showSnackbar(it); vm.clearSnackbar() }
    }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar("Resume Optimizer", onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = AscendColors.Ink, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("TAILOR FOR A JOB", fontFamily = JetBrainsMono, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = AscendColors.Violet2)
                    Spacer(Modifier.height(8.dp))
                    Text("Beat the ATS for the exact role you want", color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Ascend's AI rewrites your resume to match this role's keywords and scoring.",
                        fontSize = 13.5.sp, color = Color(0xFFB8B8C4), lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(22.dp))

            // ---- Resume library ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Your resumes")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = pickResume) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp), tint = AscendColors.Indigo)
                    Spacer(Modifier.width(6.dp))
                    Text("Add", color = AscendColors.Indigo, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (lib.resumes.isEmpty()) {
                EmptyLibrary(onAdd = pickResume)
            } else {
                lib.resumes.forEach { r ->
                    ResumeRow(
                        record = r,
                        selected = r.id == lib.selectedId,
                        onSelect = { vm.select(r.id) },
                        onRemove = { vm.remove(r.id) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))

            SectionLabel(if (vm.targetTitle != null) "Target role" else "Optimization mode")
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(vm.targetTitle ?: "General ATS optimization", fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                    if (vm.targetTitle == null) {
                        Text("Open a job first to tailor for that specific role.", fontSize = 12.5.sp, color = AscendColors.Muted2)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))

            when (val s = ui) {
                ResumeUi.Idle -> PrimaryButton(
                    label = if (lib.resumes.isEmpty()) "Add a resume to start" else "Analyze & optimize",
                    icon = Icons.Outlined.AutoFixHigh,
                    enabled = lib.resumes.isNotEmpty(),
                    onClick = vm::optimize,
                )
                ResumeUi.Loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) }
                is ResumeUi.Result -> Results(s.data, lib.selected?.name)
                is ResumeUi.Error -> ApiError(s.message, onRetry = vm::optimize, onDismiss = vm::reset)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResumeRow(record: ResumeRecord, selected: Boolean, onSelect: () -> Unit, onRemove: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) AscendColors.Indigo.copy(alpha = 0.06f) else AscendColors.Card,
        border = BorderStroke(1.5.dp, if (selected) AscendColors.Indigo else AscendColors.Line),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected" else "Select",
                tint = if (selected) AscendColors.Indigo else AscendColors.Muted2,
            )
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Outlined.Description, null, tint = AscendColors.Muted)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(buildString {
                    append(formatSize(record.sizeBytes))
                    record.atsScore?.let { append(" · ATS $it") }
                }, fontSize = 12.sp, color = AscendColors.Muted2)
            }
            TextButton(onClick = onRemove) { Text("Remove", color = AscendColors.Muted2, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun EmptyLibrary(onAdd: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onAdd),
        shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.UploadFile, null, tint = AscendColors.Indigo, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("Upload your resume", fontWeight = FontWeight.Bold, color = AscendColors.Ink)
            Text("PDF, DOC, or DOCX · up to 10MB", fontSize = 12.sp, color = AscendColors.Muted2)
        }
    }
}

@Composable
private fun Results(data: OptimizeResponse, resumeName: String?) {
    val uriHandler = LocalUriHandler.current
    val ctx = LocalContext.current
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

    // Download / share the optimized resume.
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = {
                data.downloadUrl?.let { url ->
                    if (runCatching { uriHandler.openUri(url) }.isFailure) {
                        android.widget.Toast.makeText(ctx, "Couldn't open the download link.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = data.downloadUrl != null,
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Download")
        }
        OutlinedButton(
            onClick = {
                val summary = buildShareText(data, resumeName)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, summary)
                    putExtra(Intent.EXTRA_SUBJECT, "Optimized resume")
                }
                runCatching { ctx.startActivity(Intent.createChooser(send, "Share resume")) }
            },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Share")
        }
    }
}

private fun buildShareText(data: OptimizeResponse, resumeName: String?): String = buildString {
    appendLine("My resume${resumeName?.let { " ($it)" } ?: ""} scored ${data.atsScore}/100 on Ascend's ATS check.")
    data.optimizedScore?.let { appendLine("Optimized score: $it/100.") }
    data.downloadUrl?.let { appendLine(it) }
    append("— optimized with Ascend")
}

private fun formatSize(bytes: Long?): String = when {
    bytes == null -> "Document"
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

@Composable
private fun PrimaryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
        Icon(icon, null); Spacer(Modifier.width(8.dp)); Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun ApiError(message: String, onRetry: (() -> Unit)? = null, onDismiss: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.AmberBg, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Something went wrong", fontWeight = FontWeight.Bold, color = AscendColors.Amber)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = AscendColors.Amber)
            Spacer(Modifier.height(12.dp))
            Row {
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
                    ) { Text("Try again", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("Dismiss", color = AscendColors.Indigo) }
            }
        }
    }
}
