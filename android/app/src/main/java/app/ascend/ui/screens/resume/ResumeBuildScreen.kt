package app.ascend.ui.screens.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.ui.components.AscendCard
import app.ascend.ui.components.AscendIconBadge
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.util.rememberResumePicker

/**
 * Build method chooser — "How do you want to start?" Live methods only (no dead "coming soon"):
 * a guided form, and using an existing file. The hands-free voice path ships in a later increment.
 */
@Composable
fun ResumeBuildScreen(nav: NavController, vm: ResumeHubViewModel = hiltViewModel()) {
    val snackbarHost = remember { SnackbarHostState() }
    val snackbar by vm.snackbar.collectAsStateWithLifecycle()
    val pickResume = rememberResumePicker { vm.addResumeThenOptimize(it) { nav.navigate(Routes.RESUME_OPTIMIZE) } }

    LaunchedEffect(snackbar) {
        snackbar?.let { snackbarHost.showSnackbar(it); vm.clearSnackbar() }
    }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_build_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.resume_build_question), fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            Text(stringResource(R.string.resume_build_sub), fontSize = 13.5.sp,
                color = AscendColors.Muted, lineHeight = 19.sp)
            Spacer(Modifier.height(4.dp))

            MethodCard(
                icon = Icons.Outlined.Mic,
                tint = AscendColors.Violet,
                title = stringResource(R.string.resume_build_voice),
                subtitle = stringResource(R.string.resume_build_voice_sub),
                onClick = { nav.navigate(Routes.RESUME_BUILD_VOICE) },
            )
            MethodCard(
                icon = Icons.Outlined.EditNote,
                tint = AscendColors.Indigo,
                title = stringResource(R.string.resume_build_form),
                subtitle = stringResource(R.string.resume_build_form_sub),
                onClick = { nav.navigate(Routes.RESUME_BUILD_FORM) },
            )
            MethodCard(
                icon = Icons.Outlined.UploadFile,
                tint = AscendColors.Green,
                title = stringResource(R.string.resume_build_upload),
                subtitle = stringResource(R.string.resume_build_upload_sub),
                onClick = pickResume,
            )
        }
    }
}

@Composable
private fun MethodCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    AscendCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AscendIconBadge(icon, tint = tint, size = 46)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.5.sp, color = AscendColors.Muted2, lineHeight = 16.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AscendColors.Faint,
                modifier = Modifier.size(20.dp))
        }
    }
}
