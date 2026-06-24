package app.ascend.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Mic
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
import app.ascend.i18n.LocaleManager
import app.ascend.monetization.Placement
import app.ascend.ui.components.AscendBackCircle
import app.ascend.ui.components.AscendCard
import app.ascend.ui.components.AscendIconBadge
import app.ascend.ui.components.AscendPrimaryButton
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.monetization.NativeAdSlot
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.util.rememberResumePicker

// Prototype onboarding order: Language → Location → Job Title → Product Tour → Resume Upload → Home.
private const val STEP_LANGUAGE = 0
private const val STEP_LOCATION = 1
private const val STEP_JOBTITLE = 2
private const val STEP_TOUR = 3
private const val STEP_RESUME = 4
private const val TOTAL_STEPS = 5

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    var step by rememberSaveable { mutableIntStateOf(STEP_LANGUAGE) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // RC-controlled animation (fail open; reduced-motion aware; never blocks navigation).
    val config = vm.onboardingConfig
    val reducedMotion = config.reduceMotionRespectSystem && isReducedMotion(context)
    val animate = config.animationsEnabled &&
        config.animationVariant != app.ascend.analytics.OnboardingAnimationVariant.NONE && !reducedMotion
    val stepAnimMs = if (animate) config.animationDurationMs.toInt() else 0
    LaunchedEffect(Unit) { vm.onAnimationVariantApplied("onboarding_step") }

    // ---- Product Tour: full-screen RC-controlled slides (own dots/skip). Auto-skip when 0 cards. ----
    if (step == STEP_TOUR) {
        val cards = if (vm.tourStateLoaded) vm.eligibleTourCards(vm.resumeName != null) else -1
        when {
            cards < 0 -> Box(Modifier.fillMaxSize().background(AscendColors.Bg), Alignment.Center) {
                CircularProgressIndicator(color = AscendColors.Indigo)
            }
            cards == 0 -> LaunchedEffect(Unit) { step = STEP_RESUME }
            else -> OnboardingTour(
                cardCount = cards,
                showSkip = config.showSkip,
                forceCompletion = config.forceCompletion,
                onView = { vm.onTourView(it) },
                onSkip = { vm.onTourSkip(it) },
                onComplete = { vm.onTourComplete(it) },
                onResolve = { step = STEP_RESUME },
            )
        }
        return
    }

    val canContinue = when (step) {
        STEP_LOCATION -> vm.location.isNotBlank()
        STEP_JOBTITLE -> vm.role.isNotBlank()
        else -> true
    }
    val pickResume = rememberResumePicker { vm.onResumePicked(it) }

    Column(Modifier.fillMaxSize().background(AscendColors.Bg).statusBarsPadding().padding(22.dp)) {
        // top bar: back circle + progress segments
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > STEP_LANGUAGE) AscendBackCircle(onClick = { step-- }) else Spacer(Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(TOTAL_STEPS) { i ->
                    Box(
                        Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(if (i <= step) AscendColors.Indigo else AscendColors.Line),
                    )
                }
            }
        }
        Spacer(Modifier.height(26.dp))

        Box(Modifier.weight(1f)) {
            Crossfade(targetState = step, animationSpec = tween(stepAnimMs), label = "onboardingStep") { s ->
                when (s) {
                    STEP_LANGUAGE -> LanguageStep(context)
                    STEP_LOCATION -> LocationStep(vm)
                    STEP_JOBTITLE -> JobTitleStep(vm)
                    else -> ResumeUploadStep(vm, pickResume)
                }
            }
        }

        if (vm.saveFailed) {
            Text(
                stringResource(R.string.onboarding_save_error), color = Color(0xFFB3261E), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }

        if (step == STEP_RESUME) {
            AscendPrimaryButton(
                text = if (vm.resumeName != null) stringResource(R.string.onboarding_resume_continue)
                else stringResource(R.string.action_continue),
                enabled = !vm.saving,
                gradient = vm.resumeName != null,
                onClick = { vm.logStep(app.ascend.analytics.OnboardingStep.RESUME, vm.resumeName == null); vm.finish(onDone) },
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = { vm.logStep(app.ascend.analytics.OnboardingStep.RESUME, true); vm.finish(onDone) },
                enabled = !vm.saving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            ) { Text(stringResource(R.string.onboarding_resume_skip), color = AscendColors.Muted, fontWeight = FontWeight.SemiBold) }
        } else {
            AscendPrimaryButton(
                text = stringResource(R.string.action_continue),
                enabled = canContinue,
                modifier = Modifier.navigationBarsPadding(),
                onClick = {
                    when (step) {
                        STEP_LANGUAGE -> vm.logStep(app.ascend.analytics.OnboardingStep.LANGUAGE, false)
                        STEP_LOCATION -> vm.logStep(app.ascend.analytics.OnboardingStep.LOCATION, vm.location.isBlank())
                        STEP_JOBTITLE -> vm.logStep(app.ascend.analytics.OnboardingStep.ROLE, vm.role.isBlank())
                    }
                    step++
                },
            )
        }
    }
}

@Composable
private fun LanguageStep(context: Context) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Heading(stringResource(R.string.onboarding_language_title), stringResource(R.string.onboarding_language_sub))
        // Selecting a language persists it and recreates the Activity so the rest of onboarding
        // (still on this first step) re-renders localized — no new analytics, reuses LocaleManager.
        val current = remember { LocaleManager.persistedTag(context) }
        LocaleManager.supported.forEach { lang ->
            val selected = lang.tag == current
            Surface(
                onClick = { LocaleManager.setLanguage(context, lang.tag); context.findActivity()?.recreate() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) AscendColors.Indigo.copy(alpha = 0.06f) else AscendColors.Card,
                border = BorderStroke(1.5.dp, if (selected) AscendColors.Indigo else AscendColors.Line),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(lang.endonym, fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 15.sp)
                        Text(lang.english, fontSize = 12.5.sp, color = AscendColors.Muted2)
                    }
                    if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = AscendColors.Indigo)
                }
            }
        }
    }
}

