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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun BudgetHealthCard(
    period: DashboardPeriod,
    spentAmount: Double,
    budgetGoal: Double,
    onAdjustBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = if (budgetGoal > 0.0) (spentAmount / budgetGoal).toFloat() else 0f
    val progressClamped = ratio.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = progressClamped,
        animationSpec = tween(durationMillis = 600),
        label = "budget_progress"
    )

    val finColors = MaterialTheme.financialColors

    // Colores semafóricos según el nivel de consumo centralizados en el sistema de diseño
    val healthColor = when {
        ratio > 0.90f -> MaterialTheme.colorScheme.error
        ratio >= 0.70f -> finColors.warning
        else -> finColors.income
    }

    val healthContainerColor = when {
        ratio > 0.90f -> MaterialTheme.colorScheme.errorContainer
        ratio >= 0.70f -> finColors.warningContainer
        else -> finColors.incomeContainer
    }

    val healthOnContainerColor = when {
        ratio > 0.90f -> MaterialTheme.colorScheme.onErrorContainer
        ratio >= 0.70f -> finColors.onWarningContainer
        else -> finColors.onIncomeContainer
    }

    val animatedBarColor by animateColorAsState(
        targetValue = healthColor,
        animationSpec = tween(durationMillis = 400),
        label = "health_bar_color"
    )

    val remainingAmount = budgetGoal - spentAmount

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
            // Fila de Encabezado: Título y Botón Ajustar
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
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Presupuesto Objetivo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (period == DashboardPeriod.WEEK) "Límite semanal" else "Límite mensual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onAdjustBudgetClick,
                    shape = PixelPillShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Ajustar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajustar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Montos y Porcentaje
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Gastado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CurrencyUtils.format(spentAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " / ${CurrencyUtils.format(budgetGoal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(PixelPillShape)
                        .background(healthContainerColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = healthOnContainerColor
                    )
                }
            }

            // Barra de Progreso Semafórica con Guías Visuales
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(PixelPillShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    // Barra de llenado animada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(14.dp)
                            .clip(PixelPillShape)
                            .background(animatedBarColor)
                    )

                    // Marcadores de zona sutiles al 70% y 90%
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.weight(0.70f))
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.weight(0.20f))
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.weight(0.10f))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Alerta 70%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = CurrencyUtils.format(budgetGoal),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Callout de Estado y Saldo Disponible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(healthContainerColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = when {
                        ratio > 1.0f -> Icons.Default.ErrorOutline
                        ratio >= 0.70f -> Icons.Default.Warning
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = healthOnContainerColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when {
                        remainingAmount < 0 -> "¡Presupuesto superado por ${CurrencyUtils.format(-remainingAmount)}!"
                        ratio >= 0.90f -> "Al límite: Solo te quedan ${CurrencyUtils.format(remainingAmount)} disponibles"
                        ratio >= 0.70f -> "Atención: Te quedan ${CurrencyUtils.format(remainingAmount)} disponibles"
                        else -> "¡Excelente ritmo! Te quedan ${CurrencyUtils.format(remainingAmount)} disponibles"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = healthOnContainerColor
                )
            }
        }
    }
}
