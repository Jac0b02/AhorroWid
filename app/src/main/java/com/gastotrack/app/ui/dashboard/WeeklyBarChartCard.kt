package com.gastotrack.app.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gastotrack.app.data.local.entities.ExpenseEntity
import com.gastotrack.app.ui.theme.PixelCardShape
import com.gastotrack.app.ui.theme.PixelPillShape
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun WeeklyBarChartCard(
    period: DashboardPeriod,
    expenses: List<ExpenseEntity>,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now(zoneId) }

    AnimatedContent(
        targetState = period,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "chart_period_transition"
    ) { currentPeriod ->
        if (currentPeriod == DashboardPeriod.WEEK) {
            // Desglose de los 7 días de la semana: Lun a Dom
            val dailyData = remember(expenses, today) {
                val dayTotals = expenses.groupBy { expense ->
                    val date = LocalDate.ofInstant(Instant.ofEpochMilli(expense.timestamp), zoneId)
                    date.dayOfWeek
                }

                val daysOrder = listOf(
                    DayOfWeek.MONDAY to ("Lun" to "Lunes"),
                    DayOfWeek.TUESDAY to ("Mar" to "Martes"),
                    DayOfWeek.WEDNESDAY to ("Mié" to "Miércoles"),
                    DayOfWeek.THURSDAY to ("Jue" to "Jueves"),
                    DayOfWeek.FRIDAY to ("Vie" to "Viernes"),
                    DayOfWeek.SATURDAY to ("Sáb" to "Sábado"),
                    DayOfWeek.SUNDAY to ("Dom" to "Domingo")
                )

                daysOrder.map { (dayOfWeek, labels) ->
                    val matching = dayTotals[dayOfWeek].orEmpty()
                    val total = matching.sumOf { it.amount }
                    DailySpendData(
                        dayOfWeek = dayOfWeek,
                        shortLabel = labels.first,
                        fullLabel = labels.second,
                        total = total,
                        count = matching.size,
                        isToday = dayOfWeek == today.dayOfWeek
                    )
                }
            }

            val maxTotal = remember(dailyData) {
                dailyData.maxOfOrNull { it.total }?.takeIf { it > 0.0 } ?: 1.0
            }

            val peakDay = remember(dailyData) {
                dailyData.filter { it.total > 0.0 }.maxByOrNull { it.total }
            }

            var selectedIndex by remember(currentPeriod) {
                val todayIdx = dailyData.indexOfFirst { it.isToday }
                mutableIntStateOf(if (todayIdx >= 0) todayIdx else 0)
            }

            val selectedItem = dailyData.getOrNull(selectedIndex) ?: dailyData.first()

            Card(
                shape = PixelCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Encabezado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Gastos por Día",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Semana en curso (Lun a Dom)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Badge del día seleccionado
                        Box(
                            modifier = Modifier
                                .clip(PixelPillShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${selectedItem.shortLabel}: ${CurrencyUtils.format(selectedItem.total)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Gráfico de 7 columnas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyData.forEachIndexed { index, item ->
                            val isSelected = index == selectedIndex
                            val rawRatio = (item.total / maxTotal).toFloat()
                            val fillRatio = if (item.total > 0.0) {
                                (0.12f + rawRatio * 0.88f).coerceIn(0.12f, 1f)
                            } else {
                                0.05f // Slot mínimo sutil cuando es 0
                            }

                            val animatedRatio by animateFloatAsState(
                                targetValue = fillRatio,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                label = "bar_ratio_$index"
                            )

                            val barColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    item.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    item.total > 0.0 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                animationSpec = tween(durationMillis = 250),
                                label = "bar_color_$index"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false, radius = 24.dp)
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedIndex = index
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Monto compacto superior si tiene gasto
                                if (item.total > 0.0) {
                                    Text(
                                        text = CurrencyUtils.formatCompact(item.total),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Barra vertical
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(animatedRatio)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(barColor)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Etiqueta del día
                                Text(
                                    text = item.shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected || item.isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else if (item.isToday) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                // Indicador de "Hoy"
                                if (item.isToday) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    // Detalle inferior: Día pico y conteo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (peakDay != null) {
                                    "Pico: ${peakDay.fullLabel} (${CurrencyUtils.format(peakDay.total)})"
                                } else {
                                    "Sin gastos registrados aún"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${selectedItem.count} ${if (selectedItem.count == 1) "compra" else "compras"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Desglose Mensual por Semanas
            val weekBuckets = remember(expenses) {
                val weekFields = WeekFields.of(Locale.getDefault())
                val grouped = expenses.groupBy { expense ->
                    val date = LocalDate.ofInstant(Instant.ofEpochMilli(expense.timestamp), zoneId)
                    date.get(weekFields.weekOfMonth())
                }

                (1..4).map { weekIndex ->
                    val itemsInWeek = grouped[weekIndex].orEmpty()
                    WeeklyBucketData(
                        weekNumber = weekIndex,
                        label = "Semana $weekIndex",
                        total = itemsInWeek.sumOf { it.amount },
                        count = itemsInWeek.size
                    )
                }
            }

            val maxWeekTotal = remember(weekBuckets) {
                weekBuckets.maxOfOrNull { it.total }?.takeIf { it > 0.0 } ?: 1.0
            }

            Card(
                shape = PixelCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Progreso del Mes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Evolución semana a semana",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Columnas de semanas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekBuckets.forEachIndexed { index, bucket ->
                            val rawRatio = (bucket.total / maxWeekTotal).toFloat()
                            val fillRatio = if (bucket.total > 0.0) {
                                (0.12f + rawRatio * 0.88f).coerceIn(0.12f, 1f)
                            } else {
                                0.05f
                            }

                            val animatedRatio by animateFloatAsState(
                                targetValue = fillRatio,
                                animationSpec = tween(durationMillis = 500),
                                label = "week_ratio_$index"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (bucket.total > 0.0) {
                                    Text(
                                        text = CurrencyUtils.formatCompact(bucket.total),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(84.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(animatedRatio)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (bucket.total > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = bucket.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
