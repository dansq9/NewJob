package app.ascend.ui.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.ascend.R
import app.ascend.monetization.Placement
import app.ascend.ui.monetization.NativeAdSlot
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.util.rememberResumePicker

private const val STEPS = 5

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val canContinue = when (step) {
        1 -> vm.name.isNotBlank()
        2 -> vm.role.isNotBlank()
        3 -> vm.location.isNotBlank()
        else -> true
    }
    val pickResume = rememberResumePicker { vm.onResumePicked(it) }

    // RC-controlled animations (fail open; reduced-motion aware; never blocks navigation).
    val config = vm.onboardingConfig
    val context = androidx.compose.ui.platform.LocalContext.current
    val reducedMotion = config.reduceMotionRespectSystem && isReducedMotion(context)
    val animate = config.animationsEnabled &&
        config.animationVariant != app.ascend.analytics.OnboardingAnimationVariant.NONE && !reducedMotion
    val stepAnimMs = if (animate) config.animationDurationMs.toInt() else 0
    LaunchedEffect(Unit) { vm.onAnimationVariantApplied("onboarding_step") }

    // RC-controlled tour-guide overlay. Placement maps to a concrete step boundary.
    //
    // NOTE (Product/QA): this native onboarding has NO language step. `before_language` and
    // `after_language` are LEGACY/ABSTRACT placement names carried in Remote Config for cross-
    // platform parity; in THIS flow they map to the Welcome and Name steps respectively. Exact
    // mapping (steps: 0=Welcome, 1=Name, 2=Role, 3=Location, 4=Resume, then finish → Home):
    //   before_language → step 0 (before/at Welcome — the first screen)
    //   after_language  → step 1 (at the Name step)
    //   after_location  → step 4 (after Location, at the Resume step)
    //   before_home     → after finishing onboarding, before the first main screen
    // Future: add before_welcome / after_welcome aliases when RC schema is revised.
    // Fails open: eligibleTourCards == 0 → tour is skipped entirely.
    val triggerStep = remember(config.placement) {
        when (config.placement) {
            app.ascend.analytics.OnboardingTourPlacement.BEFORE_LANGUAGE -> 0   // Welcome (no language step in this flow)
            app.ascend.analytics.OnboardingTourPlacement.AFTER_LANGUAGE -> 1     // Name (no language step in this flow)
            app.ascend.analytics.OnboardingTourPlacement.AFTER_LOCATION -> 4     // Resume (after Location)
            app.ascend.analytics.OnboardingTourPlacement.BEFORE_HOME -> STEPS    // after finish, before Home
        }
    }
    var tourResolved by rememberSaveable { mutableStateOf(false) }
    var tourActive by remember { mutableStateOf(false) }
    var tourCards by remember { mutableIntStateOf(0) }
    var tourThenFinish by remember { mutableStateOf(false) }
    LaunchedEffect(step, vm.tourStateLoaded) {
        if (vm.tourStateLoaded && !tourResolved && !tourActive && triggerStep < STEPS && step == triggerStep) {
            val cards = vm.eligibleTourCards(vm.resumeName != null)
            if (cards > 0) { tourCards = cards; tourThenFinish = false; tourActive = true } else tourResolved = true
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(AscendColors.Bg).statusBarsPadding().padding(22.dp)) {
        // top bar: back + progress
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) IconButton(onClick = { step-- }, modifier = Modifier.size(38.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AscendColors.Muted)
            } else Spacer(Modifier.size(38.dp))
            Spacer(Modifier.width(8.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(STEPS) { i ->
                    Box(
                        Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (i <= step) AscendColors.Indigo else AscendColors.Line)
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))

        Box(Modifier.weight(1f)) {
            Crossfade(targetState = step, animationSpec = tween(stepAnimMs), label = "onboardingStep") { s ->
                when (s) {
                    0 -> Welcome()
                    1 -> Field(stringResource(R.string.onboarding_name_title), stringResource(R.string.onboarding_name_sub),
                        vm.name, { vm.name = it }, stringResource(R.string.onboarding_name_hint), KeyboardCapitalization.Words)
                    2 -> RoleStep(vm)
                    3 -> LocationStep(vm)
                    else -> ResumeStep(vm, pickResume)
                }
            }
        }

        if (vm.saveFailed) {
            Text(stringResource(R.string.onboarding_save_error), color = Color(0xFFB3261E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Button(
            onClick = {
                // onboarding_step for the schema-mapped steps (role/location/resume).
                when (step) {
                    2 -> vm.logStep(app.ascend.analytics.OnboardingStep.ROLE, vm.role.isBlank())
                    3 -> vm.logStep(app.ascend.analytics.OnboardingStep.LOCATION, vm.location.isBlank())
                    STEPS - 1 -> vm.logStep(app.ascend.analytics.OnboardingStep.RESUME, vm.resumeName == null)
                }
                if (step < STEPS - 1) {
                    step++
                } else if (!tourResolved && triggerStep == STEPS) {
                    // before_home placement: show the tour, then finish (or finish now if not eligible).
                    val cards = vm.eligibleTourCards(vm.resumeName != null)
                    if (cards > 0) { tourCards = cards; tourThenFinish = true; tourActive = true }
                    else { tourResolved = true; vm.finish(onDone) }
                } else {
                    vm.finish(onDone)
                }
            },
            enabled = canContinue && !vm.saving,
            modifier = Modifier.fillMaxWidth().height(54.dp).navigationBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
        ) {
            Text(
                stringResource(when (step) { 0 -> R.string.onboarding_get_started; STEPS - 1 -> R.string.onboarding_finish; else -> R.string.action_continue }),
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
        }
    } // end Column
        if (tourActive) {
            OnboardingTour(
                cardCount = tourCards,
                showSkip = config.showSkip,
                forceCompletion = config.forceCompletion,
                onView = { vm.onTourView(it) },
                onSkip = { vm.onTourSkip(it) },
                onComplete = { vm.onTourComplete(it) },
                onResolve = {
                    tourActive = false
                    tourResolved = true
                    if (tourThenFinish) vm.finish(onDone)
                },
            )
        }
    } // end Box
}

/** Reduced-motion when the system animator scale is 0 (Developer options / accessibility). */
private fun isReducedMotion(context: android.content.Context): Boolean = try {
    android.provider.Settings.Global.getFloat(
        context.contentResolver,
        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
} catch (e: Exception) {
    false
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Box(
            Modifier.size(86.dp).clip(RoundedCornerShape(24.dp)).background(AscendColors.Indigo),
            contentAlignment = Alignment.Center,
        ) { Text("↑↑", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.onboarding_welcome_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_welcome_sub),
            fontSize = 15.sp, color = AscendColors.Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp), lineHeight = 22.sp)
    }
}

@Composable
private fun Heading(title: String, sub: String) {
    Column {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, lineHeight = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text(sub, fontSize = 15.sp, color = AscendColors.Muted, lineHeight = 21.sp)
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun Field(
    title: String, sub: String, value: String, onChange: (String) -> Unit,
    placeholder: String, caps: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    Column { Heading(title, sub)
        OutlinedTextField(
            value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) }, singleLine = true, shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AscendColors.Indigo, unfocusedBorderColor = AscendColors.Line,
                focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card,
            ),
        )
    }
}

@Composable
private fun RoleStep(vm: OnboardingViewModel) {
    Column {
        Field(stringResource(R.string.onboarding_role_title), stringResource(R.string.onboarding_role_sub),
            vm.role, { vm.role = it }, stringResource(R.string.onboarding_role_hint), KeyboardCapitalization.Words)
        Spacer(Modifier.height(16.dp))
        Chips(listOf("Product Manager", "Software Engineer", "Data Analyst", "Designer", "Marketing Manager")) { vm.role = it }
    }
}

@Composable
private fun LocationStep(vm: OnboardingViewModel) {
    Column {
        Field(stringResource(R.string.onboarding_location_title), stringResource(R.string.onboarding_location_sub),
            vm.location, { vm.location = it }, stringResource(R.string.onboarding_location_hint), KeyboardCapitalization.Words)
        Spacer(Modifier.height(16.dp))
        Chips(listOf("Remote", "San Francisco, CA", "New York, NY", "Austin, TX", "London, UK")) { vm.location = it }
    }
}

@Composable
private fun ResumeStep(vm: OnboardingViewModel, pick: () -> Unit) {
    Column {
        Heading(stringResource(R.string.onboarding_resume_title), stringResource(R.string.onboarding_resume_sub))
        Surface(
            onClick = pick, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), color = Color(0xFFF3F1FF),
            border = BorderStroke(2.dp, Color(0xFFC7C2F7)),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (vm.resumeName == null) {
                    Icon(Icons.Outlined.UploadFile, null, tint = AscendColors.Indigo, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.onboarding_resume_upload), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                    Text(stringResource(R.string.onboarding_resume_formats), fontSize = 12.sp, color = AscendColors.Muted2)
                } else {
                    Icon(Icons.Filled.CheckCircle, null, tint = AscendColors.Green, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(vm.resumeName!!, fontWeight = FontWeight.Bold, color = AscendColors.Ink, maxLines = 1)
                    Text(stringResource(R.string.onboarding_resume_replace), fontSize = 12.sp, color = AscendColors.Muted2)
                }
            }
        }
        vm.resumeError?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFFB3261E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (vm.resumeName != null) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { vm.clearResume() }) { Text(stringResource(R.string.action_remove), color = AscendColors.Muted) }
        }
        Spacer(Modifier.height(16.dp))
        NativeAdSlot(placement = Placement.NATIVE_ONBOARDING_FINAL, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Chips(options: List<String>, onPick: (String) -> Unit) {
    FlowChips(options, onPick)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(options: List<String>, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            Surface(
                onClick = { onPick(opt) }, shape = RoundedCornerShape(999.dp),
                color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line),
            ) { Text(opt, Modifier.padding(horizontal = 15.dp, vertical = 9.dp), fontSize = 13.5.sp, color = AscendColors.Ink, fontWeight = FontWeight.SemiBold) }
        }
    }
}
