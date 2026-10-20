package com.gastotrack.app.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gastotrack.app.ui.theme.PixelCardShape
import com.gastotrack.app.ui.theme.PixelPillShape
import com.gastotrack.app.ui.theme.financialColors

@Composable
fun BalanceComparisonCard(
    period: DashboardPeriod,
    incomeTotal: Double,
    expenseTotal: Double,
    incomeCount: Int,
    expenseCount: Int,
    onAddIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netBalance = incomeTotal - expenseTotal
    val isPositive = netBalance >= 0.0

    // Colores semánticos centralizados desde MaterialTheme
    val finColors = MaterialTheme.financialColors
    val incomeColor = finColors.income
    val incomeContainerColor = finColors.incomeContainer
    val incomeOnContainerColor = finColors.onIncomeContainer

    val expenseColor = finColors.expense
    val expenseContainerColor = finColors.expenseContainer
    val expenseOnContainerColor = finColors.onExpenseContainer

    // Proporción de gasto sobre el total ganado
    val ratioSpent = if (incomeTotal > 0.0) {
        (expenseTotal / incomeTotal).toFloat().coerceIn(0f, 1f)
    } else if (expenseTotal > 0.0) {
        1f
    } else {
        0f
    }

    val animatedSpentProgress by animateFloatAsState(
        targetValue = ratioSpent,
        animationSpec = tween(durationMillis = 650),
        label = "balance_spent_progress"
    )

    Card(
        shape = PixelCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera: Título y Botón rápido de Registrar Ingreso
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isPositive) incomeContainerColor else expenseContainerColor
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = null,
                            tint = if (isPositive) incomeColor else expenseColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Balance del Período",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (period == DashboardPeriod.WEEK) "Ganancias vs Pérdidas (Semana)" else "Ganancias vs Pérdidas (Mes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onAddIncomeClick,
                    shape = PixelPillShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = incomeContainerColor,
                        contentColor = incomeOnContainerColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Meter Ingreso",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ingreso",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Saldo Neto Hero (A favor o en contra)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isPositive) "Dinero Disponible (A favor)" else "Balance en Déficit (En contra)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isPositive) "+${CurrencyUtils.format(netBalance)}" else "-${CurrencyUtils.format(-netBalance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPositive) incomeColor else expenseColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(PixelPillShape)
                        .background(if (isPositive) incomeContainerColor else expenseContainerColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (isPositive) incomeColor else expenseColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPositive) "Superávit" else "Déficit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) incomeOnContainerColor else expenseOnContainerColor
                        )
                    }
                }
            }

            // Tarjetas gemelas de Ganancias (Ingresos) vs Pérdidas (Gastos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tarjeta Ingresos
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(incomeContainerColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = incomeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Ganado / Entró",
                            style = MaterialTheme.typography.labelSmall,
                            color = incomeOnContainerColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = CurrencyUtils.format(incomeTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = incomeColor
                    )
                    Text(
                        text = "$incomeCount registros",
                        style = MaterialTheme.typography.bodySmall,
                        color = incomeOnContainerColor.copy(alpha = 0.8f)
                    )
                }

                // Tarjeta Gastos / Pérdidas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(expenseContainerColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = expenseColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Gastado / Pérdidas",
                            style = MaterialTheme.typography.labelSmall,
                            color = expenseOnContainerColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = CurrencyUtils.format(expenseTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = expenseColor
                    )
                    Text(
                        text = "$expenseCount gastos",
                        style = MaterialTheme.typography.bodySmall,
                        color = expenseOnContainerColor.copy(alpha = 0.8f)
                    )
                }
            }

            // Barra proporcional de consumo de ingresos
            if (incomeTotal > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(PixelPillShape)
                            .background(incomeColor.copy(alpha = 0.35f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedSpentProgress)
                                .height(10.dp)
                                .clip(PixelPillShape)
                                .background(if (expenseTotal > incomeTotal) expenseColor else finColors.warning)
                        )
                    }

                    val spentPct = ((expenseTotal / incomeTotal) * 100).toInt()
                    val remainingPct = (100 - spentPct).coerceAtLeast(0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gastado: $spentPct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isPositive) "Restante disponible: $remainingPct%" else "¡Superaste ingresos por ${CurrencyUtils.format(expenseTotal - incomeTotal)}!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) incomeColor else expenseColor
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 Toca '+ Ingreso' para meter cuánto dinero tienes o ganaste esta semana y calcular tu comparativa de pérdidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
