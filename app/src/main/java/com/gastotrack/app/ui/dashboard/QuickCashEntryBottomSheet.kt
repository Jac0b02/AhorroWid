package com.gastotrack.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gastotrack.app.data.local.dao.FrequentCashExpense
import com.gastotrack.app.ui.theme.PixelInputShape
import com.gastotrack.app.ui.theme.PixelPillShape
import com.gastotrack.app.ui.theme.financialColors

/**
 * BottomSheet ergonómico de Material 3 para el registro ágil de gastos en efectivo.
 * Permite seleccionar gastos frecuentes con 1 toque o introducir montos y conceptos
 * personalizados con el teclado numérico de forma natural.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCashEntryBottomSheet(
    onDismiss: () -> Unit,
    onConfirmExpense: (Double, String) -> Unit,
    frequentExpenses: List<FrequentCashExpense> = emptyList()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val finColors = MaterialTheme.financialColors

    var amountInput by rememberSaveable { mutableStateOf("") }
    var conceptInput by rememberSaveable { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val defaultFrequentConcepts = listOf("Comida", "Café", "Transporte", "Super", "Farmacia", "Snack")
    val suggestedAmounts = listOf(20.0, 50.0, 100.0, 200.0, 500.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Registrar Gasto en Efectivo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Registra compras o pagos que realizaste sin tarjeta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chips de Sugerencias Frecuentes
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Conceptos frecuentes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val conceptsToDisplay = if (frequentExpenses.isNotEmpty()) {
                        frequentExpenses.map { it.concept }.distinct().take(6)
                    } else {
                        defaultFrequentConcepts
                    }

                    conceptsToDisplay.forEach { concept ->
                        val isSelected = conceptInput == concept
                        FilterChip(
                            selected = isSelected,
                            onClick = { conceptInput = concept },
                            shape = PixelPillShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = concept,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }

            // Campo de Monto
            OutlinedTextField(
                value = amountInput,
                onValueChange = {
                    amountInput = it
                    amountError = false
                },
                label = { Text("Monto Pagado ($ MXN)") },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        "$ ",
                        fontWeight = FontWeight.Bold,
                        color = finColors.expense
                    )
                },
                isError = amountError,
                supportingText = if (amountError) {
                    { Text("Escribe un monto mayor a $0") }
                } else null,
                singleLine = true,
                shape = PixelInputShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Chips de Montos Rápidos
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Billetes y monedas comunes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            }

            // Campo de Concepto Personalizado
            OutlinedTextField(
                value = conceptInput,
                onValueChange = { conceptInput = it },
                label = { Text("Concepto o Establecimiento (Opcional)") },
                placeholder = { Text("Ej: Comida corrida, Tiendita, Pasaje") },
                singleLine = true,
                shape = PixelInputShape,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Botones de Acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = PixelPillShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        val parsed = amountInput.replace(",", "").trim().toDoubleOrNull()
                        if (parsed == null || parsed <= 0.0) {
                            amountError = true
                        } else {
                            onConfirmExpense(parsed, conceptInput.trim().ifEmpty { "Efectivo" })
                        }
                    },
                    shape = PixelPillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guardar Gasto",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
