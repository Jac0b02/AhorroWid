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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Balance
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gastotrack.app.data.local.entities.ExpenseEntity
import com.gastotrack.app.data.local.entities.IncomeEntity
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
    incomes: List<IncomeEntity> = emptyList(),
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now(zoneId) }
    val isDark = isSystemInDarkTheme()

    // Paleta semántica accesible: Verde esmeralda (Ingreso) y Rojo coral (Gasto)
    val incomeColor = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val expenseColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    AnimatedContent(
        targetState = period,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
        label = "chart_period_transition"
    ) { currentPeriod ->
        if (currentPeriod == DashboardPeriod.WEEK) {
            // Desglose de los 7 días de la semana: Lun a Dom con Barras Dobles (Ingreso vs Gasto)
            val dailyData = remember(expenses, incomes, today) {
                val dayExpenses = expenses.groupBy { expense ->
                    val date = LocalDate.ofInstant(Instant.ofEpochMilli(expense.timestamp), zoneId)
                    date.dayOfWeek
                }
                val dayIncomes = incomes.groupBy { income ->
                    val date = LocalDate.ofInstant(Instant.ofEpochMilli(income.timestamp), zoneId)
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
                    val matchingExp = dayExpenses[dayOfWeek].orEmpty()
                    val matchingInc = dayIncomes[dayOfWeek].orEmpty()
                    DailySpendData(
                        dayOfWeek = dayOfWeek,
                        shortLabel = labels.first,
                        fullLabel = labels.second,
                        expenseTotal = matchingExp.sumOf { it.amount },
                        incomeTotal = matchingInc.sumOf { it.amount },
                        expenseCount = matchingExp.size,
                        incomeCount = matchingInc.size,
                        isToday = dayOfWeek == today.dayOfWeek
                    )
                }
            }

            val maxVal = remember(dailyData) {
                dailyData.maxOfOrNull { maxOf(it.expenseTotal, it.incomeTotal) }?.takeIf { it > 0.0 } ?: 1.0
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
                    // Encabezado y Leyenda
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
                                    text = "Comparativa Diaria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ingresos vs Gastos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Leyenda elegante
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(incomeColor)
                                )
                                Text(
                                    text = "Ingreso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(expenseColor)
                                )
                                Text(
                                    text = "Gasto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Columnas de días con Barras Dobles lado a lado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dailyData.forEachIndexed { index, item ->
                            val isSelected = index == selectedIndex

                            // Ratios normalizados
                            val expRatio = if (item.expenseTotal > 0.0) {
                                (0.12f + (item.expenseTotal / maxVal).toFloat() * 0.88f).coerceIn(0.12f, 1f)
                            } else 0.04f

                            val incRatio = if (item.incomeTotal > 0.0) {
                                (0.12f + (item.incomeTotal / maxVal).toFloat() * 0.88f).coerceIn(0.12f, 1f)
                            } else 0.04f

                            val animatedExpRatio by animateFloatAsState(
                                targetValue = expRatio,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                label = "bar_exp_$index"
                            )

                            val animatedIncRatio by animateFloatAsState(
                                targetValue = incRatio,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                label = "bar_inc_$index"
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                                        else Color.Transparent
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true, radius = 24.dp)
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedIndex = index
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Balance neto compacto arriba si hay movimientos
                                if (item.expenseTotal > 0.0 || item.incomeTotal > 0.0) {
                                    val net = item.netBalance
                                    val prefix = if (net > 0) "+" else ""
                                    Text(
                                        text = "$prefix${CurrencyUtils.formatCompact(net)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (net >= 0) incomeColor else expenseColor,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                // Contenedor de barras dobles (Lado a Lado)
                                Row(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(84.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Barra 1: Ingreso (Verde)
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(animatedIncRatio)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (item.incomeTotal > 0.0) incomeColor else Color.Transparent)
                                        )
                                    }

                                    // Barra 2: Gasto (Rojo)
                                    Box(
                                        modifier = Modifier
                                            .width(9.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(animatedExpRatio)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (item.expenseTotal > 0.0) expenseColor else Color.Transparent)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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

                    // Detalle del día seleccionado: Ingresos, Gastos y Balance Neto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PixelPillShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedItem.fullLabel}:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "🟢 +${CurrencyUtils.format(selectedItem.incomeTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = incomeColor
                            )
                            Text(
                                text = "🔴 -${CurrencyUtils.format(selectedItem.expenseTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = expenseColor
                            )
                        }
                    }
                }
            }
        } else {
            // Desglose Mensual por Semanas con Barras Dobles
            val weekBuckets = remember(expenses, incomes) {
                val weekFields = WeekFields.of(Locale.getDefault())
                val groupedExp = expenses.groupBy { expense ->
                    val date = LocalDate.ofInstant(Instant.ofEpochMilli(expense.timestamp), zoneId)
                    date.get(weekFields.weekOfMonth())
                }
                val groupedInc = incomes.groupBy { income ->
                    val date = LocalDate.ofInstant(Instant.ofEpochMilli(income.timestamp), zoneId)
                    date.get(weekFields.weekOfMonth())
                }

                (1..5).map { weekIndex ->
                    val expInWeek = groupedExp[weekIndex].orEmpty()
                    val incInWeek = groupedInc[weekIndex].orEmpty()
                    WeeklyBucketData(
                        weekNumber = weekIndex,
                        label = "Sem $weekIndex",
                        expenseTotal = expInWeek.sumOf { it.amount },
                        incomeTotal = incInWeek.sumOf { it.amount },
                        expenseCount = expInWeek.size,
                        incomeCount = incInWeek.size
                    )
                }.filter { it.expenseTotal > 0.0 || it.incomeTotal > 0.0 || it.weekNumber <= 4 }
            }

            val maxWeekVal = remember(weekBuckets) {
                weekBuckets.maxOfOrNull { maxOf(it.expenseTotal, it.incomeTotal) }?.takeIf { it > 0.0 } ?: 1.0
            }

            var selectedWeekIndex by remember(currentPeriod) { mutableIntStateOf(0) }
            val selectedBucket = weekBuckets.getOrNull(selectedWeekIndex) ?: weekBuckets.first()

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
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Evolución del Mes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Comparativa semana a semana",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Leyenda
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(incomeColor))
                                Text("Ingreso", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(expenseColor))
                                Text("Gasto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Columnas de semanas con barras dobles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekBuckets.forEachIndexed { index, bucket ->
                            val isSelected = index == selectedWeekIndex

                            val expRatio = if (bucket.expenseTotal > 0.0) {
                                (0.12f + (bucket.expenseTotal / maxWeekVal).toFloat() * 0.88f).coerceIn(0.12f, 1f)
                            } else 0.04f

                            val incRatio = if (bucket.incomeTotal > 0.0) {
                                (0.12f + (bucket.incomeTotal / maxWeekVal).toFloat() * 0.88f).coerceIn(0.12f, 1f)
                            } else 0.04f

                            val animatedExpRatio by animateFloatAsState(targetValue = expRatio, animationSpec = tween(500), label = "w_exp_$index")
                            val animatedIncRatio by animateFloatAsState(targetValue = incRatio, animationSpec = tween(500), label = "w_inc_$index")

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedWeekIndex = index
                                    }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Row(
                                    modifier = Modifier.width(26.dp).height(80.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Ingreso semanal
                                    Box(
                                        modifier = Modifier.width(11.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(animatedIncRatio).clip(RoundedCornerShape(4.dp)).background(if (bucket.incomeTotal > 0.0) incomeColor else Color.Transparent)
                                        )
                                    }

                                    // Gasto semanal
                                    Box(
                                        modifier = Modifier.width(11.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(animatedExpRatio).clip(RoundedCornerShape(4.dp)).background(if (bucket.expenseTotal > 0.0) expenseColor else Color.Transparent)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = bucket.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Detalle de la semana seleccionada
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PixelPillShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedBucket.label}:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "🟢 +${CurrencyUtils.format(selectedBucket.incomeTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = incomeColor
                            )
                            Text(
                                text = "🔴 -${CurrencyUtils.format(selectedBucket.expenseTotal)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = expenseColor
                            )
                        }
                    }
                }
            }
        }
    }
}
