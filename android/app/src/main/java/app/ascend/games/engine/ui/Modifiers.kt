package app.ascend.games.engine.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Clickable with no ripple/indication — handy for icons and custom-drawn cells. */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember(),
        indication = null,
        onClick = onClick
    )
}

@Composable
private fun remember(): MutableInteractionSource =
    androidx.compose.runtime.remember { MutableInteractionSource() }
