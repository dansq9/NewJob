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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.core.Resource
import app.ascend.data.model.Job
import app.ascend.ui.components.JobCard
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(nav: NavController, vm: JobsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nativeAdAllowed by vm.nativeAdAllowed.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showFilters by remember { mutableStateOf(false) }
    // Job rows interleaved with native ad slots (computed here; remember can't run in LazyListScope).
    val feed = remember(state.jobs, nativeAdAllowed, vm.nativeFrequency) {
        buildFeed(state.jobs, nativeAdAllowed, vm.nativeFrequency)
    }

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
        item { Text(stringResource(R.string.jobs_title), style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink) }
        item {
            OutlinedTextField(
                value = state.query, onValueChange = vm::onQueryChange, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2) },
                placeholder = { Text(stringResource(R.string.jobs_search_hint)) }, singleLine = true, shape = RoundedCornerShape(14.dp),
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
                Text(state.location.ifBlank { stringResource(R.string.jobs_location_anywhere) }, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                    color = AscendColors.Ink, modifier = Modifier.weight(1f))
                FilterButton(state.filters.activeCount) { showFilters = true }
            }
        }

        when (val st = state.status) {
            Resource.Loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) } }
            is Resource.Error ->
                if (st.rateLimited) item { RateLimitState() }
                else item { ErrorState(st.messageRes?.let { stringResource(it) } ?: st.message, onRetry = vm::search) }
            is Resource.Success -> {
                if (state.jobs.isEmpty()) item {
                    EmptyState(hasFilters = state.filters.activeCount > 0, onClear = { vm.setFilters(JobFilters()) })
                }
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
                        Text(stringResource(R.string.jobs_end_reached, state.jobs.size), Modifier.fillMaxWidth().padding(14.dp),
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
            Text(stringResource(R.string.jobs_filters), fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = AscendColors.Indigo)
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
                Text(stringResource(R.string.jobs_filters), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                TextButton(onClick = { draft = JobFilters() }) { Text(stringResource(R.string.jobs_reset), color = AscendColors.Indigo) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.jobs_remote_only), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                Switch(checked = draft.remoteOnly, onCheckedChange = { draft = draft.copy(remoteOnly = it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = AscendColors.Indigo))
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.jobs_date_posted))
            ChipRow(
                listOf(
                    "all" to stringResource(R.string.jobs_date_any),
                    "today" to stringResource(R.string.jobs_date_today),
                    "3days" to stringResource(R.string.jobs_date_3days),
                    "week" to stringResource(R.string.jobs_date_week),
                    "month" to stringResource(R.string.jobs_date_month),
                ),
                selected = setOf(draft.datePosted),
            ) { draft = draft.copy(datePosted = it) }
            Spacer(Modifier.height(14.dp))
            SectionLabel(stringResource(R.string.jobs_employment_type))
            ChipRow(
                listOf(
                    "FULLTIME" to stringResource(R.string.jobs_emp_fulltime),
                    "PARTTIME" to stringResource(R.string.jobs_emp_parttime),
                    "CONTRACTOR" to stringResource(R.string.jobs_emp_contract),
                    "INTERN" to stringResource(R.string.jobs_emp_internship),
                ),
                selected = draft.employmentTypes,
            ) { key -> draft = draft.copy(employmentTypes = draft.employmentTypes.toggle(key)) }
            Spacer(Modifier.height(20.dp))
            Button(onClick = { onApply(draft) }, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Text(stringResource(R.string.jobs_show_results), fontWeight = FontWeight.ExtraBold)
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
        Text(stringResource(R.string.jobs_rate_limit_title), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.error_rate_limited),
            fontSize = 13.sp, color = AscendColors.Muted2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.jobs_error_title), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 13.sp, color = AscendColors.Muted2)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun EmptyState(hasFilters: Boolean, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.jobs_empty), fontWeight = FontWeight.Bold, color = AscendColors.Ink)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.jobs_empty_hint), fontSize = 13.sp, color = AscendColors.Muted2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (hasFilters) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClear, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) {
                Text(stringResource(R.string.jobs_clear_filters), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── native ad interleaving (every 5 listings, free users only) ───────────────
private sealed interface FeedRow {
    data class JobItem(val job: Job) : FeedRow
    data object Ad : FeedRow
}

private fun buildFeed(jobs: List<Job>, nativeAdAllowed: Boolean, frequency: Int): List<FeedRow> = buildList {
    val n = frequency.coerceAtLeast(2)
    jobs.forEachIndexed { i, job ->
        add(FeedRow.JobItem(job))
        // Spec: first ad after the 4th organic row, then one every N rows. Collapse
        // (insert nothing) when the placement is suppressed — never a blank slot.
        val placed = i + 1
        if (nativeAdAllowed && placed >= 4 && (placed - 4) % n == 0 && i != jobs.lastIndex) add(FeedRow.Ad)
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
                Text(stringResource(R.string.jobs_ad_sponsored), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                Text(stringResource(R.string.jobs_ad_placeholder), fontSize = 13.sp, color = AscendColors.Muted)
            }
            Text(stringResource(R.string.jobs_ad_badge), color = Color(0xFFB0A06A), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFFBF4D9)).padding(horizontal = 7.dp, vertical = 2.dp))
        }
    }
}
