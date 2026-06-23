package app.ascend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono

@Composable
fun Pill(
    text: String,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    dot: Color? = null,
    mono: Boolean = false,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot)); Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            color = fg,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) JetBrainsMono else FontFamily.Default,
        )
    }
}

@Composable
fun MatchBadge(percent: Int, modifier: Modifier = Modifier) {
    Pill("$percent%", AscendColors.Green, AscendColors.GreenBg, modifier, mono = true)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = AscendColors.Muted2,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = JetBrainsMono,
    )
}
