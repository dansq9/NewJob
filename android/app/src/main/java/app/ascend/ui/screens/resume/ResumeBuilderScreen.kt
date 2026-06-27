package app.ascend.ui.screens.resume

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.data.remote.platform.GenerateResponse
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

/**
 * The guided-form builder. A single forgiving form (Contact → Summary → Experience → Education →
 * Skills) with a friendly, fixable strength meter and a neutral opener so no-experience users
 * aren't disqualified. On finish it calls the platform generate endpoint and offers a
 * rewarded-gated download (per monetization-spec). Auto-save to the library lands in a later increment.
 */
@Composable
fun ResumeBuilderScreen(nav: NavController, vm: ResumeBuilderViewModel = hiltViewModel()) {
    val form by vm.form.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    // Suppress the app-open ad while in the resume build flow (spec suppress_during_resume_flow).
    app.ascend.ui.monetization.SuppressAppOpenWhileActive(app.ascend.monetization.AdFlow.RESUME)

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_build_form_title), onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            when (val s = ui) {
                BuilderUi.Generating -> Box(Modifier.fillMaxWidth().padding(top = 60.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AscendColors.Indigo)
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.resume_build_generating), color = AscendColors.Muted)
                    }
                }
                is BuilderUi.Done -> DoneCard(s.data, onEditMore = vm::backToEditing)
                is BuilderUi.Error -> ApiError(stringResource(s.messageRes), onRetry = vm::generate, onDismiss = vm::backToEditing)
                BuilderUi.Editing -> {
                    // Strength meter — fixable, never pass/fail.
                    StrengthHeader(vm.strength)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.resume_build_opener), fontSize = 13.5.sp,
                        color = AscendColors.Muted, lineHeight = 19.sp)
                    Spacer(Modifier.height(18.dp))

                    SectionLabel(stringResource(R.string.resume_section_contact))
                    Spacer(Modifier.height(10.dp))
                    Field(stringResource(R.string.resume_field_name), form.name) { v -> vm.update { it.copy(name = v) } }
                    Field(stringResource(R.string.resume_field_email), form.email) { v -> vm.update { it.copy(email = v) } }
                    Field(stringResource(R.string.resume_field_phone), form.phone) { v -> vm.update { it.copy(phone = v) } }
                    Field(stringResource(R.string.resume_field_location), form.location) { v -> vm.update { it.copy(location = v) } }

                    Spacer(Modifier.height(18.dp))
                    SectionLabel(stringResource(R.string.resume_field_summary))
                    Spacer(Modifier.height(10.dp))
                    Field(stringResource(R.string.resume_field_summary_hint), form.summary, minLines = 3) { v -> vm.update { it.copy(summary = v) } }

                    Spacer(Modifier.height(18.dp))
                    SectionLabel(stringResource(R.string.resume_field_experience))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = form.noExperienceYet, onCheckedChange = { c -> vm.update { it.copy(noExperienceYet = c) } })
                        Text(stringResource(R.string.resume_no_experience), fontSize = 13.sp, color = AscendColors.Muted)
                    }
                    Spacer(Modifier.height(4.dp))
                    Field(stringResource(R.string.resume_field_experience_hint), form.experience, minLines = 3) { v -> vm.update { it.copy(experience = v) } }

                    Spacer(Modifier.height(18.dp))
                    SectionLabel(stringResource(R.string.resume_field_education))
                    Spacer(Modifier.height(10.dp))
                    Field(stringResource(R.string.resume_field_education), form.education, minLines = 2) { v -> vm.update { it.copy(education = v) } }

                    Spacer(Modifier.height(18.dp))
                    SectionLabel(stringResource(R.string.resume_field_skills))
                    Spacer(Modifier.height(10.dp))
                    Field(stringResource(R.string.resume_field_skills_hint), form.skills, minLines = 2) { v -> vm.update { it.copy(skills = v) } }

                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = vm::generate,
                        enabled = vm.canGenerate,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null); Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.resume_build_generate), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StrengthHeader(strength: Int) {
    Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AscendColors.Line)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.resume_strength_label), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                Spacer(Modifier.weight(1f))
                Text("$strength/100", fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold,
                    color = AscendColors.Indigo, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { strength / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = AscendColors.Green, trackColor = AscendColors.Line,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.resume_strength_hint), fontSize = 12.sp, color = AscendColors.Muted2, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun Field(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun DoneCard(data: GenerateResponse, onEditMore: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val monetization = app.ascend.ui.monetization.rememberMonetizationManager()
    val downloadLinkError = stringResource(R.string.resume_download_link_error)
    Spacer(Modifier.height(8.dp))
    Surface(shape = RoundedCornerShape(20.dp), color = AscendColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = AscendColors.Green)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.resume_build_done_title), fontWeight = FontWeight.ExtraBold,
                    color = AscendColors.Ink, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(data.summary ?: stringResource(R.string.resume_build_done_sub),
                fontSize = 13.5.sp, color = AscendColors.Muted, lineHeight = 19.sp)
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                val url = data.downloadUrl ?: return@Button
                // ad_rewarded_resume_download — gate export behind a rewarded unlock (Pro bypasses).
                // Open only on the earned grant, never on no-fill/close/offline (rule 5).
                scope.launch {
                    when (monetization.showRewarded(app.ascend.monetization.Placement.REWARDED_RESUME_DOWNLOAD)) {
                        is app.ascend.monetization.RewardOutcome.NotGranted ->
                            Toast.makeText(ctx, downloadLinkError, Toast.LENGTH_SHORT).show()
                        else -> if (runCatching { uriHandler.openUri(url) }.isFailure) {
                            Toast.makeText(ctx, downloadLinkError, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            enabled = data.downloadUrl != null,
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
        ) {
            Icon(Icons.Outlined.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.resume_build_download))
        }
        OutlinedButton(
            onClick = onEditMore,
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(14.dp),
        ) { Text(stringResource(R.string.resume_build_edit_more)) }
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.resume_build_download_note), fontSize = 11.5.sp,
        color = AscendColors.Muted2, lineHeight = 15.sp)
}
