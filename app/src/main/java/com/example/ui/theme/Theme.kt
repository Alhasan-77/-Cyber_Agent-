package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeminiStudioColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = GeminiPillSelectedContainer,
    onPrimaryContainer = GeminiOnPillSelectedContainer,
    secondary = CyberViolet,
    onSecondary = Color(0xFF281344),
    secondaryContainer = GeminiPillSelectedContainer,
    onSecondaryContainer = GeminiOnPillSelectedContainer,
    tertiary = CyberEmerald,
    onTertiary = Color(0xFF0A3818),
    background = CyberBackground,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = CyberTextSecondary,
    surfaceContainerLowest = Color(0xFF0E0E0F),
    surfaceContainerLow = Color(0xFF18191A),
    surfaceContainer = CyberSurface,
    surfaceContainerHigh = CyberSurfaceElevated,
    surfaceContainerHighest = CyberSurfaceHigh,
    outline = CyberBorder,
    outlineVariant = Color(0xFF2D3033),
    error = CyberCoral,
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GeminiStudioColorScheme,
        typography = Typography,
        content = content
    )
}
