package app.ascend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.data.model.Job
import app.ascend.data.model.WorkType
import app.ascend.ui.theme.AscendColors
import coil.compose.AsyncImage

private val avatarPalette = listOf(
    AscendColors.Indigo, AscendColors.Green, Color(0xFFE0913F),
    Color(0xFFD6457A), AscendColors.Violet2, Color(0xFF2AA7B8),
)

@Composable
fun CompanyAvatar(company: String, logoUrl: String?, size: Int = 46) {
    val shape = RoundedCornerShape((size * 0.28).dp)
    if (logoUrl != null) {
        AsyncImage(
            model = logoUrl, contentDescription = null,
            modifier = Modifier.size(size.dp).clip(shape),
        )
    } else {
        val color = avatarPalette[(company.firstOrNull()?.code ?: 0) % avatarPalette.size]
        Box(Modifier.size(size.dp).clip(shape).then(Modifier), contentAlignment = Alignment.Center) {
            Surface(color = color, shape = shape, modifier = Modifier.matchParentSize()) {}
            Text(
                company.firstOrNull()?.uppercase() ?: "?",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size * 0.38).sp,
            )
        }
    }
}

private fun WorkType.color() = when (this) {
    WorkType.REMOTE -> AscendColors.Green
    WorkType.HYBRID -> Color(0xFFE0913F)
    WorkType.ONSITE -> AscendColors.StageApplied
    WorkType.UNKNOWN -> AscendColors.Muted
}

@Composable
fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CompanyAvatar(job.company, job.logoUrl)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(job.title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold,
                        color = AscendColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("${job.company} · ${job.location}", fontSize = 13.sp, color = AscendColors.Muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (onToggleSave != null) {
                    IconButton(onClick = onToggleSave, modifier = Modifier.size(34.dp)) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (saved) AscendColors.Indigo else AscendColors.Faint,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (job.workType != WorkType.UNKNOWN)
                    Pill(job.workType.label, job.workType.color(), AscendColors.Bg, dot = job.workType.color())
                job.employmentType?.let { Pill(it, AscendColors.Muted, AscendColors.Bg) }
                job.salary?.let { Pill(it, AscendColors.Muted, AscendColors.Bg) }
            }
            if (job.postedAgo != null || job.matchPercent != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = AscendColors.Line2)
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    job.postedAgo?.let {
                        Icon(Icons.Outlined.Schedule, null, tint = AscendColors.Muted2, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(it, fontSize = 12.sp, color = AscendColors.Muted2)
                    }
                    Spacer(Modifier.weight(1f))
                    job.matchPercent?.let { MatchBadge(it) }
                }
            }
        }
    }
}
