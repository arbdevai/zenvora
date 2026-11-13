package com.zenvora.gesture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TrueNeutralOledColorScheme = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF242424),
    onSecondaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF242424),
    error = Color(0xFFCF6679),
    onError = Color(0xFF121212)
)

@Composable
fun ZenvoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TrueNeutralOledColorScheme,
        content = content
    )
}
