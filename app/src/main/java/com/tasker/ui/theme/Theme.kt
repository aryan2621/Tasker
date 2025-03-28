package com.tasker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tasker.ui.components.AppShapes

// Light theme color scheme
private val LightColorScheme = lightColorScheme(
    primary = NeobankGreen,
    onPrimary = NeobankDark,
    primaryContainer = NeobankGreen.copy(alpha = 0.7f),
    onPrimaryContainer = NeobankDark,

    secondary = NeobankDark,
    onSecondary = NeobankWhite,
    secondaryContainer = NeobankDark.copy(alpha = 0.8f),
    onSecondaryContainer = NeobankWhite,

    tertiary = NeobankLight,
    onTertiary = NeobankDark,
    tertiaryContainer = NeobankLight,
    onTertiaryContainer = NeobankDark,

    background = NeobankWhite,
    onBackground = NeobankDark,
    surface = NeobankWhite,
    onSurface = NeobankDark,

    error = ErrorRed,
    onError = NeobankWhite,

    surfaceVariant = NeobankLight,
    onSurfaceVariant = NeobankDark.copy(alpha = 0.7f),
    outline = NeobankDark.copy(alpha = 0.3f)
)

// Dark theme color scheme
private val DarkColorScheme = darkColorScheme(
    primary = NeobankGreen,
    onPrimary = NeobankDark,
    primaryContainer = NeobankGreen.copy(alpha = 0.5f),
    onPrimaryContainer = NeobankDark,

    secondary = NeobankLight,
    onSecondary = NeobankDark,
    secondaryContainer = NeobankLight.copy(alpha = 0.2f),
    onSecondaryContainer = NeobankLight,

    tertiary = NeobankDark,
    onTertiary = NeobankLight,
    tertiaryContainer = NeobankDark.copy(alpha = 0.7f),
    onTertiaryContainer = NeobankLight,

    background = Color(0xFF121212),
    onBackground = NeobankLight,
    surface = Color(0xFF1E1E1E),
    onSurface = NeobankLight,

    error = ErrorRed,
    onError = NeobankWhite,

    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = NeobankLight.copy(alpha = 0.7f),
    outline = NeobankLight.copy(alpha = 0.3f)
)

@Composable
fun TaskerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}