package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaForgeDarkScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFFB8F5FF),
    secondary = CyberViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D1B69),
    onSecondaryContainer = Color(0xFFE4D9FF),
    tertiary = CyberEmerald,
    onTertiary = Color(0xFF00210E),
    background = CyberBackground,
    onBackground = CyberTextPrimary,
    surface = CyberSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = CyberTextSecondary,
    error = CyberCoral,
    onError = Color.White,
    outline = CyberBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovaForgeDarkScheme,
        typography = Typography,
        content = content
    )
}
