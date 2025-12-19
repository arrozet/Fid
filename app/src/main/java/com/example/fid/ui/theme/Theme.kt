package com.example.fid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FidColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = DarkBackground,
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = TextPrimary,
    
    secondary = PrimaryGreenLight,
    onSecondary = DarkBackground,
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun FidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FidColorScheme,
        typography = Typography,
        content = content
    )
}
