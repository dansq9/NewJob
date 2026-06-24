package app.ascend.ui.screens.jobs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.core.Resource
import app.ascend.data.model.Job
import app.ascend.ui.components.JobCard
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(nav: NavController, vm: JobsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val adsEnabled by vm.adsEnabled.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showFilters by remember { mutableStateOf(false) }
    // Job rows interleaved with native ad slots (computed here; remember can't run in LazyListScope).
    val feed = remember(state.jobs, adsEnabled) { buildFeed(state.jobs, adsEnabled) }

    // Infinite scroll: load more when near the end.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.totalItemsCount to (info.visibleItemsInfo.lastOrNull()?.index ?: 0)
        }.collect { (total, last) -> if (total > 0 && last >= total - 3) vm.loadMore() }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Find your next role", style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink) }
        item {
            OutlinedTextField(
                value = state.query, onValueChange = vm::onQueryChange, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2) },
                placeholder = { Text("Title, company or keyword") }, singleLine = true, shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AscendColors.Indigo, unfocusedBorderColor = AscendColors.Line,
                    focusedContainerColor = AscendColors.Card, unfocusedContainerColor = AscendColors.Card,
                ),
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = AscendColors.Indigo, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text(state.location.ifBlank { "Anywhere" }, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                    color = AscendColors.Ink, modifier = Modifier.weight(1f))
                FilterButton(state.filters.activeCount) { showFilters = true }
            }
        }

        when (val st = state.status) {
            Resource.Loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) } }
            is Resource.Error ->
                if (st.rateLimited) item { RateLimitState() } else item { ErrorState(st.message, onRetry = vm::search) }
            is Resource.Success -> {
                if (state.jobs.isEmpty()) item { EmptyState() }
                else {
                    items(feed.size) { idx ->
                        when (val row = feed[idx]) {
                            is FeedRow.JobItem -> JobCard(
                                job = row.job,
                                onClick = { vm.select(row.job); nav.navigate(Routes.jobDetail(row.job.id)) },
                                saved = state.savedIds.contains(row.job.id),
                                onToggleSave = { vm.toggleSave(row.job) },
                            )
                            FeedRow.Ad -> NativeAdSlot()
                        }
                    }
                    if (state.loadingMore) item { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(26.dp), color = AscendColors.Indigo, strokeWidth = 2.dp) } }
                    else if (state.endReached) item {
                        Text("You've reached the end · ${state.jobs.size} jobs", Modifier.fillMaxWidth().padding(14.dp),
                            color = AscendColors.Muted2, fontSize = 12.5.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(state.filters, onApply = { vm.setFilters(it); showFilters = false }, onDismiss = { showFilters = false })
    }
}

@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp),
        color = if (count > 0) AscendColors.ChipIndigo else AscendColors.Card,
        border = BorderStroke(1.5.dp, if (count > 0) AscendColors.Indigo else AscendColors.Line)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, tint = AscendColors.Indigo, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Filters", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = AscendColors.Indigo)
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(AscendColors.Indigo), Alignment.Center) {
                    Text("$count", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(current: JobFilters, onApply: (JobFilters) -> Unit, onDismiss: () -> Unit) {
    var draft by remember { mutableStateOf(current) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AscendColors.Card) {
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                TextButton(onClick = { draft = JobFilters() }) { Text("Reset", color = AscendColors.Indigo) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Remote only", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                Switch(checked = draft.remoteOnly, onCheckedChange = { draft = draft.copy(remoteOnly = it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AscendColors.Indigo))
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("Date posted")
            ChipRow(
                listOf("all" to "Any", "today" to "Today", "3days" to "3 days", "week" to "Week", "month" to "Month"),
                selected = setOf(draft.datePosted),
            ) { draft = draft.copy(datePosted = it) }
            Spacer(Modifier.height(14.dp))
            SectionLabel("Employment type")
            ChipRow(
                listOf("FULLTIME" to "Full-time", "PARTTIME" to "Part-time", "CONTRACTOR" to "Contract", "INTERN" to "Internship"),
                selected = draft.employmentTypes,
            ) { key -> draft = draft.copy(employmentTypes = draft.employmentTypes.toggle(key)) }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onApply(draft) }, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Text("Show results", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun Set<String>.toggle(v: String) = if (contains(v)) this - v else this + v

@Composable
private fun SectionLabel(text: String) =
    Text(text.uppercase(), color = AscendColors.Muted2, fontSize = 12.sp, fontWeight = FontWeight.Bold)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: Set<String>, onPick: (String) -> Unit) {
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val sel = selected.contains(key)
            FilterChip(selected = sel, onClick = { onPick(key) }, label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AscendColors.ChipIndigo, selectedLabelColor = AscendColors.Indigo),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = sel,
                    borderColor = AscendColors.Line, selectedBorderColor = AscendColors.Indigo))
        }
    }
}

@Composable
private fun RateLimitState() {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Daily search limit reached", fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text("You've hit the JSearch quota for now. Try again later, or upgrade the plan.",
            fontSize = 13.sp, color = AscendColors.Muted2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Couldn't load jobs", fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 13.sp, color = AscendColors.Muted2)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text("Try again") }
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

@Composable
private fun NativeAdSlot() {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(AscendColors.ChipIndigo), Alignment.Center) {
                Icon(Icons.Outlined.Campaign, null, tint = AscendColors.Indigo)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sponsored", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                Text("Your native ad here", fontSize = 13.sp, color = AscendColors.Muted)
            }
            Text("Ad", color = Color(0xFFB0A06A), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFFBF4D9)).padding(horizontal = 7.dp, vertical = 2.dp))
        }
    }
}