@Composable
private fun LocationStep(vm: OnboardingViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    // Real geolocation: request COARSE location permission, then reverse-geocode to a city.
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { locating = true; scope.launch { vm.location = resolveCity(context); locating = false } }
    }
    val useCurrent: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locating = true; scope.launch { vm.location = resolveCity(context); locating = false }
        } else {
            permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Heading(stringResource(R.string.onboarding_location_title), stringResource(R.string.onboarding_location_sub))
        AscendCard(onClick = useCurrent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AscendIconBadge(Icons.Outlined.MyLocation, tint = AscendColors.Indigo)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.onboarding_location_current), fontWeight = FontWeight.Bold, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                if (locating) CircularProgressIndicator(Modifier.size(18.dp), color = AscendColors.Indigo, strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Field(
            "", stringResource(R.string.onboarding_location_hint), vm.location, { vm.location = it },
            stringResource(R.string.onboarding_location_hint), KeyboardCapitalization.Words, hideHeading = true,
        )
        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(R.string.onboarding_section_popular))
        Spacer(Modifier.height(10.dp))
        SelectableChips(
            listOf("Remote", "San Francisco, CA", "New York, NY", "Austin, TX", "London, UK"),
            selected = vm.location,
        ) { vm.location = it }
    }
}

@Composable
private fun JobTitleStep(vm: OnboardingViewModel) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Field(
            stringResource(R.string.onboarding_role_title), stringResource(R.string.onboarding_role_sub),
            vm.role, { vm.role = it }, stringResource(R.string.onboarding_role_hint), KeyboardCapitalization.Words,
        )
        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(R.string.onboarding_section_popular))
        Spacer(Modifier.height(10.dp))
        SelectableChips(
            listOf("Product Manager", "Software Engineer", "Data Analyst", "Designer", "Marketing Manager"),
            selected = vm.role,
        ) { vm.role = it }
    }
}

@Composable
private fun ResumeUploadStep(vm: OnboardingViewModel, pick: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Heading(stringResource(R.string.onboarding_resume_title), stringResource(R.string.onboarding_resume_sub))
        Surface(
            onClick = pick, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), color = Color(0xFFF3F1FF), border = BorderStroke(2.dp, Color(0xFFC7C2F7)),
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (vm.resumeName == null) {
                    Icon(Icons.Outlined.UploadFile, null, tint = AscendColors.Indigo, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.onboarding_resume_upload), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                    Text(stringResource(R.string.onboarding_resume_formats), fontSize = 12.sp, color = AscendColors.Muted2)
                } else {
                    Icon(Icons.Outlined.CheckCircle, null, tint = AscendColors.Green, modifier = Modifier.size(40.dp))
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
        Spacer(Modifier.height(12.dp))
        // "No resume? Build one by voice" — prototype card (shell; advances like skip for now).
        AscendCard(onClick = pick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AscendIconBadge(Icons.Outlined.Mic, tint = AscendColors.Violet2)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.onboarding_resume_voice), fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.5.sp)
                    Text(stringResource(R.string.onboarding_resume_voice_sub), fontSize = 12.sp, color = AscendColors.Muted2)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        NativeAdSlot(placement = Placement.NATIVE_ONBOARDING_FINAL, modifier = Modifier.fillMaxWidth())
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
    placeholder: String, caps: KeyboardCapitalization = KeyboardCapitalization.Sentences, hideHeading: Boolean = false,
) {
    Column {
        if (!hideHeading) Heading(title, sub)
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

/** Suggestion chips that highlight the one matching [selected]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectableChips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            val isSel = opt.equals(selected, ignoreCase = true)
            Surface(
                onClick = { onPick(opt) }, shape = RoundedCornerShape(999.dp),
                color = if (isSel) AscendColors.ChipIndigo else AscendColors.Card,
                border = BorderStroke(1.5.dp, if (isSel) AscendColors.Indigo else AscendColors.Line),
            ) {
                Text(
                    opt, Modifier.padding(horizontal = 15.dp, vertical = 9.dp), fontSize = 13.5.sp,
                    color = if (isSel) AscendColors.Indigo else AscendColors.Ink, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Reduced-motion when the system animator scale is 0 (Developer options / accessibility). */
private fun isReducedMotion(context: Context): Boolean = try {
    android.provider.Settings.Global.getFloat(
        context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    ) == 0f
} catch (e: Exception) {
    false
}

/**
 * Best-effort reverse-geocode of the last known location to a "City, Region" string.
 * Falls back to a neutral label if no fix or no geocoder result. Runs off the main thread.
 * Caller must already hold ACCESS_COARSE_LOCATION.
 */
@Suppress("DEPRECATION", "MissingPermission")
private suspend fun resolveCity(context: Context): String = withContext(Dispatchers.IO) {
    val fallback = "Current location"
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext fallback
    val loc = try {
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
    } catch (e: SecurityException) {
        null
    } catch (e: Exception) {
        null
    } ?: return@withContext fallback
    val addr = try {
        Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
    } catch (e: Exception) {
        null
    } ?: return@withContext fallback
    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
    val region = addr.adminArea
    when {
        city == null -> fallback
        region != null && region != city -> "$city, $region"
        else -> city
    }
}

/** Walk the ContextWrapper chain to the hosting Activity (for locale-change recreate). */
private fun Context.findActivity(): android.app.Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}
