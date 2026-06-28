package app.ascend.ui.screens.resume

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
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
 * The Resume hub — the single entry from Home. Routes to Build / Edit / Check-for-a-job,
 * with a top "Use a file I have" shortcut so the already-have-a-PDF user is one tap from value.
 * (See docs/resume-hub-design.md.)
 */
@Composable
fun ResumeHubScreen(nav: NavController, vm: ResumeHubViewModel = hiltViewModel()) {
    val savedCount by vm.savedCount.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val snackbar by vm.snackbar.collectAsStateWithLifecycle()
    // Adding a file here jumps straight into the optimizer (the "use a file I have" hot path).
    val pickResume = rememberResumePicker { vm.addResumeThenOptimize(it) { nav.navigate(Routes.RESUME_OPTIMIZE) } }

    LaunchedEffect(snackbar) {
        snackbar?.let { snackbarHost.showSnackbar(it); vm.clearSnackbar() }
    }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_hub_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.resume_hub_subtitle),
                fontSize = 14.sp, color = AscendColors.Muted, lineHeight = 19.sp,
            )

            // "Continue where you left off" — additive shortcut; the hub stays the default landing
            // and we never deep-link into a single resume (per the persona panel).
            vm.lastAction?.let { action ->
                val (label, route) = when (action) {
                    ResumeAction.OPTIMIZE -> stringResource(R.string.resume_continue_optimize) to Routes.RESUME_OPTIMIZE
                    ResumeAction.BUILD -> stringResource(R.string.resume_continue_build) to Routes.RESUME_BUILD
                    ResumeAction.EDIT -> stringResource(R.string.resume_continue_edit) to Routes.RESUME_EDIT
                }
                ContinueChip(label = label, onClick = { nav.navigate(route) })
            }

            HubCard(
                icon = Icons.Outlined.AutoFixHigh,
                tint = AscendColors.Indigo,
                title = stringResource(R.string.resume_hub_build),
                subtitle = stringResource(R.string.resume_hub_build_sub),
                onClick = { nav.navigate(Routes.RESUME_BUILD) },
            )
            HubCard(
                icon = Icons.Outlined.Edit,
                tint = AscendColors.Violet,
                title = stringResource(R.string.resume_hub_edit),
                // Empty library: muted hint, and tapping routes into Build instead of a dead list.
                subtitle = if (savedCount == 0) stringResource(R.string.resume_hub_edit_empty)
                else stringResource(R.string.resume_hub_edit_sub),
                muted = savedCount == 0,
                onClick = { nav.navigate(if (savedCount == 0) Routes.RESUME_BUILD else Routes.RESUME_EDIT) },
            )
            HubCard(
                icon = Icons.Outlined.Description,
                tint = AscendColors.Green,
                title = stringResource(R.string.resume_hub_check),
                subtitle = stringResource(R.string.resume_hub_check_sub),
                onClick = { nav.navigate(Routes.RESUME_OPTIMIZE) },
            )

            Spacer(Modifier.height(2.dp))
            // "Use a file I have" — promote upload so it isn't buried inside Build.
            AscendCard(onClick = pickResume) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AscendIconBadge(Icons.Outlined.UploadFile, tint = AscendColors.Muted)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.resume_hub_use_file), fontSize = 15.sp,
                            fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                        Text(stringResource(R.string.resume_hub_use_file_sub), fontSize = 12.5.sp,
                            color = AscendColors.Muted2, lineHeight = 16.sp)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AscendColors.Faint,
                        modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.resume_hub_hint),
                fontSize = 12.5.sp, color = AscendColors.Muted2, lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun ContinueChip(label: String, onClick: () -> Unit) {
    AscendCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AscendIconBadge(Icons.AutoMirrored.Outlined.ArrowForward, tint = AscendColors.Indigo)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.resume_continue_label), fontSize = 11.5.sp,
                    color = AscendColors.Muted2, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AscendColors.Indigo)
            }
        }
    }
}

@Composable
private fun HubCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    AscendCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AscendIconBadge(icon, tint = if (muted) AscendColors.Muted2 else tint, size = 46)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.5.sp,
                    color = if (muted) AscendColors.Faint else AscendColors.Muted2, lineHeight = 16.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AscendColors.Faint,
                modifier = Modifier.size(20.dp))
        }
    }
}
