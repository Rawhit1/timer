package com.intervalpulse.timer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WorkoutEmerald = Color(0xFF10B981)
val WorkoutEmeraldDark = Color(0xFF065F46)
val RestCyan = Color(0xFF06B6D4)
val RestCyanDark = Color(0xFF155E75)
val SurfaceDark = Color(0xFF0F172A)
val CardDark = Color(0xFF1E293B)
val CardBorder = Color(0xFF334155)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

private val DarkColors = darkColorScheme(
    primary = WorkoutEmerald,
    secondary = RestCyan,
    background = SurfaceDark,
    surface = CardDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF273549),
    onSurfaceVariant = TextSecondary
)

@Composable
fun IntervalPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
