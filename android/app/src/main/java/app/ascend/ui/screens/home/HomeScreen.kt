package app.ascend.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import app.ascend.core.Resource
import app.ascend.monetization.Placement
import app.ascend.ui.components.JobCard
import app.ascend.ui.monetization.NativeAdSlot
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

/** A home quick-action tile: gradient background, white content (mirrors the prototype). */
private data class QuickAction(
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val route: String,
    val gradient: Brush,
)

/** Time-of-day greeting (desugared java.time). */
@Composable
private fun greeting(): String = stringResource(
    when (java.time.LocalTime.now().hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..16 -> R.string.home_greeting_afternoon
        in 17..21 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_default
    }
)

@Composable
fun HomeScreen(nav: NavController, vm: HomeViewModel = hiltViewModel()) {
    val matches by vm.topMatches.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val firstName = profile.name.trim().substringBefore(' ').ifBlank { stringResource(R.string.home_fallback_name) }
    // Prototype order: Resume, Copilot, Mock, Games — each a gradient tile.
    val actions = listOf(
        QuickAction(stringResource(R.string.home_qa_resume), stringResource(R.string.home_qa_resume_sub), Icons.Outlined.AutoFixHigh, Routes.RESUME,
            Brush.linearGradient(listOf(AscendColors.Indigo, AscendColors.Violet2))),
        QuickAction(stringResource(R.string.home_qa_copilot), stringResource(R.string.home_qa_copilot_sub), Icons.Outlined.Bolt, Routes.COPILOT,
            Brush.linearGradient(listOf(AscendColors.Violet, AscendColors.Indigo2))),
        QuickAction(stringResource(R.string.home_qa_mock), stringResource(R.string.home_qa_mock_sub), Icons.Outlined.RecordVoiceOver, Routes.MOCK,
            Brush.linearGradient(listOf(AscendColors.Green, Color(0xFF12B981)))),
        QuickAction(stringResource(R.string.home_qa_games), stringResource(R.string.home_qa_games_sub), Icons.Outlined.Extension, Routes.GAMES,
            Brush.linearGradient(listOf(Color(0xFFE0913F), Color(0xFFF2B24C)))),
    )

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Muted2)
                    Spacer(Modifier.height(2.dp))
                    Text(firstName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
                }
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color(0xFF5C5C6B), modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(AscendColors.Indigo)
                        .clickable { nav.navigate(Routes.SETTINGS) },
                    contentAlignment = Alignment.Center,
                ) { Text(profile.name.firstOrNull()?.uppercase() ?: "A", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Surface(
                onClick = { nav.navigate(Routes.JOBS) },
                shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
                border = BorderStroke(1.5.dp, AscendColors.Line),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Row(Modifier.height(52.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2)
                    Spacer(Modifier.width(11.dp))
                    Text(stringResource(R.string.home_search_hint), color = Color(0xFFB3B3BD), fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ArrowCircleRight, null, tint = AscendColors.Indigo, modifier = Modifier.size(24.dp))
                }
            }
        }
        item { Text(stringResource(R.string.home_quick_actions), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, modifier = Modifier.padding(top = 10.dp)) }
        items(actions.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { qa -> QuickActionCard(qa, Modifier.weight(1f)) { nav.navigate(qa.route) } }
            }
        }
        item { NativeAdSlot(placement = Placement.NATIVE_HOME_MID, modifier = Modifier.fillMaxWidth()) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Text(stringResource(R.string.home_top_matches), style = MaterialTheme.typography.titleLarge, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.home_see_all), color = AscendColors.Indigo, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clickable { nav.navigate(Routes.JOBS) })
            }
        }
        item {
            val sub = listOf(profile.targetRole, profile.location).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) Text(stringResource(R.string.home_matches_for, sub), fontSize = 13.sp, color = AscendColors.Muted2)
        }

        when (val m = matches) {
            is Resource.Loading -> item { Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) } }
            is Resource.Error -> item { MatchesMessage(m.messageRes?.let { stringResource(it) } ?: m.message, onRetry = vm::retry) }
            is Resource.Success ->
                if (m.data.isEmpty()) {
                    item { MatchesMessage(stringResource(R.string.home_no_matches), onRetry = null, action = stringResource(R.string.home_find_jobs)) { nav.navigate(Routes.JOBS) } }
                } else items(m.data, key = { it.id }) { job ->
                    JobCard(job = job, onClick = { vm.select(job); nav.navigate(Routes.jobDetail(job.id)) })
                }
        }
    }
}

@Composable
private fun MatchesMessage(message: String, onRetry: (() -> Unit)?, action: String? = null, onAction: (() -> Unit)? = null) {
    Surface(shape = RoundedCornerShape(16.dp), color = AscendColors.Card, border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = AscendColors.Muted2, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 18.sp)
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold) }
            } else if (action != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo)) { Text(action, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun QuickActionCard(qa: QuickAction, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(qa.gradient)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(qa.icon, null, tint = Color.White) }
            Column {
                Text(qa.label, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 18.sp)
                Spacer(Modifier.height(3.dp))
                Text(qa.sub, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 15.sp)
            }
        }
    }
}
