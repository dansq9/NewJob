package app.ascend.ui.screens.tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.data.model.TrackStage
import app.ascend.data.repo.TrackedJob
import app.ascend.ui.components.CompanyAvatar
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun TrackStage.color() = when (this) {
    TrackStage.SAVED -> AscendColors.StageSaved
    TrackStage.APPLIED -> AscendColors.StageApplied
    TrackStage.INTERVIEW -> AscendColors.StageInterview
    TrackStage.OFFER -> AscendColors.StageOffer
    TrackStage.CLOSED -> AscendColors.StageClosed
}

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private fun Long.asDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)

@Composable
fun TrackerScreen(nav: NavController, vm: TrackerViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TrackedJob?>(null) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Job Tracker", style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
                Text("${state.total} jobs · ${state.active} active", fontSize = 13.sp, color = AscendColors.Muted2)
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search title, company, location") },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, "Clear", tint = AscendColors.Muted2) }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(14.dp),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackerSort.entries.forEach { s ->
                    FilterChip(
                        selected = state.sort == s,
                        onClick = { vm.setSort(s) },
                        label = { Text(s.label) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TrackStage.entries.forEach { stage ->
                    StatChip(stage, state.grouped[stage]?.size ?: 0, Modifier.weight(1f))
                }
            }
        }
        if (state.total == 0) {
            item { EmptyTracker(onFind = { nav.navigate(Routes.JOBS) }) }
        } else if (state.grouped.isEmpty()) {
            item { NoMatches() }
        } else {
            TrackStage.entries.forEach { stage ->
                val jobs = state.grouped[stage].orEmpty()
                if (jobs.isNotEmpty()) {
                    item { StageHeader(stage, jobs.size) }
                    items(jobs.size, key = { jobs[it].job.id }) { i ->
                        TrackerCard(jobs[i], onMove = vm::move, onEdit = { editing = jobs[i] })
                    }
                }
            }
        }
    }

    editing?.let { job ->
        TrackerEditSheet(
            item = job,
            onDismiss = { editing = null },
            onSaveNotes = { vm.saveNotes(job.job.id, it) },
            onSetInterview = { vm.saveSchedule(job.job.id, it, it) },
            onClose = { reason -> vm.close(job.job.id, reason); editing = null },
            onDelete = { vm.remove(job.job.id); editing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackerEditSheet(
    item: TrackedJob,
    onDismiss: () -> Unit,
    onSaveNotes: (String?) -> Unit,
    onSetInterview: (Long?) -> Unit,
    onClose: (String?) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var notes by remember(item.job.id) { mutableStateOf(item.notes.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var closeReason by remember(item.job.id) { mutableStateOf(item.closedReason.orEmpty()) }

    fun dismiss() = scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = AscendColors.Bg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompanyAvatar(item.job.company, item.job.logoUrl, size = 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.job.title, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.job.company, fontSize = 13.sp, color = AscendColors.Muted2)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = item.stage.color().copy(alpha = 0.12f)) {
                    Text(item.stage.label, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = item.stage.color(), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            // Interview / follow-up date
            Surface(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = AscendColors.Card,
                border = BorderStroke(1.5.dp, AscendColors.Line),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, null, tint = AscendColors.Indigo)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Interview / follow-up", fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.sp)
                        Text(item.interviewDate?.asDate() ?: "Not set", fontSize = 12.5.sp, color = AscendColors.Muted2)
                    }
                    TextButton(onClick = { showDatePicker = true }) { Text(if (item.interviewDate == null) "Set" else "Change") }
                    if (item.interviewDate != null) {
                        TextButton(onClick = { onSetInterview(null) }) { Text("Clear", color = AscendColors.Muted2) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Notes
            Text("Notes", fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                placeholder = { Text("Recruiter name, salary range, next steps…") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSaveNotes(notes.ifBlank { null }); dismiss() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) { Text("Save notes", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = AscendColors.Line2)
            Spacer(Modifier.height(14.dp))

            if (closing) {
                Text("Why did this close?", fontWeight = FontWeight.Bold, color = AscendColors.Ink, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Rejected", "Withdrew", "Position filled").forEach { r ->
                        FilterChip(selected = closeReason == r, onClick = { closeReason = r }, label = { Text(r, fontSize = 12.sp) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = closeReason, onValueChange = { closeReason = it },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("Reason (optional)") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    singleLine = true, shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onClose(closeReason.ifBlank { null }) },
                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.StageClosed),
                ) { Text("Mark closed", fontWeight = FontWeight.Bold) }
            } else if (item.stage != TrackStage.CLOSED) {
                OutlinedButton(
                    onClick = { closing = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp),
                ) { Text("Mark as closed / lost") }
            } else if (item.closedReason != null) {
                Text("Closed: ${item.closedReason}", fontSize = 13.sp, color = AscendColors.Muted2)
            }

            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Remove from tracker", color = AscendColors.StageClosed, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = item.interviewDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { onSetInterview(dpState.selectedDateMillis); showDatePicker = false }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dpState) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove from tracker?") },
            text = { Text("\"${item.job.title}\" at ${item.job.company} will be removed. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Remove", color = AscendColors.StageClosed) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatChip(stage: TrackStage, count: Int, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line)) {
        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = stage.color())
            Text(stage.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AscendColors.Muted2)
        }
    }
}

@Composable
private fun StageHeader(stage: TrackStage, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(stage.color()))
        Spacer(Modifier.width(8.dp))
        Text(stage.label.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
        Spacer(Modifier.width(8.dp))
        Surface(shape = RoundedCornerShape(999.dp), color = AscendColors.Line) {
            Text("$count", Modifier.padding(horizontal = 8.dp, vertical = 1.dp), fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = AscendColors.Muted2)
        }
    }
}

@Composable
private fun TrackerCard(item: TrackedJob, onMove: (String, TrackStage) -> Unit, onEdit: () -> Unit) {
    val stage = item.stage
    val ordinal = TrackStage.entries.indexOf(stage)
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompanyAvatar(item.job.company, item.job.logoUrl, size = 42)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.job.title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${item.job.company} · ${item.job.workType.label}", fontSize = 12.sp, color = AscendColors.Muted2)
                }
            }
            if (item.interviewDate != null || !item.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.interviewDate?.let {
                        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(15.dp), tint = AscendColors.Indigo)
                        Spacer(Modifier.width(5.dp))
                        Text(it.asDate(), fontSize = 12.sp, color = AscendColors.Indigo, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(12.dp))
                    }
                    if (!item.notes.isNullOrBlank()) {
                        Text(item.notes!!, fontSize = 12.sp, color = AscendColors.Muted2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
            HorizontalDivider(color = AscendColors.Line2)
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onMove(item.job.id, TrackStage.entries[(ordinal - 1).coerceAtLeast(0)]) },
                    enabled = ordinal > 0,
                    modifier = Modifier.size(34.dp),
                ) { Icon(Icons.Outlined.ArrowDownward, "Demote", tint = AscendColors.Muted) }
                Spacer(Modifier.width(8.dp))
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = stage.color().copy(alpha = 0.12f)) {
                    Text(stage.label, Modifier.padding(vertical = 8.dp), color = stage.color(),
                        fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onMove(item.job.id, TrackStage.entries[(ordinal + 1).coerceAtMost(TrackStage.entries.lastIndex)]) },
                    enabled = ordinal < TrackStage.entries.lastIndex,
                    modifier = Modifier.size(34.dp),
                ) { Icon(Icons.Outlined.ArrowUpward, "Promote", tint = stage.color()) }
            }
        }
    }
}

@Composable
private fun NoMatches() {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No matches", fontWeight = FontWeight.Bold, color = AscendColors.Muted)
        Text("Try a different search.", fontSize = 13.sp, color = AscendColors.Muted2)
    }
}

@Composable
private fun EmptyTracker(onFind: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No jobs tracked yet", fontWeight = FontWeight.Bold, color = AscendColors.Muted)
        Spacer(Modifier.height(4.dp))
        Text("Save or apply to jobs and they'll appear here.", fontSize = 13.sp, color = AscendColors.Muted2)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFind, colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text("Find jobs") }
    }
}
