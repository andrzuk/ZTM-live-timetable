package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PolishPrimary,
    onPrimary = PolishOnPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    
    secondary = PolishSecondary,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = Color(0xFFE8DEF8),
    
    tertiary = PolishTertiary,
    onTertiary = Color(0xFF492532),
    tertiaryContainer = PolishTertiaryContainer,
    onTertiaryContainer = Color(0xFFFFD8E4),
    
    background = M3Background,
    onBackground = TextPrimary,
    
    surface = M3Surface,
    onSurface = TextPrimary,
    surfaceVariant = M3SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    
    outline = M3Border,
    outlineVariant = M3BorderSubtle,
    error = AlertRed,
    onError = Color(0xFF601410),
    errorContainer = AlertRedContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce our tailored high-contrast dark transit theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
