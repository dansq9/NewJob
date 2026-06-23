package app.ascend.ui.screens.tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

private fun TrackStage.color() = when (this) {
    TrackStage.SAVED -> AscendColors.StageSaved
    TrackStage.APPLIED -> AscendColors.StageApplied
    TrackStage.INTERVIEW -> AscendColors.StageInterview
    TrackStage.OFFER -> AscendColors.StageOffer
    TrackStage.CLOSED -> AscendColors.StageClosed
}

@Composable
fun TrackerScreen(nav: NavController, vm: TrackerViewModel = hiltViewModel()) {
    val grouped by vm.grouped.collectAsStateWithLifecycle()
    val total = grouped.values.sumOf { it.size }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Job Tracker", style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
                Text("$total jobs · ${grouped.filterKeys { it != TrackStage.CLOSED }.values.sumOf { it.size }} active",
                    fontSize = 13.sp, color = AscendColors.Muted2)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TrackStage.entries.forEach { stage ->
                    StatChip(stage, grouped[stage]?.size ?: 0, Modifier.weight(1f))
                }
            }
        }
        if (total == 0) {
            item { EmptyTracker(onFind = { nav.navigate(Routes.JOBS) }) }
        } else {
            TrackStage.entries.forEach { stage ->
                val jobs = grouped[stage].orEmpty()
                if (jobs.isNotEmpty()) {
                    item { StageHeader(stage, jobs.size) }
                    items(jobs.size, key = { jobs[it].job.id }) { i ->
                        TrackerCard(jobs[i], onMove = vm::move)
                    }
                }
            }
        }
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
private fun TrackerCard(item: TrackedJob, onMove: (String, TrackStage) -> Unit) {
    val stage = item.stage
    val ordinal = TrackStage.entries.indexOf(stage)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line)) {
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
private fun EmptyTracker(onFind: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No jobs tracked yet", fontWeight = FontWeight.Bold, color = AscendColors.Muted)
        Spacer(Modifier.height(4.dp))
        Text("Save or apply to jobs and they'll appear here.", fontSize = 13.sp, color = AscendColors.Muted2)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFind, colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text("Find jobs") }
    }
}
