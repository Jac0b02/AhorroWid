package com.gastotrack.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gastotrack.app.ui.theme.PixelDialogShape
import com.gastotrack.app.ui.theme.PixelInputShape
import com.gastotrack.app.ui.theme.PixelPillShape

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetDialog(
    period: DashboardPeriod,
    currentBudget: Double,
    onDismiss: () -> Unit,
    onConfirmBudget: (Double) -> Unit
) {
    var budgetInput by remember {
        mutableStateOf(if (currentBudget % 1.0 == 0.0) currentBudget.toInt().toString() else currentBudget.toString())
    }
    var isError by remember { mutableStateOf(false) }

    val presetChips = if (period == DashboardPeriod.WEEK) {
        listOf(1500.0, 2000.0, 2500.0, 3500.0, 5000.0)
    } else {
        listOf(6000.0, 8000.0, 10000.0, 15000.0, 20000.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PixelDialogShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = if (period == DashboardPeriod.WEEK) "Ajustar Presupuesto Semanal" else "Ajustar Presupuesto Mensual",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Elige un monto sugerido o escribe tu propio límite para calcular tus alertas visuales de gasto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Chips de sugerencia rápida
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetChips.forEach { preset ->
                        val isCurrent = (budgetInput.toDoubleOrNull() ?: 0.0) == preset
                        AssistChip(
                            onClick = {
                                budgetInput = preset.toInt().toString()
                                isError = false
                            },
                            shape = PixelPillShape,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                labelColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            label = {
                                Text(
                                    text = CurrencyUtils.format(preset),
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                // Campo numérico personalizado
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = {
                        budgetInput = it
                        isError = false
                    },
                    label = { Text("Monto límite ($ MXN)") },
                    placeholder = { Text("Ej: 2500") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Por favor ingresa un monto válido mayor a $0") }
                    } else null,
                    singleLine = true,
                    shape = PixelInputShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = budgetInput.replace(",", "").trim().toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        isError = true
                    } else {
                        onConfirmBudget(parsed)
                    }
                },
                shape = PixelPillShape
            ) {
                Text("Guardar Presupuesto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = PixelPillShape
            ) {
                Text("Cancelar")
            }
        }
    )
}
