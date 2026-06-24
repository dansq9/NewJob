package app.ascend.ui.monetization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import app.ascend.monetization.AdFlow

/**
 * Marks [flow] active for as long as this composable is in the composition, so the
 * app-open ad is suppressed while the user is inside a sensitive flow (resume /
 * mock / copilot / billing / legal — spec `suppress_during_*`). The flag is cleared
 * on dispose, including when the screen is left or the process is recreated.
 */
@Composable
fun SuppressAppOpenWhileActive(flow: AdFlow) {
    val manager = rememberMonetizationManager()
    DisposableEffect(flow) {
        manager.enterFlow(flow)
        onDispose { manager.exitFlow(flow) }
    }
}
