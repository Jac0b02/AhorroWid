package com.gastotrack.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.data.local.dao.FrequentCashExpense
import com.gastotrack.app.data.local.dao.WeeklyExpenseSummary
import com.gastotrack.app.data.local.entities.ExpenseEntity
import com.gastotrack.app.ui.settings.SettingsScreen
import com.gastotrack.app.widget.ExpenseGlanceWidget
import com.gastotrack.app.worker.WeeklySummaryWorker
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var openQuickCashDialogState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programar el Worker dominical de resumen semanal a las 20:00
        WeeklySummaryWorker.scheduleWeeklySummary(applicationContext)

        // Verificar si fue abierto desde el botón "+ Otro" del Widget
        handleQuickCashIntent(intent)

        val database = AppDatabase.getInstance(applicationContext)

        setContent {
            GastoTrackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GastoTrackMainApp(
                        database = database,
                        openQuickCashModalExternal = openQuickCashDialogState,
                        onConsumeQuickCashModalExternal = {
                            openQuickCashDialogState = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickCashIntent(intent)
    }

    private fun handleQuickCashIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_QUICK_CASH_DIALOG, false) == true) {
            openQuickCashDialogState = true
            intent.removeExtra(EXTRA_OPEN_QUICK_CASH_DIALOG)
        }
    }

    companion object {
        const val EXTRA_OPEN_QUICK_CASH_DIALOG = "extra_open_quick_cash_dialog"
    }
}

