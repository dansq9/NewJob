package app.ascend.ui.screens.interviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.ui.components.AscendEmptyState
import app.ascend.ui.components.SectionLabel
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono

@Composable
fun InterviewsScreen(nav: NavController) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.interviews_title), style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
        Text(stringResource(R.string.interviews_subtitle), fontSize = 14.sp, color = AscendColors.Muted)
        Spacer(Modifier.height(2.dp))

        // Dark Copilot hero (prototype: live AI copilot, dark surface).
        CopilotHero(onLaunch = { nav.navigate(Routes.COPILOT) })

        // Mock interview card.
        InterviewCard(
            stringResource(R.string.interviews_mock_title), stringResource(R.string.interviews_mock_sub),
            Icons.Outlined.RecordVoiceOver, AscendColors.Indigo,
        ) { nav.navigate(Routes.MOCK) }

        Spacer(Modifier.height(2.dp))
        SectionLabel(stringResource(R.string.interviews_recent))
        Surface(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = AscendColors.Card,
            border = BorderStroke(1.5.dp, AscendColors.Line),
        ) {
            AscendEmptyState(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.interviews_recent),
                subtitle = stringResource(R.string.interviews_recent_empty),
            )
        }
    }
}

@Composable
private fun CopilotHero(onLaunch: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(AscendColors.Dark, Color(0xFF262636))))
            .padding(22.dp),
    ) {
        Column {
            Text(
                stringResource(R.string.interviews_copilot_title).uppercase(),
                color = Color(0xFFA89BFF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMono, letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.interviews_copilot_sub), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(AscendColors.Violet2).clickable(onClick = onLaunch)
                    .padding(horizontal = 20.dp, vertical = 13.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.interviews_launch_copilot), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun InterviewCard(title: String, sub: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
                Text(sub, fontSize = 12.5.sp, color = AscendColors.Muted2)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AscendColors.Faint)
        }
    }
}
