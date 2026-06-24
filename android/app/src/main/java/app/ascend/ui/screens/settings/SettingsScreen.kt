package app.ascend.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.i18n.LocaleManager
import app.ascend.monetization.Placement
import app.ascend.ui.monetization.NativeAdSlot
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profileSavedMsg = stringResource(R.string.settings_profile_saved)

    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var location by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(profile) {
        if (name == null) name = profile.name
        if (role == null) role = profile.targetRole
        if (location == null) location = profile.location
    }
    var confirmReset by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    val monetization = app.ascend.ui.monetization.rememberMonetizationManager()

    val currentTag = LocaleManager.persistedTag(context)
    val currentLanguage = LocaleManager.supported.firstOrNull { it.tag == currentTag }
        ?: LocaleManager.supported.first()

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.settings_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            SectionLabel(stringResource(R.string.settings_section_profile))
            Spacer(Modifier.height(10.dp))
            Field(stringResource(R.string.settings_field_name), name ?: "", { name = it }, KeyboardCapitalization.Words)
            Spacer(Modifier.height(12.dp))
            Field(stringResource(R.string.settings_field_role), role ?: "", { role = it }, KeyboardCapitalization.Words)
            Spacer(Modifier.height(12.dp))
            Field(stringResource(R.string.settings_field_location), location ?: "", { location = it }, KeyboardCapitalization.Words)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    vm.save(name.orEmpty(), role.orEmpty(), location.orEmpty())
                    scope.launch { snackbar.showSnackbar(profileSavedMsg) }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) { Text(stringResource(R.string.settings_save_profile), fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_resume))
            Spacer(Modifier.height(10.dp))
            RowCard(
                Icons.Outlined.Description,
                stringResource(R.string.settings_manage_resumes),
                profile.resumeName ?: stringResource(R.string.settings_no_resume),
            ) { nav.navigate(Routes.RESUME) }

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_language))
            Spacer(Modifier.height(10.dp))
            RowCard(
                Icons.Outlined.Language,
                stringResource(R.string.settings_app_language),
                currentLanguage.endonym,
            ) { showLanguages = true }
            Spacer(Modifier.height(12.dp))
            // ad_native_language — the app's language-selection surface (collapses on no-fill).
            NativeAdSlot(Placement.NATIVE_LANGUAGE)

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_data))
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = AscendColors.Card, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_backup_note), Modifier.padding(14.dp),
                    fontSize = 12.5.sp, color = AscendColors.Muted2, lineHeight = 18.sp)
            }

            // UMP "Privacy options" — only where the consent mandate requires it (EEA/UK/CH).
            if (vm.privacyOptionsRequired()) {
                Spacer(Modifier.height(10.dp))
                RowCard(
                    Icons.Outlined.PrivacyTip,
                    stringResource(R.string.settings_privacy_options),
                    stringResource(R.string.settings_privacy_options_sub),
                ) { (context as? Activity)?.let { monetization.notePermissionPrompt(); vm.showPrivacyOptions(it) } }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_section_reset))
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text(stringResource(R.string.settings_reset_onboarding), color = AscendColors.StageClosed, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.settings_version, vm.versionName, vm.versionCode),
                Modifier.fillMaxWidth(), color = AscendColors.Muted2, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showLanguages) AlertDialog(
        onDismissRequest = { showLanguages = false },
        title = { Text(stringResource(R.string.settings_choose_language)) },
        text = {
            Column {
                LocaleManager.supported.forEach { lang ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = lang.tag == currentTag, onClick = {
                                showLanguages = false
                                if (lang.tag != currentTag) {
                                    LocaleManager.setLanguage(context, lang.tag)
                                    (context as? Activity)?.recreate()
                                }
                            })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = lang.tag == currentTag, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(lang.endonym, color = AscendColors.Ink)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { showLanguages = false }) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.settings_reset_confirm_title)) },
        text = { Text(stringResource(R.string.settings_reset_confirm_body)) },
        confirmButton = {
            TextButton(onClick = {
                confirmReset = false
                vm.resetOnboarding {
                    nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }) { Text(stringResource(R.string.settings_reset_confirm_action), color = AscendColors.StageClosed) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, caps: KeyboardCapitalization) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card,
        ),
    )
}

@Composable
private fun RowCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), color = AscendColors.Card,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AscendColors.Indigo)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.5.sp, color = AscendColors.Muted2)
            }
        }
    }
}
