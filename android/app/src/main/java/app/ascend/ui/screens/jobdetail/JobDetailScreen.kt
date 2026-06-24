package app.ascend.ui.screens.jobdetail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.ui.components.CompanyAvatar
import app.ascend.ui.components.Pill
import app.ascend.ui.i18n.label
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(nav: NavController, vm: JobDetailViewModel = hiltViewModel()) {
    val job by vm.job.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val stage by vm.stage.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val openLinkError = stringResource(R.string.error_open_link)
    val j = job

    // System back also counts as closing the detail (ad_inter_after_job_detail_close).
    androidx.activity.compose.BackHandler { vm.onClose(); nav.popBackStack() }

    // After the user opens the external apply link and returns, prompt to track it.
    var pendingApply by remember { mutableStateOf(false) }
    var showApplyPrompt by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && pendingApply) { pendingApply = false; showApplyPrompt = true }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        containerColor = AscendColors.Card,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { vm.onClose(); nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AscendColors.Ink)
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleSave) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            stringResource(R.string.action_save), tint = if (saved) AscendColors.Indigo else AscendColors.Muted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AscendColors.Card),
            )
        },
        bottomBar = {
            if (j?.applyUrl != null) Surface(color = AscendColors.Card, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.jobdetail_apply_site), fontSize = 11.sp, color = AscendColors.Muted2, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.jobdetail_opens_browser), fontSize = 13.sp, color = AscendColors.Ink, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            // A malformed URL or a device with no browser must not crash the app.
                            val opened = runCatching { uri.openUri(j.applyUrl!!) }.isSuccess
                            if (opened) { pendingApply = true; vm.onApplyExternal() }
                            else android.widget.Toast.makeText(ctx, openLinkError, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(R.string.jobdetail_apply), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp)); Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp))
                    }
                }
            }
        },
    ) { padding ->
        if (j == null) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.jobdetail_unavailable_title), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.jobdetail_unavailable_body),
                    fontSize = 13.sp, color = AscendColors.Muted2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { nav.navigate(Routes.JOBS) { popUpTo(Routes.JOB_DETAIL) { inclusive = true } } },
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
                ) { Text(stringResource(R.string.jobdetail_browse)) }
            }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 20.dp),
        ) {
            CompanyAvatar(j.company, j.logoUrl, size = 62)
            Spacer(Modifier.height(14.dp))
            Text(j.title, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, lineHeight = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text("${j.company} · ${j.location}", fontSize = 15.sp, color = AscendColors.Muted)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                j.employmentType?.let { Pill(it, AscendColors.Muted, AscendColors.Bg) }
                j.salary?.let { Pill(it, AscendColors.Muted, AscendColors.Bg) }
            }
            if (stage != null) {
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.jobdetail_pipeline_stage), fontSize = 12.sp, color = AscendColors.Muted2, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    app.ascend.data.model.TrackStage.entries.forEach { s ->
                        FilterChip(
                            selected = stage == s,
                            onClick = { vm.setStage(s) },
                            label = { Text(label(s), fontSize = 12.5.sp) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(stringResource(R.string.jobdetail_action_resume), Icons.Outlined.AutoFixHigh, AscendColors.Green, Modifier.weight(1f)) { nav.navigate(Routes.RESUME) }
                ActionTile(stringResource(R.string.jobdetail_action_copilot), Icons.Outlined.Bolt, AscendColors.Violet2, Modifier.weight(1f)) { nav.navigate(Routes.COPILOT) }
                ActionTile(stringResource(R.string.jobdetail_action_mock), Icons.Outlined.RecordVoiceOver, AscendColors.Indigo, Modifier.weight(1f)) { nav.navigate(Routes.MOCK) }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.jobdetail_about), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            Spacer(Modifier.height(8.dp))
            Text(
                j.description?.take(1200) ?: stringResource(R.string.jobdetail_no_description),
                fontSize = 14.sp, color = AscendColors.Body, lineHeight = 22.sp,
            )
            Spacer(Modifier.height(20.dp))
            // ad_native_job_detail_bottom — below the description (collapses on no-fill).
            app.ascend.ui.monetization.NativeAdSlot(app.ascend.monetization.Placement.NATIVE_JOB_DETAIL_BOTTOM)
        }
    }

    if (showApplyPrompt && j != null) AlertDialog(
        onDismissRequest = { showApplyPrompt = false },
        title = { Text(stringResource(R.string.jobdetail_applied_q_title)) },
        text = { Text(stringResource(R.string.jobdetail_applied_q_body, j.title, j.company)) },
        confirmButton = { TextButton(onClick = { vm.markApplied(); showApplyPrompt = false }) { Text(stringResource(R.string.jobdetail_applied_q_yes)) } },
        dismissButton = { TextButton(onClick = { showApplyPrompt = false }) { Text(stringResource(R.string.jobdetail_applied_q_no)) } },
    )
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, tint: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp),
        color = AscendColors.Card, border = androidx.compose.foundation.BorderStroke(1.5.dp, AscendColors.Line)) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
        }
    }
}
