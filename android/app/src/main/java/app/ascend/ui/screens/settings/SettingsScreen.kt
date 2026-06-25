package app.ascend.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.i18n.LocaleManager
import app.ascend.monetization.Placement
import app.ascend.ui.components.AscendStatCard
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.monetization.NativeAdSlot
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController, vm: SettingsViewModel = hiltViewModel()) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profileSavedMsg = stringResource(R.string.settings_profile_saved)
    val restoreMsg = stringResource(R.string.profile_row_restore)

    var showEdit by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    val monetization = app.ascend.ui.monetization.rememberMonetizationManager()

    val currentTag = LocaleManager.persistedTag(context)
    val currentLanguage = LocaleManager.supported.firstOrNull { it.tag == currentTag }
        ?: LocaleManager.supported.first()

    fun openUrl(url: String) {
        monetization.noteExternalLinkOpened()
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.settings_profile_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            // --- Profile card (tap to edit) ---
            Surface(
                onClick = { showEdit = true }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line),
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AscendColors.Indigo, AscendColors.Violet))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            profile.name.trim().firstOrNull()?.uppercase() ?: "A",
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name.ifBlank { stringResource(R.string.home_fallback_name) }, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                        val sub = listOf(profile.targetRole, profile.location).filter { it.isNotBlank() }.joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, fontSize = 13.sp, color = AscendColors.Muted)
                    }
                    Icon(Icons.Outlined.Edit, null, tint = AscendColors.Faint)
                }
            }

            Spacer(Modifier.height(12.dp))
            // --- Pro card ---
            if (isPro) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(AscendColors.Indigo, AscendColors.Violet2))).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = Color(0xFFFFD66B))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.profile_pro_active), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(AscendColors.Ink, Color(0xFF2A2A3C))))
                        .clickable { nav.navigate(Routes.PAYWALL) }.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x29FFD66B)), Alignment.Center) {
                        Icon(Icons.Outlined.WorkspacePremium, null, tint = Color(0xFFFFD66B))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.profile_upgrade_title), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(stringResource(R.string.profile_upgrade_sub), color = Color(0xFFA0A0AD), fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AscendColors.Muted)
                }
            }

            Spacer(Modifier.height(12.dp))
            // --- Stats row ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AscendStatCard(stats.applied.toString(), stringResource(R.string.profile_stat_applied), Modifier.weight(1f))
                AscendStatCard(stats.saved.toString(), stringResource(R.string.profile_stat_saved), Modifier.weight(1f))
                AscendStatCard(stats.ats?.toString() ?: "—", stringResource(R.string.profile_stat_ats), Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))
            // --- Settings list (grouped) ---
            Surface(shape = RoundedCornerShape(18.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileRow(Icons.Outlined.Description, stringResource(R.string.profile_row_documents)) { nav.navigate(Routes.RESUME) }
                    RowDivider()
                    ProfileRow(Icons.Outlined.BookmarkBorder, stringResource(R.string.profile_row_saved_jobs)) { nav.navigate(Routes.TRACKER) }
                    RowDivider()
                    ProfileRow(Icons.Outlined.Language, stringResource(R.string.settings_app_language), value = currentLanguage.endonym) { showLanguages = true }
                    RowDivider()
                    ProfileRow(Icons.Outlined.CardMembership, stringResource(R.string.profile_row_manage_sub)) {
                        openUrl("https://play.google.com/store/account/subscriptions")
                    }
                    RowDivider()
                    ProfileRow(Icons.Outlined.Restore, stringResource(R.string.profile_row_restore)) {
                        vm.restore(); scope.launch { snackbar.showSnackbar(restoreMsg) }
                    }
                    RowDivider()
                    ProfileRow(Icons.Outlined.HelpOutline, stringResource(R.string.profile_row_help)) { openUrl("https://ascend.app/help") }
                    if (vm.privacyOptionsRequired()) {
                        RowDivider()
                        ProfileRow(Icons.Outlined.PrivacyTip, stringResource(R.string.settings_privacy_options)) {
                            (context as? Activity)?.let { monetization.notePermissionPrompt(); vm.showPrivacyOptions(it) }
                        }
                    }
                    RowDivider()
                    ProfileRow(Icons.Outlined.RestartAlt, stringResource(R.string.settings_reset_onboarding), tint = AscendColors.StageClosed) { confirmReset = true }
                }
            }

            Spacer(Modifier.height(16.dp))
            // ad_native_language — collapses on no-fill.
            NativeAdSlot(Placement.NATIVE_LANGUAGE)

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.settings_version, vm.versionName, vm.versionCode),
                Modifier.fillMaxWidth(), color = AscendColors.Muted2, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
        }
    }

    // --- Edit profile bottom sheet ---
    if (showEdit) {
        var name by remember { mutableStateOf(profile.name) }
        var role by remember { mutableStateOf(profile.targetRole) }
        var location by remember { mutableStateOf(profile.location) }
        ModalBottomSheet(onDismissRequest = { showEdit = false }, containerColor = AscendColors.Card) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
                Text(stringResource(R.string.profile_edit_title), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                Spacer(Modifier.height(14.dp))
                EditField(stringResource(R.string.settings_field_name), name, { name = it }, KeyboardCapitalization.Words)
                Spacer(Modifier.height(12.dp))
                EditField(stringResource(R.string.settings_field_role), role, { role = it }, KeyboardCapitalization.Words)
                Spacer(Modifier.height(12.dp))
                EditField(stringResource(R.string.settings_field_location), location, { location = it }, KeyboardCapitalization.Words)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        vm.save(name, role, location)
                        showEdit = false
                        scope.launch { snackbar.showSnackbar(profileSavedMsg) }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
                ) { Text(stringResource(R.string.settings_save_profile), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
            }
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
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    tint: Color = Color(0xFF5C5C6B),
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(13.dp))
        Text(label, Modifier.weight(1f), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Ink2)
        if (value != null) {
            Text(value, fontSize = 12.5.sp, color = AscendColors.Muted2, fontFamily = JetBrainsMono)
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AscendColors.Faint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(color = AscendColors.Line2, modifier = Modifier.padding(horizontal = 16.dp))

@Composable
private fun EditField(label: String, value: String, onChange: (String) -> Unit, caps: KeyboardCapitalization) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(13.dp),
        keyboardOptions = KeyboardOptions(capitalization = caps, imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card,
        ),
    )
}
