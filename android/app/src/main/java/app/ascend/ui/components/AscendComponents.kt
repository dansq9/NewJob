package app.ascend.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.ui.theme.AscendColors
import app.ascend.ui.theme.JetBrainsMono

/**
 * Shared design-system primitives mirrored from Ascend.dc.html. Screens compose these
 * instead of raw Material defaults so the app matches the prototype's cards/buttons/headers.
 * Tokens: bg #f5f5f8, card #fff, border #ececf2, primary #4f46e5, violet #7c5cff,
 * ink #15151c, muted #6b6b78 / #9a9aa6 (all in [AscendColors]).
 */

/** Circular back button — 40dp white circle, hairline border, back chevron (prototype). */
@Composable
fun AscendBackCircle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = AscendColors.Ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** Screen header: optional back circle, title (+ optional subtitle), optional trailing actions. */
@Composable
fun AscendScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) { AscendBackCircle(onBack); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 13.sp, color = AscendColors.Muted2, lineHeight = 18.sp)
            }
        }
        actions()
    }
}

/** Editable search box card — 50dp tall, 14dp radius, search icon, optional submit arrow. */
@Composable
fun AscendSearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
) {
    Surface(
        modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(14.dp),
        color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = AscendColors.Muted2, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = AscendColors.Faint, fontSize = 15.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = AscendColors.Ink),
                    cursorBrush = SolidColor(AscendColors.Indigo),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (onSubmit != null && value.isNotEmpty()) {
                Icon(
                    Icons.Outlined.ArrowCircleRight, "Search", tint = AscendColors.Indigo,
                    modifier = Modifier.size(24.dp).clickable(onClick = onSubmit),
                )
            }
        }
    }
}

/** Primary CTA — 54dp, 16dp radius, white bold label; solid indigo or indigo→violet gradient. */
@Composable
fun AscendPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    gradient: Boolean = false,
) {
    val shape = RoundedCornerShape(16.dp)
    val fill: Brush = when {
        !enabled -> SolidColor(AscendColors.Indigo.copy(alpha = 0.4f))
        gradient -> Brush.linearGradient(listOf(AscendColors.Indigo, AscendColors.Violet2))
        else -> SolidColor(AscendColors.Indigo)
    }
    Box(
        modifier.fillMaxWidth().height(54.dp).clip(shape).background(fill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, tint = Color.White); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** White card with hairline border + rounded corners; padded content slot. */
@Composable
fun AscendCard(
    modifier: Modifier = Modifier,
    radius: Int = 18,
    padding: Int = 16,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius.dp)
    var base = modifier.fillMaxWidth().clip(shape).background(AscendColors.Card).border(BorderStroke(1.5.dp, AscendColors.Line), shape)
    if (onClick != null) base = base.clickable(onClick = onClick)
    Column(base.padding(padding.dp), content = content)
}

/** Rounded icon badge — tinted square with an icon (the 42–56dp box used across the prototype). */
@Composable
fun AscendIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = AscendColors.Indigo,
    bg: Color = tint.copy(alpha = 0.14f),
    size: Int = 42,
    radius: Int = 13,
) {
    Box(modifier.size(size.dp).clip(RoundedCornerShape(radius.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size((size * 0.52f).dp))
    }
}

/** Gradient action tile (home quick-actions) — icon + label + sub over a gradient. */
@Composable
fun AscendActionTile(
    label: String,
    sub: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier.height(120.dp).clip(RoundedCornerShape(20.dp)).background(gradient).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White) }
            Column {
                Text(label, fontSize = 15.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 18.sp)
                Spacer(Modifier.height(3.dp))
                Text(sub, fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 15.sp)
            }
        }
    }
}

/** Pipeline stage chip with a colored dot. */
@Composable
fun AscendStageChip(label: String, color: Color, modifier: Modifier = Modifier, selected: Boolean = false) {
    Row(
        modifier.clip(RoundedCornerShape(999.dp))
            .background(if (selected) color.copy(alpha = 0.16f) else AscendColors.Line2)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (selected) color else AscendColors.Muted)
    }
}

/** Small stat card — big mono value + caption. */
@Composable
fun AscendStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape).background(AscendColors.Card).border(BorderStroke(1.5.dp, AscendColors.Line), shape)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, fontFamily = JetBrainsMono)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.5.sp, color = AscendColors.Muted2)
    }
}

/** Empty / illustrative state — icon badge, title, subtitle, optional CTA. */
@Composable
fun AscendEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AscendIconBadge(icon, size = 56, radius = 18, tint = AscendColors.Indigo)
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 13.sp, color = AscendColors.Muted2, textAlign = TextAlign.Center, lineHeight = 19.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            AscendPrimaryButton(actionLabel, onAction)
        }
    }
}

/** App-styled confirmation dialog. */
@Composable
fun AscendConfirmationDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = AscendColors.Indigo, fontWeight = FontWeight.Bold) } },
        dismissButton = dismissLabel?.let { lbl -> { TextButton(onClick = onDismiss) { Text(lbl, color = AscendColors.Muted) } } },
        title = { Text(title, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink) },
        text = { Text(text, color = AscendColors.Muted, fontSize = 14.sp, lineHeight = 20.sp) },
        containerColor = AscendColors.Card,
        shape = RoundedCornerShape(20.dp),
    )
}

/** Bottom sheet with prototype styling (rounded top, card surface, side padding). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscendBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AscendColors.Card,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), content = content)
    }
}
