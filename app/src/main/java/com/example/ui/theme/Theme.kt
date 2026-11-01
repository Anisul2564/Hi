package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BdPartyDarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.White,
    primaryContainer = DeepViolet,
    onPrimaryContainer = Color.White,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color.White,
    tertiary = BrightGold,
    onTertiary = Color.Black,
    tertiaryContainer = GoldDark,
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3B285E),
    outlineVariant = Color(0xFF261942),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // BD PARTY is a luxury dark-mode entertainment experience
    dynamicColor: Boolean = false, // Preserve brand identity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BdPartyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
