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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import app.ascend.ui.components.AscendEmptyState
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import kotlinx.coroutines.launch

/**
 * Edit a resume — the saved-resume library with non-destructive management (rename, duplicate,
 * delete-with-undo). Uploaded files open in the optimizer ("open to check for a job"); structured
 * editing of built resumes arrives with the Build editor increment. Reuses [ResumeViewModel].
 */
@Composable
fun ResumeEditScreen(nav: NavController, vm: ResumeViewModel = hiltViewModel()) {
    val lib by vm.library.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.markEntered(ResumeAction.EDIT) }
    var renameTarget by remember { mutableStateOf<ResumeRecord?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMsg = stringResource(R.string.resume_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar(stringResource(R.string.resume_edit_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
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
                        // Built resumes open the structured editor; uploaded files open the optimizer.
                        onOpen = {
                            vm.select(r.id)
                            if (r.isBuilt) nav.navigate(Routes.resumeBuildEdit(r.id))
                            else nav.navigate(Routes.RESUME_OPTIMIZE)
                        },
                        onRename = { renameTarget = r },
                        onDuplicate = { vm.duplicate(r.id) },
                        onDelete = {
                            vm.remove(r.id)
                            // Soft delete: offer Undo before the removal is final.
                            scope.launch {
                                val res = snackbarHost.showSnackbar(deletedMsg, actionLabel = undoLabel)
                                if (res == SnackbarResult.ActionPerformed) vm.restore(r)
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.resume_local_only_note), fontSize = 11.5.sp,
                    color = AscendColors.Muted2, lineHeight = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    renameTarget?.let { rec ->
        RenameDialog(
            initial = rec.title ?: rec.name,
            onConfirm = { vm.rename(rec.id, it); renameTarget = null },
            onDismiss = { renameTarget = null },
        )
    }
}

@Composable
private fun LibraryRow(
    record: ResumeRecord,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, null, tint = AscendColors.Muted)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.displayName, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val atsLabel = stringResource(R.string.resume_ats_label)
                    val hint = if (record.isBuilt) stringResource(R.string.resume_edit_built_hint)
                    else stringResource(R.string.resume_edit_uploaded_hint)
                    Text(buildString {
                        append(hint)
                        record.atsScore?.let { append(" · $atsLabel $it") }
                    }, fontSize = 12.sp, color = AscendColors.Muted2)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = AscendColors.Faint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton(onClick = onRename) { Text(stringResource(R.string.resume_rename), color = AscendColors.Indigo, fontSize = 13.sp) }
                TextButton(onClick = onDuplicate) { Text(stringResource(R.string.resume_duplicate), color = AscendColors.Indigo, fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text(stringResource(R.string.resume_remove), color = AscendColors.Muted2, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_save), color = AscendColors.Indigo, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = AscendColors.Muted) }
        },
        title = { Text(stringResource(R.string.resume_rename), fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true,
                label = { Text(stringResource(R.string.resume_rename_label)) },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
            )
        },
        containerColor = AscendColors.Card,
        shape = RoundedCornerShape(20.dp),
    )
}
