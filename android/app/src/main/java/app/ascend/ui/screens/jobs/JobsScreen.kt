package app.ascend.ui.screens.jobs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.ascend.data.model.Job
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.core.Resource
import app.ascend.ui.components.JobCard
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

@Composable
fun JobsScreen(nav: NavController, vm: JobsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val adsEnabled by vm.adsEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Find your next role", style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2) },
                placeholder = { Text("Title, company or keyword") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AscendColors.Indigo, unfocusedBorderColor = AscendColors.Line,
                    focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card,
                ),
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.LocationOn, null, tint = AscendColors.Indigo, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text(state.location, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                    color = AscendColors.Ink, modifier = Modifier.weight(1f))
                FilterChip(
                    selected = state.remoteOnly,
                    onClick = vm::toggleRemote,
                    label = { Text("Remote only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AscendColors.ChipIndigo,
                        selectedLabelColor = AscendColors.Indigo,
                    ),
                    border = BorderStroke(1.5.dp, if (state.remoteOnly) AscendColors.Indigo else AscendColors.Line),
                )
            }
        }

        when (val r = state.results) {
            is Resource.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    CircularProgressIndicator(color = AscendColors.Indigo)
                }
            }
            is Resource.Error -> item {
                ErrorState(r.message, onRetry = vm::search)
            }
            is Resource.Success -> {
                if (r.data.isEmpty()) item { EmptyState() }
                else {
                    val feed = remember(r.data, adsEnabled) { buildFeed(r.data, adsEnabled) }
                    items(feed.size) { idx ->
                        when (val row = feed[idx]) {
                            is FeedRow.JobItem -> JobCard(
                                job = row.job,
                                onClick = { vm.select(row.job); nav.navigate(Routes.JOB_DETAIL) },
                                saved = state.savedIds.contains(row.job.id),
                                onToggleSave = { vm.toggleSave(row.job) },
                            )
                            FeedRow.Ad -> NativeAdSlot()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn't load jobs", fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 13.sp, color = AscendColors.Muted2)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
            Text("Try again")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No jobs match your search", fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text("Try a different keyword or widen your filters.", fontSize = 13.sp, color = AscendColors.Muted2)
    }
}

// ── native ad interleaving (every 5 listings, free users only) ───────────────
private sealed interface FeedRow {
    data class JobItem(val job: Job) : FeedRow
    data object Ad : FeedRow
}

private fun buildFeed(jobs: List<Job>, adsEnabled: Boolean): List<FeedRow> = buildList {
    jobs.forEachIndexed { i, job ->
        add(FeedRow.JobItem(job))
        if (adsEnabled && (i + 1) % 5 == 0 && i != jobs.lastIndex) add(FeedRow.Ad)
    }
}

/** Placeholder native-ad card. Swap the inner content for a real AdMob NativeAd view. */
@Composable
private fun NativeAdSlot() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(AscendColors.ChipIndigo),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Campaign, null, tint = AscendColors.Indigo) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Sponsored", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                    Text("Your native ad here", fontSize = 13.sp, color = AscendColors.Muted)
                }
                Text(
                    "Ad", color = Color(0xFFB0A06A), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFFBF4D9)).padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
    }
}
