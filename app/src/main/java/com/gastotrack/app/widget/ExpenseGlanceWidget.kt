package com.gastotrack.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.data.local.dao.FrequentCashExpense
import com.gastotrack.app.data.local.dao.WeeklyExpenseSummary
import com.gastotrack.app.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class ExpenseGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val mondayStart = AppDatabase.getStartOfCurrentWeekMondayMillis()
        val thirtyDaysAgo = AppDatabase.getThirtyDaysAgoMillis()

        val summary = withContext(Dispatchers.IO) {
            db.expenseDao().getWeeklySummarySync(mondayStart)
        }

        val topCashList = withContext(Dispatchers.IO) {
            val dbSuggestions = db.expenseDao().getTopFrequentCashExpensesSync(thirtyDaysAgo)
            if (dbSuggestions.isNotEmpty()) {
                dbSuggestions
            } else {
                // Sugerencias iniciales inteligentes cuando aún no hay historial de efectivo
                listOf(
                    FrequentCashExpense(amount = 50.0, concept = "Café", frequency = 1, lastUsed = 0L),
                    FrequentCashExpense(amount = 73.0, concept = "Comida", frequency = 1, lastUsed = 0L),
                    FrequentCashExpense(amount = 100.0, concept = "Transporte", frequency = 1, lastUsed = 0L)
                )
            }
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
                ExpenseWidgetContent(
                    summary = summary,
                    suggestions = topCashList,
                    openAppIntent = openMainIntent,
                    openQuickDialogIntent = quickDialogIntent
                )
            }
        }
    }

    @Composable
    private fun ExpenseWidgetContent(
        summary: WeeklyExpenseSummary,
        suggestions: List<FrequentCashExpense>,
        openAppIntent: Intent,
        openQuickDialogIntent: Intent
    ) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            // Encabezado con Total Semanal Acumulado y desglose Tarjeta vs Efectivo
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openAppIntent)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Total Semanal",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = currencyFormat.format(summary.total),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "💳 Tarjeta: ${currencyFormat.format(summary.tarjeta)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "💵 Efectivo: ${currencyFormat.format(summary.efectivo)}",
                        style = TextStyle(
                            color = GlanceTheme.colors.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            Text(
                text = "Gasto rápido en efectivo (1 toque):",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Fila de botones rápidos de 1 solo toque + botón "+ Otro"
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestions.take(3).forEach { suggestion ->
                    val amountLabel = if (suggestion.amount % 1.0 == 0.0) {
                        "+$${suggestion.amount.toInt()}"
                    } else {
                        "+$${String.format(Locale.US, "%.2f", suggestion.amount)}"
                    }
                    val shortConcept = suggestion.concept.take(8)

                    Button(
                        text = "$amountLabel $shortConcept",
                        onClick = actionRunCallback<QuickCashActionCallback>(
                            actionParametersOf(
                                QuickCashActionCallback.KEY_AMOUNT to suggestion.amount,
                                QuickCashActionCallback.KEY_CONCEPT to suggestion.concept
                            )
                        ),
                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = GlanceTheme.colors.secondaryContainer,
                            contentColor = GlanceTheme.colors.onSecondaryContainer
                        ),
                        modifier = GlanceModifier
                            .defaultWeight()
                            .padding(end = 4.dp)
                    )
                }

                Button(
                    text = "+ Otro",
                    onClick = actionStartActivity(openQuickDialogIntent),
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = GlanceTheme.colors.primary,
                        contentColor = GlanceTheme.colors.onPrimary
                    )
                )
            }
        }
    }
}

class ExpenseGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseGlanceWidget()
}