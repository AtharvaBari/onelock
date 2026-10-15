package com.onelock.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OneUIAccent,
    onPrimary = Color.White,
    primaryContainer = OneUIAccentDark,
    onPrimaryContainer = Color.White,
    secondary = OneUIAccent,
    onSecondary = Color.White,
    background = OneUIBackground,
    onBackground = OneUITextPrimary,
    surface = OneUISurfaceCard,
    onSurface = OneUITextPrimary,
    surfaceVariant = OneUISearchBar,
    onSurfaceVariant = OneUITextSecondary,
    outline = OneUIDivider,
    error = OneUIDanger,
    onError = Color.White
)

@Composable
fun OneLockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
