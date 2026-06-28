package app.ascend.ui.screens.resume

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * The guided resume builder — a paged 6-step wizard (Contact → Summary → Experience → Education →
 * Skills → Review) with a fixable strength meter, AI-write assist, repeating work-experience
 * entries, and a live preview on the Review step. Built resumes persist to the library; the final
 * download is rewarded-gated (monetization-spec). New resume or editing an existing one (Edit flow).
 */
@Composable
fun ResumeBuilderScreen(nav: NavController, resumeId: String? = null, vm: ResumeBuilderViewModel = hiltViewModel()) {
    LaunchedEffect(resumeId) { vm.start(resumeId) }
    val form by vm.form.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val stepIndex by vm.step.collectAsStateWithLifecycle()
    val aiBusy by vm.aiBusy.collectAsStateWithLifecycle()
    app.ascend.ui.monetization.SuppressAppOpenWhileActive(app.ascend.monetization.AdFlow.RESUME)

    val steps = vm.steps
    val current = steps[stepIndex]

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = {
            AscendTopBar(
                stringResource(R.string.resume_build_form_title),
                onBack = { if (stepIndex > 0 && ui is BuilderUi.Editing) vm.prevStep() else nav.popBackStack() },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(8.dp))
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
                        StepProgress(stepIndex, steps.size, vm.strength)
                        // Fast path: jump to the live preview without walking every step.
                        if (current != BuildStep.REVIEW) {
                            Spacer(Modifier.height(6.dp))
                            TextButton(onClick = { vm.goToStep(steps.lastIndex) }, contentPadding = PaddingValues(0.dp)) {
                                Text(stringResource(R.string.resume_skip_to_preview), color = AscendColors.Indigo,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        when (current) {
                            BuildStep.CONTACT -> ContactStep(form, vm)
                            BuildStep.SUMMARY -> SummaryStep(form, vm, aiBusy)
                            BuildStep.EXPERIENCE -> ExperienceStep(form, vm)
                            BuildStep.BACKGROUND -> {
                                EducationStep(form, vm)
                                Spacer(Modifier.height(18.dp))
                                SkillsStep(form, vm)
                            }
                            BuildStep.REVIEW -> ReviewStep(form)
                        }
                        if (current == BuildStep.REVIEW && !vm.canGenerate) {
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.resume_review_need_name), fontSize = 12.5.sp,
                                color = AscendColors.Amber, lineHeight = 16.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
            if (ui is BuilderUi.Editing) {
                WizardFooter(
                    isLast = vm.isLastStep,
                    canGenerate = vm.canGenerate,
                    onBack = { if (stepIndex > 0) vm.prevStep() else nav.popBackStack() },
                    onNext = { vm.nextStep() },
                    onCreate = { vm.generate() },
                )
            }
        }
    }
}

@Composable
private fun StepProgress(stepIndex: Int, count: Int, strength: Int) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(count) { i ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(if (i <= stepIndex) AscendColors.Indigo else AscendColors.Line)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.resume_build_step_of, stepIndex + 1, count),
                fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AscendColors.Indigo)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.resume_strength_label) + " $strength/100",
                fontSize = 11.5.sp, color = AscendColors.Muted2)
        }
    }
}

@Composable
private fun ContactStep(form: BuilderForm, vm: ResumeBuilderViewModel) {
    SectionLabel(stringResource(R.string.resume_section_contact))
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.resume_build_opener), fontSize = 13.sp, color = AscendColors.Muted, lineHeight = 18.sp)
    Spacer(Modifier.height(12.dp))
    Field(stringResource(R.string.resume_field_name), form.name) { v -> vm.update { it.copy(name = v) } }
    Field(stringResource(R.string.resume_field_email), form.email) { v -> vm.update { it.copy(email = v) } }
    Field(stringResource(R.string.resume_field_phone), form.phone) { v -> vm.update { it.copy(phone = v) } }
    Field(stringResource(R.string.resume_field_location), form.location) { v -> vm.update { it.copy(location = v) } }
}

@Composable
private fun SummaryStep(form: BuilderForm, vm: ResumeBuilderViewModel, aiBusy: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(stringResource(R.string.resume_field_summary))
        Spacer(Modifier.weight(1f))
        TextButton(onClick = vm::aiWriteSummary, enabled = !aiBusy) {
            if (aiBusy) CircularProgressIndicator(Modifier.size(15.dp), color = AscendColors.Indigo, strokeWidth = 2.dp)
            else Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp), tint = AscendColors.Indigo)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.resume_ai_write), color = AscendColors.Indigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(8.dp))
    Field(stringResource(R.string.resume_field_summary_hint), form.summary, minLines = 4) { v -> vm.update { it.copy(summary = v) } }
    Text(stringResource(R.string.resume_ai_write_note), fontSize = 11.sp, color = AscendColors.Muted2, lineHeight = 15.sp)
}

