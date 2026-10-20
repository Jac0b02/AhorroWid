package com.gastotrack.app.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gastotrack.app.ui.theme.PixelCardShape
import com.gastotrack.app.ui.theme.PixelPillShape
import com.gastotrack.app.ui.theme.financialColors

/**
 * Estado vacío (Empty State) interactivo para la sección de movimientos.
 * Proporciona retroalimentación visual amigable con icono ilustrativo y botones
 * de llamada a la acción directa (CTA) según el filtro activo.
 */
@Composable
fun MovementsEmptyStateCard(
    filter: Int, // 0: Todos, 1: Gastos, 2: Ingresos
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = MaterialTheme.financialColors

    Card(
        shape = PixelCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icono ilustrativo en contenedor suave
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (filter) {
                        1 -> Icons.AutoMirrored.Filled.ReceiptLong
                        2 -> Icons.AutoMirrored.Filled.TrendingUp
                        else -> Icons.Default.Payments
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (filter) {
                        1 -> "No hay gastos registrados"
                        2 -> "No hay ingresos registrados"
                        else -> "Sin movimientos en este período"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (filter) {
                        1 -> "Las compras con tarjeta se detectarán automáticamente o puedes registrar gastos en efectivo aquí."
                        2 -> "Añade el dinero que obtuviste o con el que cuentas para contrastar tus balances."
                        else -> "Comienza registrando tu dinero disponible o agregando tu primer gasto en efectivo."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botones de Acción Inmediata
            when (filter) {
                1 -> {
                    Button(
                        onClick = onAddExpenseClick,
                        shape = PixelPillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Gasto en Efectivo", fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    Button(
                        onClick = onAddIncomeClick,
                        shape = PixelPillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = finColors.income,
                            contentColor = finColors.onIncome
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Ingreso", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onAddIncomeClick,
                            shape = PixelPillShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = finColors.incomeContainer,
                                contentColor = finColors.onIncomeContainer
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Ingreso", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onAddExpenseClick,
                            shape = PixelPillShape,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Gasto", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
