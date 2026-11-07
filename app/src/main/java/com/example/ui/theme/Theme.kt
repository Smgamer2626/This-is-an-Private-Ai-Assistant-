package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YuriDarkColorScheme = darkColorScheme(
    primary = YuriCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = YuriBackgroundElevated,
    onPrimaryContainer = YuriCyanPrimary,
    secondary = YuriBluePrimary,
    onSecondary = Color.White,
    secondaryContainer = YuriBackgroundCard,
    onSecondaryContainer = YuriCyanGlow,
    tertiary = YuriStatusOnline,
    onTertiary = Color.Black,
    background = YuriBackgroundDeep,
    onBackground = YuriTextPrimary,
    surface = YuriBackgroundSurface,
    onSurface = YuriTextPrimary,
    surfaceVariant = YuriBackgroundCard,
    onSurfaceVariant = YuriTextSecondary,
    outline = YuriBorderCyan,
    outlineVariant = YuriBorderSubtle,
    error = YuriStatusOffline,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YuriDarkColorScheme,
        typography = Typography,
        content = content
    )
}
