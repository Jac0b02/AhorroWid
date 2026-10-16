package com.gastotrack.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gastotrack.app.data.local.dao.BankRuleDao
import com.gastotrack.app.data.local.entities.BankRuleEntity
import com.gastotrack.app.service.NotificationRuleEvaluator
import com.gastotrack.app.service.RuleEvaluationResult
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    bankRuleDao: BankRuleDao,
    modifier: Modifier = Modifier
) {
    val rules by bankRuleDao.observeAllRules().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }

    // Estado del selector y editor en el Sandbox (Notification Playground)
    var selectedRuleId by rememberSaveable { mutableStateOf<Long?>(null) }
    var bankDropdownExpanded by remember { mutableStateOf(false) }

    // Campos editables en tiempo real de la regla seleccionada en el Sandbox
    var draftBankName by rememberSaveable { mutableStateOf("") }
    var draftFilter by rememberSaveable { mutableStateOf("") }
    var draftPositiveKeywords by rememberSaveable { mutableStateOf("") }
    var draftNegativeKeywords by rememberSaveable { mutableStateOf("") }
    var draftAmountRegex by rememberSaveable { mutableStateOf("") }
    var draftMerchantRegex by rememberSaveable { mutableStateOf("") }
    var draftIsEnabled by rememberSaveable { mutableStateOf(true) }

    // Texto de prueba del Sandbox y resultado en vivo
    var sampleNotificationText by rememberSaveable {
        mutableStateOf("BBVA: Compra realizada por $485.50 MXN en STARBUCKS COFFEE con tu tarjeta digital.")
    }
    var evaluationResult by remember { mutableStateOf<RuleEvaluationResult?>(null) }

    // Estado para el modal de creación/edición directa desde la lista
    var ruleModalTarget by remember { mutableStateOf<BankRuleEntity?>(null) }
    var showRuleModal by remember { mutableStateOf(false) }

    // Sincronizar la regla seleccionada cuando cargan las reglas de Room
    LaunchedEffect(rules) {
        if (rules.isNotEmpty() && (selectedRuleId == null || rules.none { it.id == selectedRuleId })) {
            val first = rules.first()
            selectedRuleId = first.id
            draftBankName = first.bankName
            draftFilter = first.packageOrTitleFilter
            draftPositiveKeywords = first.positiveKeywords
            draftNegativeKeywords = first.negativeKeywords
            draftAmountRegex = first.amountRegex
            draftMerchantRegex = first.merchantRegex.orEmpty()
            draftIsEnabled = first.isEnabled
        }
    }

    fun selectRuleForPlayground(rule: BankRuleEntity) {
        selectedRuleId = rule.id
        draftBankName = rule.bankName
        draftFilter = rule.packageOrTitleFilter
        draftPositiveKeywords = rule.positiveKeywords
        draftNegativeKeywords = rule.negativeKeywords
        draftAmountRegex = rule.amountRegex
        draftMerchantRegex = rule.merchantRegex.orEmpty()
        draftIsEnabled = rule.isEnabled
        evaluationResult = null
    }

    fun buildCurrentDraftRule(): BankRuleEntity {
        return BankRuleEntity(
            id = selectedRuleId ?: 0L,
            bankName = draftBankName.trim().ifEmpty { "Banco" },
            packageOrTitleFilter = draftFilter.trim(),
            positiveKeywords = draftPositiveKeywords.trim(),
            negativeKeywords = draftNegativeKeywords.trim(),
            amountRegex = draftAmountRegex.trim().ifEmpty { BankRuleEntity.DEFAULT_AMOUNT_REGEX },
            merchantRegex = draftMerchantRegex.trim().ifEmpty { null },
            isEnabled = draftIsEnabled
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gestor de Bancos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configura las reglas dinámicas de lectura de notificaciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            ruleModalTarget = BankRuleEntity.createDefaultTemplate("Mercado Pago")
                            showRuleModal = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nueva Regla")
                    }
                }
            }

            // Lista de bancos configurados
            items(rules, key = { it.id }) { rule ->
                BankRuleItemCard(
                    rule = rule,
                    isSelectedInPlayground = (rule.id == selectedRuleId),
                    onToggleEnabled = { enabled ->
                        coroutineScope.launch {
                            bankRuleDao.updateRuleEnabledState(rule.id, enabled)
                        }
                    },
                    onEditClick = {
                        selectRuleForPlayground(rule)
                        ruleModalTarget = rule
                        showRuleModal = true
                    },
                    onLoadInPlayground = {
                        selectRuleForPlayground(rule)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Regla '${rule.bankName}' cargada en el Área de Pruebas")
                        }
                    },
                    onDeleteClick = {
                        coroutineScope.launch {
                            bankRuleDao.deleteRule(rule)
                            snackbarHostState.showSnackbar("Regla '${rule.bankName}' eliminada")
                        }
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // SECCIÓN 2: ÁREA DE PRUEBAS (NOTIFICATION PLAYGROUND)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Área de Pruebas (Notification Playground)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Simula notificaciones reales y ajusta las expresiones regulares en vivo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Selector de banco a probar
                        ExposedDropdownMenuBox(
                            expanded = bankDropdownExpanded,
                            onExpandedChange = { bankDropdownExpanded = !bankDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = draftBankName.ifEmpty { "Selecciona un banco" },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Banco a probar") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = bankDropdownExpanded,
                                onDismissRequest = { bankDropdownExpanded = false }
                            ) {
                                rules.forEach { ruleItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Text("${ruleItem.bankName} (${ruleItem.packageOrTitleFilter})")
                                        },
                                        onClick = {
                                            selectRuleForPlayground(ruleItem)
                                            bankDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Parámetros editables en vivo de la regla seleccionada
                        OutlinedTextField(
                            value = draftPositiveKeywords,
                            onValueChange = { draftPositiveKeywords = it },
                            label = { Text("Palabras clave positivas (separadas por coma)") },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = draftNegativeKeywords,
                            onValueChange = { draftNegativeKeywords = it },
                            label = { Text("Palabras clave de exclusión (negativas)") },
                            singleLine = false,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = draftAmountRegex,
                            onValueChange = { draftAmountRegex = it },
                            label = { Text("Regex de Monto (Grupo 1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = draftMerchantRegex,
                            onValueChange = { draftMerchantRegex = it },
                            label = { Text("Regex de Comercio (Opcional, Grupo 1)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Ejemplos rápidos de prueba de 1 toque
                        Text(
                            text = "Ejemplos rápidos para probar:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    sampleNotificationText =
                                        "BBVA: Compra realizada por $1,249.90 MXN en AMAZON MEXICO con tu tarjeta terminación 4012."
                                },
                                label = { Text("Compra BBVA") }
                            )
                            AssistChip(
                                onClick = {
                                    sampleNotificationText =
                                        "BanCoppel: Se realizó un cargo por $320.00 en OXXO CULIACAN con tu tarjeta de débito."
                                },
                                label = { Text("Cargo BanCoppel") }
                            )
                            AssistChip(
                                onClick = {
                                    sampleNotificationText =
                                        "Nu: Compra aprobada por $185.00 en UBER TRIP con tu tarjeta Morada."
                                },
                                label = { Text("Compra Nu") }
                            )
                            AssistChip(
                                onClick = {
                                    sampleNotificationText =
                                        "BBVA: Recibiste un abono de $3,500.00 MXN por transferencia SPEI."
                                },
                                label = { Text("Abono (Excluido)") }
                            )
                        }

                        // OutlinedTextField multilinea requerido
                        OutlinedTextField(
                            value = sampleNotificationText,
                            onValueChange = { sampleNotificationText = it },
                            label = { Text("Pega aquí el texto de la notificación de tu banco") },
                            placeholder = {
                                Text("Ej: Compra aprobada por $250.00 MXN en SUPERMERCADO...")
                            },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Botón Probar lectura
                        Button(
                            onClick = {
                                val tempRule = buildCurrentDraftRule()
                                evaluationResult = NotificationRuleEvaluator.evaluateNotificationText(
                                    rule = tempRule,
                                    fullNotificationText = sampleNotificationText
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Probar lectura")
                        }

                        // Tarjeta de Resultados en tiempo real
                        AnimatedVisibility(visible = evaluationResult != null) {
                            evaluationResult?.let { result ->
                                PlaygroundResultCard(
                                    result = result,
                                    currencyFormatter = currencyFormatter,
                                    onSaveRuleChanges = {
                                        coroutineScope.launch {
                                            val updatedRule = buildCurrentDraftRule()
                                            if (updatedRule.id == 0L) {
                                                val newId = bankRuleDao.insertRule(updatedRule)
                                                selectedRuleId = newId
                                            } else {
                                                bankRuleDao.updateRule(updatedRule)
                                            }
                                            snackbarHostState.showSnackbar(
                                                "Cambios guardados en la regla '${updatedRule.bankName}'"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showRuleModal && ruleModalTarget != null) {
        RuleEditDialog(
            initialRule = ruleModalTarget!!,
            onDismiss = { showRuleModal = false },
            onConfirmSave = { savedRule ->
                showRuleModal = false
                coroutineScope.launch {
                    if (savedRule.id == 0L) {
                        val insertedId = bankRuleDao.insertRule(savedRule)
                        selectRuleForPlayground(savedRule.copy(id = insertedId))
                        snackbarHostState.showSnackbar("Nueva regla '${savedRule.bankName}' agregada")
                    } else {
                        bankRuleDao.updateRule(savedRule)
                        selectRuleForPlayground(savedRule)
                        snackbarHostState.showSnackbar("Regla '${savedRule.bankName}' actualizada")
                    }
                }
            }
        )
    }
}

@Composable
private fun BankRuleItemCard(
    rule: BankRuleEntity,
    isSelectedInPlayground: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onLoadInPlayground: () -> Unit,
    onDeleteClick: () -> Unit
) {
    OutlinedCard(
        onClick = onLoadInPlayground,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelectedInPlayground) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (rule.isEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = rule.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filtro: ${rule.packageOrTitleFilter}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Text(
                text = "Positivas: ${rule.positiveKeywords}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            Text(
                text = "Negativas: ${rule.negativeKeywords}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onLoadInPlayground) {
                    Text("Probar en Sandbox")
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar regla")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar regla",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaygroundResultCard(
    result: RuleEvaluationResult,
    currencyFormatter: NumberFormat,
    onSaveRuleChanges: () -> Unit
) {
    val containerColor = if (result.isMatched) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (result.isMatched) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.isMatched) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.isMatched) {
                        "Estado: VÁLIDA (Coincidencia exitosa)"
                    } else {
                        "Estado: DESCARTADA"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (result.isMatched && result.extractedAmount != null) {
                Text(
                    text = currencyFormatter.format(result.extractedAmount),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Comercio / Concepto extraído: ${result.extractedConcept ?: result.ruleName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                result.matchedPositiveKeyword?.let { pos ->
                    Text(
                        text = "Palabra clave detectada: '$pos'",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Text(
                    text = result.discardReason ?: "La notificación no cumplió con las condiciones de la regla.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = onSaveRuleChanges,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar cambios de la regla")
            }
        }
    }
}

@Composable
private fun RuleEditDialog(
    initialRule: BankRuleEntity,
    onDismiss: () -> Unit,
    onConfirmSave: (BankRuleEntity) -> Unit
) {
    var bankName by rememberSaveable { mutableStateOf(initialRule.bankName) }
    var filter by rememberSaveable { mutableStateOf(initialRule.packageOrTitleFilter) }
    var posKeywords by rememberSaveable { mutableStateOf(initialRule.positiveKeywords) }
    var negKeywords by rememberSaveable { mutableStateOf(initialRule.negativeKeywords) }
    var amountRegex by rememberSaveable { mutableStateOf(initialRule.amountRegex) }
    var merchantRegex by rememberSaveable { mutableStateOf(initialRule.merchantRegex.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRule.id == 0L) "Nueva Regla Bancaria" else "Editar Regla: ${initialRule.bankName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Nombre del Banco") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Filtro de Paquete o Título") },
                    placeholder = { Text("Ej: com.bbva.app, bbva") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = posKeywords,
                    onValueChange = { posKeywords = it },
                    label = { Text("Palabras Positivas (separadas por coma)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = negKeywords,
                    onValueChange = { negKeywords = it },
                    label = { Text("Palabras Negativas (separadas por coma)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountRegex,
                    onValueChange = { amountRegex = it },
                    label = { Text("Regex de Monto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchantRegex,
                    onValueChange = { merchantRegex = it },
                    label = { Text("Regex de Comercio (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanedName = bankName.trim().ifEmpty { "Banco" }
                    onConfirmSave(
                        initialRule.copy(
                            bankName = cleanedName,
                            packageOrTitleFilter = filter.trim().ifEmpty { cleanedName.lowercase(Locale.ROOT) },
                            positiveKeywords = posKeywords.trim().ifEmpty { BankRuleEntity.DEFAULT_POSITIVE_KEYWORDS },
                            negativeKeywords = negKeywords.trim().ifEmpty { BankRuleEntity.DEFAULT_NEGATIVE_KEYWORDS },
                            amountRegex = amountRegex.trim().ifEmpty { BankRuleEntity.DEFAULT_AMOUNT_REGEX },
                            merchantRegex = merchantRegex.trim().ifEmpty { null }
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}