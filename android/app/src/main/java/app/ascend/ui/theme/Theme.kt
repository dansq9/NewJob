package app.ascend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AscendLightColors = lightColorScheme(
    primary = AscendColors.Indigo,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = AscendColors.Violet2,
    background = AscendColors.Bg,
    onBackground = AscendColors.Ink,
    surface = AscendColors.Card,
    onSurface = AscendColors.Ink,
    error = AscendColors.Red,
)

@Composable
fun AscendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The prototype is a single light brand theme; keep it consistent across modes.
    MaterialTheme(
        colorScheme = AscendLightColors,
        typography = AscendTypography,
        content = content,
    )
}
