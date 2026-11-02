package com.example.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Authentic Google Gemini App Dark Mode Palette
val CyberBackground = Color(0xFF131314)       // Gemini Obsidian Primary Background
val CyberSurface = Color(0xFF1E1F20)          // Gemini Surface Container (Cards, Prompt Bar, Nav Bar)
val CyberSurfaceElevated = Color(0xFF282A2C)  // Gemini Elevated Container (User Bubble, Active Cards)
val CyberSurfaceHigh = Color(0xFF333537)      // Gemini High Container (Chips, Hover States)

val CyberCyan = Color(0xFF8AB4F8)             // Google / Gemini Soft Blue Primary Accent
val CyberCyanDark = Color(0xFF4285F4)         // Google Core Brand Blue
val CyberViolet = Color(0xFFC58AF9)           // Gemini Sparkle Purple / Violet
val CyberEmerald = Color(0xFF81C995)          // Google Mint Green
val CyberAmber = Color(0xFFFDD663)            // Google Warm Gold
val CyberCoral = Color(0xFFF28B82)            // Gemini Coral / Rose

val CyberTextPrimary = Color(0xFFE3E3E3)      // Gemini Primary OnSurface Text
val CyberTextSecondary = Color(0xFFC4C7C5)    // Gemini Secondary OnSurfaceVariant Text
val CyberBorder = Color(0xFF3C4043)           // Gemini Subtle Hairline Outline

// Gemini Material 3 Selection & Pill Containers
val GeminiPillSelectedContainer = Color(0xFF004A77)
val GeminiOnPillSelectedContainer = Color(0xFFD3E3FD)
val GeminiVioletContainer = Color(0xFF3B2654)
val GeminiRoseAccent = Color(0xFFD96570)

// Signature Google Gemini Gradients
val GeminiSparkleColors = listOf(
    Color(0xFF4285F4), // Google Blue
    Color(0xFF9B72CB), // Gemini Purple
    Color(0xFFD96570)  // Gemini Coral Rose
)

val GeminiSparkleGradient = Brush.linearGradient(
    colors = GeminiSparkleColors,
    start = Offset(0f, 0f),
    end = Offset(700f, 240f)
)

val GeminiSubtleCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1E1F20),
        Color(0xFF24262B)
    )
)

val GeminiBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8AB4F8).copy(alpha = 0.55f),
        Color(0xFFC58AF9).copy(alpha = 0.45f),
        Color(0xFFD96570).copy(alpha = 0.40f)
    )
)
