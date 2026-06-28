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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.data.remote.platform.OptimizeResponse
import app.ascend.data.repo.ResumeRecord
import app.ascend.monetization.Placement
import app.ascend.ui.monetization.NativeAdSlot
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import app.ascend.ui.util.rememberResumePicker
import kotlinx.coroutines.launch

@Composable
fun ResumeScreen(nav: NavController, vm: ResumeViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val lib by vm.library.collectAsStateWithLifecycle()
    val target by vm.target.collectAsStateWithLifecycle()
    val trackerOptions by vm.trackerOptions.collectAsStateWithLifecycle()
    val snackbar by vm.snackbar.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val pickResume = rememberResumePicker { vm.addResume(it) }
    var showTargetSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // Suppress the app-open ad while the user is in the resume flow (spec suppress_during_resume_flow).
    app.ascend.ui.monetization.SuppressAppOpenWhileActive(app.ascend.monetization.AdFlow.RESUME)

    LaunchedEffect(snackbar) {
        snackbar?.let { snackbarHost.showSnackbar(it); vm.clearSnackbar() }
    }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            Surface(shape = RoundedCornerShape(22.dp), color = AscendColors.Ink, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text(stringResource(R.string.resume_hero_eyebrow), fontFamily = JetBrainsMono, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = AscendColors.Violet2)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.resume_hero_title), color = Color.White,
                        fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.resume_hero_subtitle),
                        fontSize = 13.5.sp, color = Color(0xFFB8B8C4), lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(22.dp))

            // ---- Resume library ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.resume_library_label))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = pickResume) {
                    Icon(Icons.Outlined.UploadFile, null, Modifier.size(18.dp), tint = AscendColors.Indigo)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.resume_add), color = AscendColors.Indigo, fontWeight = FontWeight.Bold)
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

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(if (!target.isGeneral) stringResource(R.string.resume_target_role) else stringResource(R.string.resume_optimization_mode))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showTargetSheet = true }) {
                    Text(stringResource(R.string.action_change), color = AscendColors.Indigo, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
                border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth(),
                onClick = { showTargetSheet = true },
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (target.isGeneral) stringResource(R.string.resume_general_optimization)
                        else listOfNotNull(target.title, target.company).joinToString(" · "),
                        fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                    )
                    Text(
                        if (target.isGeneral) stringResource(R.string.resume_target_general_hint)
                        else stringResource(R.string.resume_target_set_hint),
                        fontSize = 12.5.sp, color = AscendColors.Muted2,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            when (val s = ui) {
                ResumeUi.Idle -> PrimaryButton(
                    label = if (lib.resumes.isEmpty()) stringResource(R.string.resume_add_to_start) else stringResource(R.string.resume_analyze),
                    icon = Icons.Outlined.AutoFixHigh,
                    enabled = lib.resumes.isNotEmpty(),
                    onClick = vm::optimize,
                )
                ResumeUi.Loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) }
                is ResumeUi.Result -> Results(s.data, s.fixesApplied, lib.selected?.name, onApplyFixes = vm::applyFixes)
                is ResumeUi.Error -> ApiError(stringResource(s.messageRes), onRetry = vm::optimize, onDismiss = vm::reset)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTargetSheet) {
        TargetSheet(
            options = trackerOptions,
            onGeneral = { vm.setGeneralTarget(); showTargetSheet = false },
            onPick = { vm.setTrackerTarget(it); showTargetSheet = false },
            onManual = { title, company -> vm.setManualTarget(title, company); showTargetSheet = false },
            onDismiss = { showTargetSheet = false },
        )
    }
}

/** JD-attach sheet — general score, a job from the tracker, or a manual title+company. */
@Composable
private fun TargetSheet(
    options: List<TargetOption>,
    onGeneral: () -> Unit,
    onPick: (TargetOption) -> Unit,
    onManual: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var company by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    app.ascend.ui.components.AscendBottomSheet(onDismiss = onDismiss) {
        Text(stringResource(R.string.resume_target_sheet_title), fontWeight = FontWeight.ExtraBold,
            color = AscendColors.Ink, fontSize = 18.sp)
        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = AscendColors.Card,
            border = BorderStroke(1.5.dp, AscendColors.Line), onClick = onGeneral,
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(R.string.resume_target_general), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                Text(stringResource(R.string.resume_target_general_hint), fontSize = 12.5.sp, color = AscendColors.Muted2)
            }
        }

        if (options.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(R.string.resume_target_from_tracker))
            Spacer(Modifier.height(8.dp))
            options.forEach { opt ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(14.dp),
                    color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line),
                    onClick = { onPick(opt) },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(opt.title, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(opt.company, fontSize = 12.5.sp, color = AscendColors.Muted2,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(R.string.resume_target_manual))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text(stringResource(R.string.resume_target_manual_title)) },
            singleLine = true, shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        OutlinedTextField(
            value = company, onValueChange = { company = it },
            label = { Text(stringResource(R.string.resume_target_manual_company)) },
            singleLine = true, shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onManual(title, company) },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
        ) { Text(stringResource(R.string.resume_target_use), fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
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
                contentDescription = if (selected) stringResource(R.string.resume_selected) else stringResource(R.string.resume_select),
                tint = if (selected) AscendColors.Indigo else AscendColors.Muted2,
            )
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Outlined.Description, null, tint = AscendColors.Muted)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val atsLabel = stringResource(R.string.resume_ats_label)
                val documentLabel = stringResource(R.string.resume_document)
                Text(buildString {
                    append(formatSize(record.sizeBytes, documentLabel))
                    record.atsScore?.let { append(" · $atsLabel $it") }
                }, fontSize = 12.sp, color = AscendColors.Muted2)
            }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.resume_remove), color = AscendColors.Muted2, fontSize = 13.sp) }
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
            Text(stringResource(R.string.resume_upload_title), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
            Text(stringResource(R.string.resume_upload_subtitle), fontSize = 12.sp, color = AscendColors.Muted2)
        }
    }
}

