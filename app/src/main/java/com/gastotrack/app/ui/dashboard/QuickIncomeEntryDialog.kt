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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
fun QuickIncomeEntryDialog(
    onDismiss: () -> Unit,
    onConfirmIncome: (Double, String) -> Unit
) {
    var amountInput by rememberSaveable { mutableStateOf("") }
    var conceptInput by rememberSaveable { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val suggestedConcepts = listOf("Sueldo", "Venta", "Dinero Inicial", "Cobro", "Extra", "Freelance")
    val suggestedAmounts = listOf(500.0, 1000.0, 1500.0, 2500.0, 5000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = PixelDialogShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Registrar Ingreso de Dinero",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Añade dinero que obtuviste o con el que cuentas en la semana para contrastarlo contra tus gastos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Chips de conceptos rápidos
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedConcepts.forEach { concept ->
                        val isSelected = conceptInput == concept
                        AssistChip(
                            onClick = { conceptInput = concept },
                            shape = PixelPillShape,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            label = { Text(concept, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }
                }

                // Campo de Monto
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        amountError = false
                    },
                    label = { Text("Monto Ganado/Obtenido ($ MXN)") },
                    placeholder = { Text("Ej: 1500.00") },
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("Por favor escribe un monto mayor a $0") }
                    } else null,
                    singleLine = true,
                    shape = PixelInputShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips de montos sugeridos
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestedAmounts.forEach { amount ->
                        AssistChip(
                            onClick = {
                                amountInput = amount.toInt().toString()
                                amountError = false
                            },
                            shape = PixelPillShape,
                            label = { Text("+${CurrencyUtils.format(amount)}") }
                        )
                    }
                }

                // Campo de Concepto personalizado
                OutlinedTextField(
                    value = conceptInput,
                    onValueChange = { conceptInput = it },
                    label = { Text("Concepto o Fuente") },
                    placeholder = { Text("Ej: Pago de turno, Venta en línea") },
                    singleLine = true,
                    shape = PixelInputShape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountInput.replace(",", "").trim().toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        amountError = true
                    } else {
                        onConfirmIncome(parsed, conceptInput.trim().ifEmpty { "Ingreso Semanal" })
                    }
                },
                shape = PixelPillShape
            ) {
                Text(
                    text = "Guardar Ingreso",
                    fontWeight = FontWeight.Bold
                )
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
