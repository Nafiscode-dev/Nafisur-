package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = DeepBackground,
    secondary = AccentOrange,
    onSecondary = DeepBackground,
    tertiary = AccentPink,
    background = DeepBackground,
    onBackground = TextPrimaryLight,
    surface = DeepSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceHover,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We force dark theme to look like the iconic ChatGPT Dark Style
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
