package app.ascend.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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

    // Seed editable fields once the stored profile arrives.
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var location by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(profile) {
        if (name == null) name = profile.name
        if (role == null) role = profile.targetRole
        if (location == null) location = profile.location
    }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar("Settings", onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            SectionLabel("Your profile")
            Spacer(Modifier.height(10.dp))
            Field("Name", name ?: "", { name = it }, KeyboardCapitalization.Words)
            Spacer(Modifier.height(12.dp))
            Field("Target role", role ?: "", { role = it }, KeyboardCapitalization.Words)
            Spacer(Modifier.height(12.dp))
            Field("Location", location ?: "", { location = it }, KeyboardCapitalization.Words)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    vm.save(name.orEmpty(), role.orEmpty(), location.orEmpty())
                    scope.launch { snackbar.showSnackbar("Profile saved") }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) { Text("Save profile", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Resume")
            Spacer(Modifier.height(10.dp))
            RowCard("Manage resumes", profile.resumeName ?: "No resume added yet") { nav.navigate(Routes.RESUME) }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Data & backup")
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = AscendColors.Card, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Your profile, saved jobs and tracker back up to your Google account and restore on a new device. This device's anonymous ID always stays local.",
                    Modifier.padding(14.dp), fontSize = 12.5.sp, color = AscendColors.Muted2, lineHeight = 18.sp,
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("Reset")
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Reset onboarding", color = AscendColors.StageClosed, fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(28.dp))
            Text(vm.versionLabel, Modifier.fillMaxWidth(), color = AscendColors.Muted2, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset onboarding?") },
        text = { Text("Your name, role, location and resume selection will be cleared and you'll go through setup again. Saved jobs and your resume library are kept.") },
        confirmButton = {
            TextButton(onClick = {
                confirmReset = false
                vm.resetOnboarding {
                    nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            }) { Text("Reset", color = AscendColors.StageClosed) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
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
private fun RowCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp), color = AscendColors.Card,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Description, null, tint = AscendColors.Indigo)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.5.sp, color = AscendColors.Muted2)
            }
        }
    }
}
