package com.example.civic.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CivicNavy,
    onPrimary = CivicSurface,
    primaryContainer = CivicNavyLight,
    onPrimaryContainer = CivicSurface,
    secondary = CivicMutedBlue,
    onSecondary = CivicSurface,
    secondaryContainer = CivicMutedBlueLight,
    onSecondaryContainer = CivicNavy,
    tertiary = CivicAccentGreen,
    onTertiary = CivicSurface,
    tertiaryContainer = CivicAccentGreenContainer,
    onTertiaryContainer = CivicNavyDark,
    background = CivicBackground,
    onBackground = CivicTextPrimary,
    surface = CivicSurface,
    onSurface = CivicTextPrimary,
    surfaceVariant = CivicSurfaceVariant,
    onSurfaceVariant = CivicTextSecondary,
    error = CivicErrorRed,
    errorContainer = CivicErrorContainer
)

@Composable
fun CivicTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
