package com.gastotrack.app.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = PixelLightPrimary,
    onPrimary = PixelLightOnPrimary,
    primaryContainer = PixelLightPrimaryContainer,
    onPrimaryContainer = PixelLightOnPrimaryContainer,
    secondary = PixelLightSecondary,
    onSecondary = PixelLightOnSecondary,
    secondaryContainer = PixelLightSecondaryContainer,
    onSecondaryContainer = PixelLightOnSecondaryContainer,
    tertiary = PixelLightTertiary,
    onTertiary = PixelLightOnTertiary,
    tertiaryContainer = PixelLightTertiaryContainer,
    onTertiaryContainer = PixelLightOnTertiaryContainer,
    error = PixelLightError,
    onError = PixelLightOnError,
    errorContainer = PixelLightErrorContainer,
    onErrorContainer = PixelLightOnErrorContainer,
    background = PixelLightBackground,
    onBackground = PixelLightOnBackground,
    surface = PixelLightSurface,
    onSurface = PixelLightOnSurface,
    surfaceVariant = PixelLightSurfaceVariant,
    onSurfaceVariant = PixelLightOnSurfaceVariant,
    outline = PixelLightOutline,
    outlineVariant = PixelLightOutlineVariant,
    surfaceContainerLowest = PixelLightSurfaceContainerLowest,
    surfaceContainerLow = PixelLightSurfaceContainerLow,
    surfaceContainer = PixelLightSurfaceContainer,
    surfaceContainerHigh = PixelLightSurfaceContainerHigh,
    surfaceContainerHighest = PixelLightSurfaceContainerHighest
)

private val DarkColorScheme = darkColorScheme(
    primary = PixelDarkPrimary,
    onPrimary = PixelDarkOnPrimary,
    primaryContainer = PixelDarkPrimaryContainer,
    onPrimaryContainer = PixelDarkOnPrimaryContainer,
    secondary = PixelDarkSecondary,
    onSecondary = PixelDarkOnSecondary,
    secondaryContainer = PixelDarkSecondaryContainer,
    onSecondaryContainer = PixelDarkOnSecondaryContainer,
    tertiary = PixelDarkTertiary,
    onTertiary = PixelDarkOnTertiary,
    tertiaryContainer = PixelDarkTertiaryContainer,
    onTertiaryContainer = PixelDarkOnTertiaryContainer,
    error = PixelDarkError,
    onError = PixelDarkOnError,
    errorContainer = PixelDarkErrorContainer,
    onErrorContainer = PixelDarkOnErrorContainer,
    background = PixelDarkBackground,
    onBackground = PixelDarkOnBackground,
    surface = PixelDarkSurface,
    onSurface = PixelDarkOnSurface,
    surfaceVariant = PixelDarkSurfaceVariant,
    onSurfaceVariant = PixelDarkOnSurfaceVariant,
    outline = PixelDarkOutline,
    outlineVariant = PixelDarkOutlineVariant,
    surfaceContainerLowest = PixelDarkSurfaceContainerLowest,
    surfaceContainerLow = PixelDarkSurfaceContainerLow,
    surfaceContainer = PixelDarkSurfaceContainer,
    surfaceContainerHigh = PixelDarkSurfaceContainerHigh,
    surfaceContainerHighest = PixelDarkSurfaceContainerHighest
)

@Composable
fun GastoTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You Dynamic Color: adapta los colores al fondo de pantalla en Android 12+ (Google Pixel)
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
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
