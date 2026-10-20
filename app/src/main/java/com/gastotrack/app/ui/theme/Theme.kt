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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * Tokens de color semánticos especializados para finanzas personales
 * (Ingresos, Gastos y Alertas de Presupuesto).
 * Garantizan accesibilidad de contraste en Light y Dark mode y consistencia visual en toda la app.
 */
@Immutable
data class FinancialColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LightFinancialColors = FinancialColors(
    income = FinancialLightIncome,
    onIncome = FinancialLightOnIncome,
    incomeContainer = FinancialLightIncomeContainer,
    onIncomeContainer = FinancialLightOnIncomeContainer,
    expense = FinancialLightExpense,
    onExpense = FinancialLightOnExpense,
    expenseContainer = FinancialLightExpenseContainer,
    onExpenseContainer = FinancialLightOnExpenseContainer,
    warning = FinancialLightWarning,
    onWarning = FinancialLightOnWarning,
    warningContainer = FinancialLightWarningContainer,
    onWarningContainer = FinancialLightOnWarningContainer
)

val DarkFinancialColors = FinancialColors(
    income = FinancialDarkIncome,
    onIncome = FinancialDarkOnIncome,
    incomeContainer = FinancialDarkIncomeContainer,
    onIncomeContainer = FinancialDarkOnIncomeContainer,
    expense = FinancialDarkExpense,
    onExpense = FinancialDarkOnExpense,
    expenseContainer = FinancialDarkExpenseContainer,
    onExpenseContainer = FinancialDarkOnExpenseContainer,
    warning = FinancialDarkWarning,
    onWarning = FinancialDarkOnWarning,
    warningContainer = FinancialDarkWarningContainer,
    onWarningContainer = FinancialDarkOnWarningContainer
)

val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

val MaterialTheme.financialColors: FinancialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinancialColors.current

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

    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors

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

    CompositionLocalProvider(
        LocalFinancialColors provides financialColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
