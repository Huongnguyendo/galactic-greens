package com.doquynhhuong.project.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GalacticGreensColorScheme = darkColorScheme(
    primary          = BRPrimary,
    onPrimary        = BROnPrimary,
    primaryContainer = BRPrimaryLight,
    onPrimaryContainer = Color(0xFFFFD9E5),
    secondary        = BRSecondary,
    onSecondary      = Color.White,
    background       = BRBackground,
    onBackground     = BROnBackground,
    surface          = BRSurface,
    onSurface        = BROnBackground,
    surfaceVariant   = BRSurfaceVariant,
    onSurfaceVariant = BRSubtext,
    error            = Color(0xFFFF6B81),
    errorContainer   = Color(0xFF4A1725),
    onErrorContainer = Color(0xFFFFD9E2),
    onError          = Color.White
)

@Composable
fun ProjectTheme(
    content: @Composable () -> Unit
) {
    // Single fixed brand scheme — avoids Android 12+ dynamic color extraction on every frame.
    MaterialTheme(
        colorScheme = GalacticGreensColorScheme,
        typography = Typography,
        content = content
    )
}
