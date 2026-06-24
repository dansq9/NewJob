package app.ascend.games.engine.games.common

import androidx.compose.ui.graphics.Color

/** Colors lifted from the Brain Games design mock. Android system font is the default. */
object Brain {
    val Blue = Color(0xFF14479E)
    val BlueSoft = Color(0xFFEEF2FB)
    val Page = Color(0xFFF2F3F5)
    val Card = Color(0xFFFFFFFF)
    val Border = Color(0xFFE7E9EC)
    val BorderStrong = Color(0xFFB9BDC4)
    val Ink = Color(0xFF16181C)
    val InkSoft = Color(0xFF3C4148)
    val Muted = Color(0xFF9AA0A8)
    val Chip = Color(0xFFEEF0F3)
    val ChipInk = Color(0xFF5B626B)
    val Green = Color(0xFF1E8E3E)
    val GreenBg = Color(0xFFE7F4EC)
    val GreenBd = Color(0xFFCFE6D6)
    val Red = Color(0xFFD6402C)
    val NewRed = Color(0xFFE8484F)
    val Gold = Color(0xFFF0902A)
    val Sel = Color(0xFFEEF2FB)
    val Sun = Color(0xFFF0A93A)
    val Moon = Color(0xFF3A4252)
    val Scrim = Color(0x730F121C)
}

fun formatTime(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
