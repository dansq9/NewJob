package com.gamestest.games.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val BrandBlue = Color(0xFF12369E)
val BrandBlueLight = Color(0xFF3C5BC0)
val Gold = Color(0xFFF5A623)
val AppBackground = Color(0xFFF2F4F7)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF101828)
val TextMuted = Color(0xFF667085)

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    secondary = Gold,
    onSecondary = Color.White,
    background = AppBackground,
    onBackground = TextDark,
    surface = CardWhite,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEAECF0),
    onSurfaceVariant = TextMuted,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color.White,
    secondary = Gold,
    background = Color(0xFF0F1424),
    surface = Color(0xFF1A2036),
)

@Composable
fun GamesTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: transparent bars; white headers want dark status icons.
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