@Composable
private fun Results(data: OptimizeResponse, fixesApplied: Boolean, resumeName: String?, onApplyFixes: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val monetization = app.ascend.ui.monetization.rememberMonetizationManager()
    val downloadLinkError = stringResource(R.string.resume_download_link_error)
    val shareSubject = stringResource(R.string.resume_share_subject)
    val shareChooserTitle = stringResource(R.string.resume_share_chooser)
    // Free analysis shows the base score; the rewarded Apply-fixes step reveals the optimized score.
    val shownScore = if (fixesApplied) (data.optimizedScore ?: data.atsScore) else data.atsScore
    Surface(shape = RoundedCornerShape(20.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { shownScore / 100f }, modifier = Modifier.size(72.dp),
                    color = AscendColors.Green, trackColor = AscendColors.Line, strokeWidth = 8.dp)
                Text("$shownScore", fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AscendColors.Ink)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (fixesApplied) stringResource(R.string.resume_optimized_verdict) else data.verdict,
                    fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink,
                )
                Text(data.verdictDetail, fontSize = 13.sp, color = AscendColors.Muted, lineHeight = 18.sp)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    // ad_native_resume_result — below the score, before suggestions (collapses on no-fill).
    NativeAdSlot(Placement.NATIVE_RESUME_RESULT)
    Spacer(Modifier.height(16.dp))
    data.issues.forEach { issue ->
        // Resolved issues only read as "fixed" once the user has applied fixes.
        val resolved = issue.resolved && fixesApplied
        Surface(Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(16.dp),
            color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line)) {
            Row(Modifier.padding(14.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = if (resolved) AscendColors.Green else AscendColors.Amber)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(issue.title, fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.5.sp)
                    Text(issue.detail, fontSize = 12.5.sp, color = AscendColors.Muted, lineHeight = 17.sp)
                }
                SeverityChip(issue.severity)
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    if (!fixesApplied) {
        // ad_rewarded_resume_optimize — Apply AI fixes after the free score (monetization-spec).
        Button(
            onClick = onApplyFixes,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
        ) {
            Icon(Icons.Outlined.AutoFixHigh, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.resume_apply_fixes), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.resume_apply_fixes_note), fontSize = 11.5.sp,
            color = AscendColors.Muted2, lineHeight = 15.sp)
    } else {
        // Download / share the optimized resume.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val url = data.downloadUrl ?: return@OutlinedButton
                    // ad_rewarded_resume_download — gate the export behind a rewarded unlock (Pro
                    // bypasses). The reward is granted only on the earned callback; open the file
                    // only on a grant, never on no-fill/close/offline (rule 5).
                    scope.launch {
                        when (monetization.showRewarded(app.ascend.monetization.Placement.REWARDED_RESUME_DOWNLOAD)) {
                            is app.ascend.monetization.RewardOutcome.NotGranted ->
                                android.widget.Toast.makeText(ctx, downloadLinkError, android.widget.Toast.LENGTH_SHORT).show()
                            else -> if (runCatching { uriHandler.openUri(url) }.isFailure) {
                                android.widget.Toast.makeText(ctx, downloadLinkError, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = data.downloadUrl != null,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.resume_download))
            }
            OutlinedButton(
                onClick = {
                    val summary = buildShareText(data, resumeName)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summary)
                        putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                    }
                    monetization.noteExternalLinkOpened()   // leaving to a share target — suppress app-open on return
                    runCatching { ctx.startActivity(Intent.createChooser(send, shareChooserTitle)) }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.resume_share))
            }
        }
    }
}

/** Small per-issue severity chip (high / medium / low) for the optimizer issue list. */
@Composable
private fun SeverityChip(severity: String) {
    val (label, color) = when (severity.lowercase()) {
        "high", "critical" -> stringResource(R.string.resume_severity_high) to AscendColors.Amber
        "low", "minor" -> stringResource(R.string.resume_severity_low) to AscendColors.Muted2
        else -> stringResource(R.string.resume_severity_medium) to AscendColors.Indigo
    }
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

private fun buildShareText(data: OptimizeResponse, resumeName: String?): String = buildString {
    appendLine("My resume${resumeName?.let { " ($it)" } ?: ""} scored ${data.atsScore}/100 on Ascend's ATS check.")
    data.optimizedScore?.let { appendLine("Optimized score: $it/100.") }
    data.downloadUrl?.let { appendLine(it) }
    append("— optimized with Ascend")
}

private fun formatSize(bytes: Long?, documentLabel: String): String = when {
    bytes == null -> documentLabel
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
            Text(stringResource(R.string.resume_error_title), fontWeight = FontWeight.Bold, color = AscendColors.Amber)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 13.sp, color = AscendColors.Amber)
            Spacer(Modifier.height(12.dp))
            Row {
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
                    ) { Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss), color = AscendColors.Indigo) }
            }
        }
    }
}