@Composable
fun GastoTrackTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GastoTrackMainApp(
    database: AppDatabase,
    openQuickCashModalExternal: Boolean,
    onConsumeQuickCashModalExternal: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showQuickCashDialog by rememberSaveable { mutableStateOf(false) }

    // Sincronizar disparo desde el botón "+ Otro" del Widget Glance
    LaunchedEffect(openQuickCashModalExternal) {
        if (openQuickCashModalExternal) {
            showQuickCashDialog = true
            onConsumeQuickCashModalExternal()
        }
    }

    // Solicitud de permiso de notificaciones push para Android 13+
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Verificar si el servicio de escucha de notificaciones bancarias está activo
    var isListenerPermissionGranted by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isListenerPermissionGranted = NotificationManagerCompat
                    .getEnabledListenerPackages(context)
                    .contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mondayStart = remember { AppDatabase.getStartOfCurrentWeekMondayMillis() }
    val thirtyDaysAgo = remember { AppDatabase.getThirtyDaysAgoMillis() }

    val weeklySummary by database.expenseDao()
        .observeWeeklySummary(mondayStart)
        .collectAsState(
            initial = WeeklyExpenseSummary(total = 0.0, tarjeta = 0.0, efectivo = 0.0, txCount = 0)
        )

    val topFrequentCash by database.expenseDao()
        .getTopFrequentCashExpenses(thirtyDaysAgo)
        .collectAsState(initial = emptyList())

    val recentExpenses by database.expenseDao()
        .observeRecentExpenses(limit = 50)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedTab == 0) "GastoTrack" else "Reglas y Sandbox Bancario",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showQuickCashDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Gasto en Efectivo") }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Resumen") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Bancos y Sandbox") }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            DashboardScreenContent(
                modifier = Modifier.padding(innerPadding),
                isListenerEnabled = isListenerPermissionGranted,
                onOpenListenerSettings = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                summary = weeklySummary,
                frequentCashList = topFrequentCash,
                recentExpenses = recentExpenses,
                onQuickCashClick = { amount, concept ->
                    coroutineScope.launch {
                        database.expenseDao().insertExpense(
                            ExpenseEntity(
                                amount = amount,
                                source = ExpenseEntity.SOURCE_CASH,
                                concept = concept,
                                bankName = "Efectivo"
                            )
                        )
                        ExpenseGlanceWidget().updateAll(context)
                    }
                },
                onDeleteExpense = { expense ->
                    coroutineScope.launch {
                        database.expenseDao().deleteExpense(expense)
                        ExpenseGlanceWidget().updateAll(context)
                    }
                }
            )
        } else {
            SettingsScreen(
                bankRuleDao = database.bankRuleDao(),
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (showQuickCashDialog) {
        QuickCashEntryDialog(
            onDismiss = { showQuickCashDialog = false },
            onConfirmExpense = { amount, concept ->
                showQuickCashDialog = false
                coroutineScope.launch {
                    database.expenseDao().insertExpense(
                        ExpenseEntity(
                            amount = amount,
                            source = ExpenseEntity.SOURCE_CASH,
                            concept = concept.ifBlank { "Efectivo" },
                            bankName = "Efectivo"
                        )
                    )
                    ExpenseGlanceWidget().updateAll(context)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardScreenContent(
    modifier: Modifier = Modifier,
    isListenerEnabled: Boolean,
    onOpenListenerSettings: () -> Unit,
    summary: WeeklyExpenseSummary,
    frequentCashList: List<FrequentCashExpense>,
    recentExpenses: List<ExpenseEntity>,
    onQuickCashClick: (Double, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    val currency = remember { NumberFormat.getCurrencyInstance(Locale("es", "MX")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("es", "MX")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isListenerEnabled) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lectura Automática de Bancos Desactivada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Activa el permiso de acceso a notificaciones para que GastoTrack registre tus compras con tarjeta automáticamente.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(onClick = onOpenListenerSettings) {
                            Text("Conceder Permiso")
                        }
                    }
                }
            }
        }

        // Tarjeta Principal de Resumen Semanal
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Balance Semanal (Desde el lunes)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currency.format(summary.total),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    val cardRatio = if (summary.total > 0) {
                        (summary.tarjeta / summary.total).toFloat().coerceIn(0f, 1f)
                    } else 0.5f

                    LinearProgressIndicator(
                        progress = { cardRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💳 Tarjeta: ${currency.format(summary.tarjeta)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "💵 Efectivo: ${currency.format(summary.efectivo)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Sugerencias frecuentes en efectivo (Últimos 30 días)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Sugerencias Rápidas en Efectivo (Top 3 últimos 30 días)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val chips = frequentCashList.ifEmpty {
                    listOf(
                        FrequentCashExpense(50.0, "Café", 1, 0L),
                        FrequentCashExpense(73.0, "Comida", 1, 0L),
                        FrequentCashExpense(100.0, "Transporte", 1, 0L)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    chips.forEach { item ->
                        AssistChip(
                            onClick = { onQuickCashClick(item.amount, item.concept) },
                            label = {
                                Text("+${currency.format(item.amount)} ${item.concept}")
                            }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Movimientos Recientes (${recentExpenses.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (recentExpenses.isEmpty()) {
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aún no hay gastos registrados esta semana. Usa los botones rápidos o prueba una notificación en la pestaña 'Bancos y Sandbox'.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentExpenses, key = { it.id }) { expense ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (expense.source == ExpenseEntity.SOURCE_CARD) {
                                    Icons.Default.CreditCard
                                } else {
                                    Icons.Default.Payments
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = expense.concept ?: expense.bankName ?: "Gasto",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${expense.bankName ?: expense.source} • ${dateFormat.format(Date(expense.timestamp))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currency.format(expense.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onDeleteExpense(expense) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Eliminar gasto"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun QuickCashEntryDialog(
    onDismiss: () -> Unit,
    onConfirmExpense: (Double, String) -> Unit
) {
    var amountInput by rememberSaveable { mutableStateOf("") }
    var conceptInput by rememberSaveable { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Gasto en Efectivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        amountError = false
                    },
                    label = { Text("Monto ($ MXN)") },
                    placeholder = { Text("Ej: 85.00") },
                    isError = amountError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conceptInput,
                    onValueChange = { conceptInput = it },
                    label = { Text("Concepto (Opcional)") },
                    placeholder = { Text("Ej: Comida, Taxi, Propina") },
                    singleLine = true,
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
                        onConfirmExpense(parsed, conceptInput.trim().ifEmpty { "Efectivo" })
                    }
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