@Composable
private fun ExperienceStep(form: BuilderForm, vm: ResumeBuilderViewModel) {
    SectionLabel(stringResource(R.string.resume_field_experience))
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = form.noExperienceYet, onCheckedChange = { c -> vm.update { it.copy(noExperienceYet = c) } })
        Text(stringResource(R.string.resume_no_experience), fontSize = 13.sp, color = AscendColors.Muted)
    }
    Spacer(Modifier.height(8.dp))
    form.experiences.forEachIndexed { i, exp ->
        Surface(
            Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp),
            color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.resume_exp_entry, i + 1), fontWeight = FontWeight.Bold,
                        color = AscendColors.Ink, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    if (form.experiences.size > 1) {
                        TextButton(onClick = { vm.removeExperience(i) }) {
                            Text(stringResource(R.string.resume_exp_remove), color = AscendColors.Muted2, fontSize = 12.5.sp)
                        }
                    }
                }
                Field(stringResource(R.string.resume_exp_role), exp.role) { v -> vm.updateExperience(i) { it.copy(role = v) } }
                Field(stringResource(R.string.resume_exp_company), exp.company) { v -> vm.updateExperience(i) { it.copy(company = v) } }
                Field(stringResource(R.string.resume_exp_dates), exp.dates) { v -> vm.updateExperience(i) { it.copy(dates = v) } }
                Field(stringResource(R.string.resume_exp_detail), exp.detail, minLines = 3) { v -> vm.updateExperience(i) { it.copy(detail = v) } }
            }
        }
    }
    OutlinedButton(onClick = vm::addExperience, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.resume_exp_add))
    }
}

@Composable
private fun EducationStep(form: BuilderForm, vm: ResumeBuilderViewModel) {
    SectionLabel(stringResource(R.string.resume_field_education))
    Spacer(Modifier.height(8.dp))
    Field(stringResource(R.string.resume_field_education), form.education, minLines = 3) { v -> vm.update { it.copy(education = v) } }
}

@Composable
private fun SkillsStep(form: BuilderForm, vm: ResumeBuilderViewModel) {
    SectionLabel(stringResource(R.string.resume_field_skills))
    Spacer(Modifier.height(8.dp))
    Field(stringResource(R.string.resume_field_skills_hint), form.skills, minLines = 3) { v -> vm.update { it.copy(skills = v) } }
}

/** Live preview — the assembled resume on a "paper" card. */
@Composable
private fun ReviewStep(form: BuilderForm) {
    SectionLabel(stringResource(R.string.resume_review_title))
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.resume_review_sub), fontSize = 13.sp, color = AscendColors.Muted, lineHeight = 18.sp)
    Spacer(Modifier.height(12.dp))
    Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(form.name.ifBlank { stringResource(R.string.resume_field_name) }, fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            val contact = listOf(form.email, form.phone, form.location).filter { it.isNotBlank() }.joinToString("  ·  ")
            if (contact.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(contact, fontSize = 12.5.sp, color = AscendColors.Muted2)
            }
            if (form.summary.isNotBlank()) { PreviewSection(stringResource(R.string.resume_field_summary)); Text(form.summary, fontSize = 13.sp, color = AscendColors.Ink, lineHeight = 18.sp) }
            val exps = form.experiences.filterNot { it.isBlank }
            if (exps.isNotEmpty()) {
                PreviewSection(stringResource(R.string.resume_field_experience))
                exps.forEach { e ->
                    if (e.heading.isNotBlank()) Text(e.heading, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                    if (e.dates.isNotBlank()) Text(e.dates, fontSize = 11.5.sp, color = AscendColors.Muted2)
                    if (e.detail.isNotBlank()) Text(e.detail, fontSize = 12.5.sp, color = AscendColors.Muted, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (form.education.isNotBlank()) { PreviewSection(stringResource(R.string.resume_field_education)); Text(form.education, fontSize = 13.sp, color = AscendColors.Ink, lineHeight = 18.sp) }
            if (form.skills.isNotBlank()) { PreviewSection(stringResource(R.string.resume_field_skills)); Text(form.skills, fontSize = 13.sp, color = AscendColors.Ink, lineHeight = 18.sp) }
        }
    }
}

@Composable
private fun PreviewSection(label: String) {
    Spacer(Modifier.height(14.dp))
    Text(label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AscendColors.Indigo, fontFamily = JetBrainsMono)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun WizardFooter(isLast: Boolean, canGenerate: Boolean, onBack: () -> Unit, onNext: () -> Unit, onCreate: () -> Unit) {
    Surface(color = AscendColors.Bg, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(stringResource(R.string.action_back))
            }
            if (isLast) {
                Button(onClick = onCreate, enabled = canGenerate, modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.resume_build_generate), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onNext, modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                    Text(stringResource(R.string.resume_build_next), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, minLines = minLines,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp),
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
        border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
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
        OutlinedButton(onClick = onEditMore, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) {
            Text(stringResource(R.string.resume_build_edit_more))
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.resume_build_download_note), fontSize = 11.5.sp,
        color = AscendColors.Muted2, lineHeight = 15.sp)
}
