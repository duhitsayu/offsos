package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TacticalDayColorScheme = lightColorScheme(
    primary = TacticalInkDark,
    onPrimary = Color.White,
    primaryContainer = TacticalSurfaceAlt,
    onPrimaryContainer = TacticalInkDark,
    secondary = TacticalMuted,
    onSecondary = Color.White,
    secondaryContainer = TacticalSurfaceAlt,
    onSecondaryContainer = TacticalInkDark,
    tertiary = TacticalAmber,
    onTertiary = Color.White,
    background = TacticalSlateBg,
    onBackground = TacticalInkDark,
    surface = TacticalCardBg,
    onSurface = TacticalInkDark,
    surfaceVariant = TacticalSurfaceAlt,
    onSurfaceVariant = TacticalMuted,
    outline = TacticalBorder,
    error = TacticalAlertRed,
    onError = Color.White
)

val TacticalNightColorScheme = darkColorScheme(
    primary = NightVisionAccentAmber,
    onPrimary = Color.Black,
    primaryContainer = NightVisionCardBg,
    onPrimaryContainer = NightVisionTextPrimary,
    secondary = NightVisionTextMuted,
    onSecondary = Color.Black,
    background = NightVisionBg,
    onBackground = NightVisionTextPrimary,
    surface = NightVisionCardBg,
    onSurface = NightVisionTextPrimary,
    surfaceVariant = Color(0xFF1B2428),
    onSurfaceVariant = NightVisionTextMuted,
    outline = NightVisionBorder,
    error = NightVisionAlertRed,
    onError = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalNightColorScheme else TacticalDayColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
