package com.webunime.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette mendekati Wibuku (dark + ungu nav + biru aksen). */
object WuColors {
    val Bg = Color(0xFF121212)
    val Surface = Color(0xFF1E1F22)
    val SurfaceAlt = Color(0xFF2B2C2F)
    val NavBar = Color(0xFF17181A)
    val NavActive = Color(0xFF5A5576)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFFA0A0A1)
    val Link = Color(0xFF5AB5FF)
    val AccentBlue = Color(0xFF64B5F6)
    val AccentYellow = Color(0xFFFFB300)
    val AccentRed = Color(0xFFE53935)
    val NewBadge = Color(0xFF1E88E5)
    val Progress = Color(0xFFE53935)
}

private val scheme = darkColorScheme(
    primary = WuColors.AccentBlue,
    onPrimary = WuColors.Text,
    background = WuColors.Bg,
    onBackground = WuColors.Text,
    surface = WuColors.Surface,
    onSurface = WuColors.Text,
    onSurfaceVariant = WuColors.Muted,
    secondary = WuColors.NavActive,
    tertiary = WuColors.AccentYellow,
)

@Composable
fun WebunimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
