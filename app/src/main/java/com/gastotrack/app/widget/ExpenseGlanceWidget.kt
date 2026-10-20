package com.gastotrack.app.widget

import android.app.KeyguardManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gastotrack.app.GastoTrackApp
import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.data.local.dao.FrequentCashExpense
import com.gastotrack.app.data.local.dao.WeeklyExpenseSummary
import com.gastotrack.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class ExpenseGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val mondayStart = AppDatabase.getStartOfCurrentWeekMondayMillis()
        val thirtyDaysAgo = AppDatabase.getThirtyDaysAgoMillis()

        val initialSummary = withContext(Dispatchers.IO) {
            db.expenseDao().getWeeklySummarySync(mondayStart)
        }

        val initialSuggestions = withContext(Dispatchers.IO) {
            val dbSuggestions = db.expenseDao().getTopFrequentCashExpensesSync(thirtyDaysAgo)
            if (dbSuggestions.isNotEmpty()) dbSuggestions else defaultSuggestions
        }

        val quickDialogIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_QUICK_CASH_DIALOG, true)
        }

        val openMainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                // Observación reactiva en tiempo real del Flow de Room
                // Cada nuevo gasto insertado en la BD actualiza la UI inmediatamente
                val summary by db.expenseDao().observeWeeklySummary(mondayStart).collectAsState(
                    initial = initialSummary
                )
                val rawSuggestions by db.expenseDao().getTopFrequentCashExpenses(thirtyDaysAgo).collectAsState(
                    initial = initialSuggestions
                )
                val topCashList = if (rawSuggestions.isNotEmpty()) rawSuggestions else defaultSuggestions

                val isLocked = isDeviceLocked(context, id)

                val size = LocalSize.current
                val isShort = size.height < 118.dp
                val isNarrow = size.width < 210.dp

                when {
                    isShort && !isNarrow -> ExpenseWidgetWideSingleRowContent(
                        summary = summary,
                        suggestions = topCashList,
                        openAppIntent = openMainIntent,
                        openQuickDialogIntent = quickDialogIntent,
                        isLocked = isLocked,
                        width = size.width
                    )
                    isShort && isNarrow -> ExpenseWidgetShortCompactContent(
                        summary = summary,
                        suggestions = topCashList,
                        openAppIntent = openMainIntent,
                        openQuickDialogIntent = quickDialogIntent,
                        isLocked = isLocked
                    )
                    size.height >= 195.dp && isNarrow -> ExpenseWidgetTallContent(
                        summary = summary,
                        suggestions = topCashList,
                        openAppIntent = openMainIntent,
                        openQuickDialogIntent = quickDialogIntent,
                        isLocked = isLocked
                    )
                    size.height >= 190.dp && !isNarrow -> ExpenseWidgetLargeContent(
                        summary = summary,
                        suggestions = topCashList,
                        openAppIntent = openMainIntent,
                        openQuickDialogIntent = quickDialogIntent,
                        isLocked = isLocked
                    )
                    !isNarrow -> ExpenseWidgetWideContent(
                        summary = summary,
                        suggestions = topCashList,
                        openAppIntent = openMainIntent,
                        openQuickDialogIntent = quickDialogIntent,
                        isLocked = isLocked
                    )
                    else -> ExpenseWidgetCompactContent(
                        summary = summary,
                        suggestions = topCashList,
                        openAppIntent = openMainIntent,
                        openQuickDialogIntent = quickDialogIntent,
                        isLocked = isLocked
                    )
                }
            }
        }
    }

    private fun isDeviceLocked(context: Context, id: GlanceId? = null): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isKeyguardLocked = keyguardManager?.isKeyguardLocked == true || keyguardManager?.isDeviceLocked == true

        val isKeyguardHost = if (id != null) {
            try {
                val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
                val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
                val category = options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            } catch (_: Exception) {
                false
            }
        } else false

        val isScreenOffOrLocked = try {
            GastoTrackApp.isScreenOffOrLocked
        } catch (_: Exception) {
            false
        }

        return isKeyguardLocked || isKeyguardHost || isScreenOffOrLocked
    }

    /**
     * Diseño Horizontal Delgado (ej. 3x1, 4x1, 5x1 celdas):
     * Muestra el resumen semanal a la izquierda y una fila horizontal continua de accesos directos
     * para aprovechar el ancho total sin comprimir la altura.
     */
    @Composable
    private fun ExpenseWidgetWideSingleRowContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent,
        isLocked: Boolean,
        width: Dp
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Resumen de la semana a la izquierda
            Column(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(14.dp)
                    .clickable(actionStartActivity(openAppIntent))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLocked) "🔒 Semana" else "Semana",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(
                    text = if (isLocked) "$ ••••" else formatCurrencyClean(summary.total),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Fila horizontal de botones de acción rápida
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val maxItems = if (width >= 320.dp) 3 else 2
                for (i in 0 until maxItems) {
                    val item = suggestions.getOrNull(i)
                    if (item != null) {
                        QuickExpenseCard(
                            item = item,
                            modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                            compact = true
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                    }
                }
                QuickAddCustomCard(
                    openQuickDialogIntent = openQuickDialogIntent,
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    compact = true
                )
            }
        }
    }

    /**
     * Diseño Ultra-Compacto (ej. 2x1 celdas):
     * Muestra el total a la izquierda y los accesos rápidos indispensables en una sola fila.
     */
    @Composable
    private fun ExpenseWidgetShortCompactContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent,
        isLocked: Boolean
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(18.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openAppIntent))
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLocked) "🔒 Total" else "Total",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(
                    text = if (isLocked) "$ ••••" else formatCurrencyClean(summary.total),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val topItem = suggestions.firstOrNull()
                if (topItem != null) {
                    QuickExpenseCard(
                        item = topItem,
                        modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                        compact = true
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
                QuickAddCustomCard(
                    openQuickDialogIntent = openQuickDialogIntent,
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    compact = true
                )
            }
        }
    }

    /**
     * Diseño Vertical / Alargado (ej. 2x6, 2x5, 2x4 celdas):
     * Muestra la tarjeta de total semanal arriba con desglose claro y la cuadrícula 2x2 abajo.
     */
    @Composable
    private fun ExpenseWidgetTallContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent,
        isLocked: Boolean
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Tarjeta de resumen superior
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(18.dp)
                    .clickable(actionStartActivity(openAppIntent))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLocked) "🔒 Total Semanal" else "Total Semanal",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.height(1.dp))
                Text(
                    text = if (isLocked) "$ ••••" else formatCurrencyClean(summary.total),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💳 Tarjeta",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isLocked) "••••" else formatCurrencyClean(summary.tarjeta),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "💵 Efectivo",
                            style = TextStyle(
                                color = GlanceTheme.colors.secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isLocked) "••••" else formatCurrencyClean(summary.efectivo),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Cuadrícula 2x2 de accesos rápidos
            ExpenseWidgetGrid(
                suggestions = suggestions,
                openQuickDialogIntent = openQuickDialogIntent,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            )
        }
    }

    /**
     * Diseño Horizontal / Ancho (ej. 4x2, 5x2, 6x2 celdas):
     * Lado izquierdo con resumen semanal y lado derecho con pad 2x2.
     */
    @Composable
    private fun ExpenseWidgetWideContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent,
        isLocked: Boolean
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(22.dp)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity(openAppIntent))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLocked) "🔒 Total Semanal" else "Total Semanal",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                Text(
                    text = if (isLocked) "$ ••••" else formatCurrencyClean(summary.total),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = if (isLocked) "💳 Tarjeta: ••••" else "💳 ${formatCurrencyClean(summary.tarjeta)}",
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Text(
                    text = if (isLocked) "💵 Efectivo: ••••" else "💵 ${formatCurrencyClean(summary.efectivo)}",
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            ExpenseWidgetGrid(
                suggestions = suggestions,
                openQuickDialogIntent = openQuickDialogIntent,
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight(),
                compact = true
            )
        }
    }

    /**
     * Diseño Grande / Expandido (ej. 3x3, 4x3, 4x4 celdas):
     * Cabecera superior espaciosa y cuadrícula 2x2 grande.
     */
    @Composable
    private fun ExpenseWidgetLargeContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent,
        isLocked: Boolean
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(26.dp)
                .padding(10.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(20.dp)
                    .clickable(actionStartActivity(openAppIntent))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if (isLocked) "🔒 Total Semanal (Bloqueado)" else "Total Semanal",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = if (isLocked) "$ ••••" else formatCurrencyClean(summary.total),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isLocked) "💳 Tarjeta: ••••" else "💳 Tarjeta: ${formatCurrencyClean(summary.tarjeta)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = if (isLocked) "💵 Efectivo: ••••" else "💵 Efectivo: ${formatCurrencyClean(summary.efectivo)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            ExpenseWidgetGrid(
                suggestions = suggestions,
                openQuickDialogIntent = openQuickDialogIntent,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                compact = false
            )
        }
    }

    /**
     * Diseño Compacto Cuadrado (ej. 2x2 celdas):
     * Muestra siempre el monto total actual arriba de forma sutil y los 4 cuadrados debajo.
     */
    @Composable
    private fun ExpenseWidgetCompactContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent,
        isLocked: Boolean
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(20.dp)
                .padding(6.dp)
        ) {
            // Cabecera compacta con el gasto acumulado
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openAppIntent))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLocked) "🔒 Total: ••••" else "Total: ${formatCurrencyClean(summary.total)}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = if (isLocked) "Bloqueado" else "Semana",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(3.dp))

            ExpenseWidgetGrid(
                suggestions = suggestions,
                openQuickDialogIntent = openQuickDialogIntent,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                compact = true
            )
        }
    }

    /**
     * Cuadrícula simétrica 2x2:
     * [ Gasto 1 ] [ Gasto 2 ]
     * [ Gasto 3 ] [ + Otro  ]
     */
    @Composable
    private fun ExpenseWidgetGrid(
        suggestions: List<FrequentCashExpense>,
        openQuickDialogIntent: Intent,
        modifier: GlanceModifier,
        compact: Boolean = false
    ) {
        val spacerSize = if (compact) 4.dp else 6.dp
        Column(modifier = modifier) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                QuickExpenseCard(
                    item = suggestions.getOrNull(0),
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    compact = compact
                )
                Spacer(modifier = GlanceModifier.width(spacerSize))
                QuickExpenseCard(
                    item = suggestions.getOrNull(1),
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    compact = compact
                )
            }

            Spacer(modifier = GlanceModifier.height(spacerSize))

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
            ) {
                QuickExpenseCard(
                    item = suggestions.getOrNull(2),
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    compact = compact
                )
                Spacer(modifier = GlanceModifier.width(spacerSize))
                QuickAddCustomCard(
                    openQuickDialogIntent = openQuickDialogIntent,
                    modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                    compact = compact
                )
            }
        }
    }

    @Composable
    private fun QuickExpenseCard(
        item: FrequentCashExpense?,
        modifier: GlanceModifier,
        compact: Boolean = false
    ) {
        if (item == null) {
            Box(modifier = modifier) {}
            return
        }

        val amountLabel = if (item.amount % 1.0 == 0.0) {
            "+$${item.amount.toInt()}"
        } else {
            "+$${String.format(Locale.US, "%.2f", item.amount)}"
        }

        Column(
            modifier = modifier
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(if (compact) 14.dp else 16.dp)
                .clickable(
                    actionRunCallback<QuickCashActionCallback>(
                        actionParametersOf(
                            QuickCashActionCallback.KEY_AMOUNT to item.amount,
                            QuickCashActionCallback.KEY_CONCEPT to item.concept
                        )
                    )
                )
                .padding(vertical = if (compact) 2.dp else 4.dp, horizontal = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = amountLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = item.concept.take(10),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }

    @Composable
    private fun QuickAddCustomCard(
        openQuickDialogIntent: Intent,
        modifier: GlanceModifier,
        compact: Boolean = false
    ) {
        Column(
            modifier = modifier
                .background(GlanceTheme.colors.primary)
                .cornerRadius(if (compact) 14.dp else 16.dp)
                .clickable(actionStartActivity(openQuickDialogIntent))
                .padding(vertical = if (compact) 2.dp else 4.dp, horizontal = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "+",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontSize = if (compact) 16.sp else 19.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = "Otro",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimary,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }

    companion object {
        private val defaultSuggestions = listOf(
            FrequentCashExpense(amount = 50.0, concept = "Café", frequency = 1, lastUsed = 0L),
            FrequentCashExpense(amount = 73.0, concept = "Comida", frequency = 1, lastUsed = 0L),
            FrequentCashExpense(amount = 100.0, concept = "Transporte", frequency = 1, lastUsed = 0L)
        )

        fun formatCurrencyClean(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
            if (amount % 1.0 == 0.0) {
                format.maximumFractionDigits = 0
                format.minimumFractionDigits = 0
            } else {
                format.maximumFractionDigits = 2
                format.minimumFractionDigits = 2
            }
            return format.format(amount)
        }
    }
}

class ExpenseGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseGlanceWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == Intent.ACTION_USER_PRESENT ||
            action == Intent.ACTION_SCREEN_OFF ||
            action == Intent.ACTION_SCREEN_ON ||
            action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val pendingResult = try { goAsync() } catch (_: Exception) { null }
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    glanceAppWidget.updateAll(context)
                } catch (e: Exception) {
                    android.util.Log.e("WidgetReceiver", "Error actualizando widget ($action): ${e.message}")
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                glanceAppWidget.updateAll(context)
            } catch (e: Exception) {
                android.util.Log.e("WidgetReceiver", "Error en onAppWidgetOptionsChanged: ${e.message}")
            } finally {
                pendingResult?.finish()
            }
        }
    }
}