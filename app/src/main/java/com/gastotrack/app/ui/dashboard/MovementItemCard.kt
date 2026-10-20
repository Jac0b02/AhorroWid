package com.gastotrack.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gastotrack.app.data.local.entities.ExpenseEntity
import com.gastotrack.app.ui.theme.PixelPillShape
import com.gastotrack.app.ui.theme.financialColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tarjeta individual de movimiento financiero (Gasto con Tarjeta, Gasto en Efectivo o Ingreso).
 * Diseñada con jerarquía tipográfica nítida, badges semánticos del método de pago y touch target
 * accesible de 48dp para la acción de eliminar.
 */
@Composable
fun MovementItemCard(
    movement: DashboardMovement,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("d MMM • HH:mm", Locale("es", "MX")) }
    val finColors = MaterialTheme.financialColors

    Card(
        shape = RoundedCornerShape(20.dp),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono, Concepto y Detalles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                when (movement) {
                    is DashboardMovement.Expense -> {
                        val expense = movement.expense
                        val isCard = expense.source == ExpenseEntity.SOURCE_CARD
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCard) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.tertiaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCard) Icons.Default.CreditCard else Icons.Default.Payments,
                                contentDescription = if (isCard) "Gasto con Tarjeta" else "Gasto en Efectivo",
                                tint = if (isCard) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = expense.concept ?: expense.bankName ?: "Gasto",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(PixelPillShape)
                                        .background(
                                            if (isCard) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = expense.bankName ?: if (isCard) "Tarjeta" else "Efectivo",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCard) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(expense.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is DashboardMovement.Income -> {
                        val income = movement.income
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(finColors.incomeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Ingreso",
                                tint = finColors.income,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = income.concept,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(PixelPillShape)
                                        .background(finColors.income.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Ingreso",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = finColors.income
                                    )
                                }
                                Text(
                                    text = dateFormat.format(Date(income.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Monto y Botón Eliminar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                when (movement) {
                    is DashboardMovement.Expense -> {
                        Text(
                            text = "-${CurrencyUtils.format(movement.expense.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    is DashboardMovement.Income -> {
                        Text(
                            text = "+${CurrencyUtils.format(movement.income.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = finColors.income
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar movimiento",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
