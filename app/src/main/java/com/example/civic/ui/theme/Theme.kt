package com.example.civic.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CivicPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E9F8),
    onPrimaryContainer = CivicDarkBlue,
    secondary = CivicDarkBlue,
    onSecondary = Color.White,
    secondaryContainer = CivicSecondaryBg,
    onSecondaryContainer = CivicTextPrimaryDark,
    tertiary = CivicSuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = CivicSuccessContainer,
    onTertiaryContainer = CivicDarkBlue,
    background = CivicPaleBlueBg,
    onBackground = CivicTextPrimaryDark,
    surface = CivicSurfaceWhite,
    onSurface = CivicTextPrimaryDark,
    surfaceVariant = CivicSecondaryBg,
    onSurfaceVariant = CivicTextSecondaryGray,
    outline = CivicSubtleBorder,
    outlineVariant = CivicSubtleBorder,
    error = CivicErrorRedRes,
    errorContainer = CivicErrorContainerRes,
    onError = Color.White,
    onErrorContainer = CivicErrorRedRes
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
