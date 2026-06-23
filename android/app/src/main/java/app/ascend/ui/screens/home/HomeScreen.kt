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
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class QuickAction(val label: String, val sub: String, val icon: ImageVector, val route: String, val accent: Color)

@Composable
fun HomeScreen(nav: NavController, vm: HomeViewModel = hiltViewModel()) {
    val matches by vm.topMatches.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val firstName = profile.name.trim().substringBefore(' ').ifBlank { "there" }
    val actions = listOf(
        QuickAction("Optimize resume", "Beat the ATS", Icons.Outlined.AutoFixHigh, Routes.RESUME, AscendColors.Indigo),
        QuickAction("Mock interview", "Practice & get scored", Icons.Outlined.RecordVoiceOver, Routes.MOCK, AscendColors.Green),
        QuickAction("Live Copilot", "Real-time answers", Icons.Outlined.Bolt, Routes.COPILOT, AscendColors.Violet2),
        QuickAction("Brain Games", "Sharpen up daily", Icons.Outlined.Extension, Routes.GAMES, Color(0xFFE0913F)),
    )

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Good morning", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Muted2)
                    Text(firstName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
                }
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(AscendColors.Indigo),
                    contentAlignment = Alignment.Center,
                ) { Text(profile.name.firstOrNull()?.uppercase() ?: "A", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            Surface(
                onClick = { nav.navigate(Routes.JOBS) },
                shape = RoundedCornerShape(16.dp), color = AscendColors.Card,
                border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2)
                    Spacer(Modifier.width(11.dp))
                    Text("Search jobs, titles, companies", color = Color(0xFFB3B3BD), fontSize = 15.sp)
                }
            }
        }
        item { Text("Quick actions", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink) }
        items(actions.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { qa -> QuickActionCard(qa, Modifier.weight(1f)) { nav.navigate(qa.route) } }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Text("Top matches", style = MaterialTheme.typography.titleLarge, color = AscendColors.Ink, modifier = Modifier.weight(1f))
                Text("See all", color = AscendColors.Indigo, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier.clickable { nav.navigate(Routes.JOBS) })
            }
        }
        item {
            val sub = listOf(profile.targetRole, profile.location).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotBlank()) Text("for $sub", fontSize = 13.sp, color = AscendColors.Muted2)
        }

        when (val m = matches) {
            is Resource.Loading -> item { Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) { CircularProgressIndicator(color = AscendColors.Indigo) } }
            is Resource.Error -> item { Text(m.message, color = AscendColors.Muted2, fontSize = 13.sp) }
            is Resource.Success -> items(m.data, key = { it.id }) { job ->
                JobCard(job = job, onClick = { vm.select(job); nav.navigate(Routes.JOB_DETAIL) })
            }
        }
    }
}

@Composable
private fun QuickActionCard(qa: QuickAction, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp), color = AscendColors.Card, border = BorderStroke(1.dp, AscendColors.Line),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(qa.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) { Icon(qa.icon, null, tint = qa.accent) }
            Column {
                Text(qa.label, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                Text(qa.sub, fontSize = 11.5.sp, color = AscendColors.Muted2)
            }
        }
    }
}
