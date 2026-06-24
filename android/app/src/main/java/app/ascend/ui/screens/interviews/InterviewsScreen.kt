package app.ascend.ui.screens.interviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ascend.R
import app.ascend.ui.navigation.Routes
import app.ascend.ui.theme.AscendColors

@Composable
fun InterviewsScreen(nav: NavController) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.interviews_title), style = MaterialTheme.typography.headlineMedium, color = AscendColors.Ink)
        Text(stringResource(R.string.interviews_subtitle), fontSize = 14.sp, color = AscendColors.Muted)
        Spacer(Modifier.height(2.dp))
        InterviewCard(
            stringResource(R.string.interviews_mock_title), stringResource(R.string.interviews_mock_sub),
            Icons.Outlined.RecordVoiceOver, AscendColors.Indigo,
        ) { nav.navigate(Routes.MOCK) }
        InterviewCard(
            stringResource(R.string.interviews_copilot_title), stringResource(R.string.interviews_copilot_sub),
            Icons.Outlined.Bolt, AscendColors.Violet2,
        ) { nav.navigate(Routes.COPILOT) }
    }
}

@Composable
private fun InterviewCard(title: String, sub: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = AscendColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, AscendColors.Line), modifier = Modifier.fillMaxWidth()) {
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
