package app.ascend.ui.screens.resume

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.data.repo.ResumeRecord
import app.ascend.ui.components.AscendConfirmationDialog
import app.ascend.ui.components.AscendEmptyState
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

/**
 * Edit a resume — the saved-resume library. Today every record is an uploaded file pointer, so
 * tapping a row opens it in the optimizer ("open to optimize, not edit"); structured in-app editing
 * of built resumes arrives with the library-model increment. Reuses [ResumeViewModel] for the list.
 */
@Composable
fun ResumeEditScreen(nav: NavController, vm: ResumeViewModel = hiltViewModel()) {
    val lib by vm.library.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ResumeRecord?>(null) }

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_edit_title), onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            if (lib.resumes.isEmpty()) {
                Spacer(Modifier.height(40.dp))
                AscendEmptyState(
                    icon = Icons.Outlined.EditNote,
                    title = stringResource(R.string.resume_edit_empty_title),
                    subtitle = stringResource(R.string.resume_edit_empty_sub),
                    actionLabel = stringResource(R.string.resume_hub_build),
                    onAction = { nav.navigate(Routes.RESUME_BUILD) },
                )
            } else {
                Text(stringResource(R.string.resume_edit_sub), fontSize = 13.5.sp,
                    color = AscendColors.Muted, lineHeight = 19.sp)
                Spacer(Modifier.height(14.dp))
                lib.resumes.forEach { r ->
                    LibraryRow(
                        record = r,
                        onOpen = { vm.select(r.id); nav.navigate(Routes.RESUME_OPTIMIZE) },
                        onRemove = { pendingDelete = r },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingDelete?.let { rec ->
        AscendConfirmationDialog(
            title = stringResource(R.string.resume_delete_title),
            text = stringResource(R.string.resume_delete_text),
            confirmLabel = stringResource(R.string.resume_delete_confirm),
            dismissLabel = stringResource(R.string.action_dismiss),
            onConfirm = { vm.remove(rec.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun LibraryRow(record: ResumeRecord, onOpen: () -> Unit, onRemove: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Description, null, tint = AscendColors.Muted)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val atsLabel = stringResource(R.string.resume_ats_label)
                val uploadedHint = stringResource(R.string.resume_edit_uploaded_hint)
                Text(buildString {
                    append(uploadedHint)
                    record.atsScore?.let { append(" · $atsLabel $it") }
                }, fontSize = 12.sp, color = AscendColors.Muted2)
            }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.resume_remove), color = AscendColors.Muted2, fontSize = 13.sp)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AscendColors.Faint, modifier = Modifier.size(18.dp))
        }
    }
}
