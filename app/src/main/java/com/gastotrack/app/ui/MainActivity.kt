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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.gastotrack.app.data.local.BudgetPreferences
import com.gastotrack.app.ui.dashboard.BudgetDialog
import com.gastotrack.app.ui.dashboard.BudgetHealthCard
import com.gastotrack.app.ui.dashboard.CurrencyUtils
import com.gastotrack.app.ui.dashboard.DashboardPeriod
import com.gastotrack.app.ui.dashboard.PeriodSelector
import com.gastotrack.app.ui.dashboard.WeeklyBarChartCard
import com.gastotrack.app.ui.theme.GastoTrackTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.lifecycle.lifecycleScope
import com.gastotrack.app.data.local.entities.IncomeEntity
import com.gastotrack.app.ui.dashboard.BalanceComparisonCard
import com.gastotrack.app.ui.dashboard.QuickIncomeEntryDialog
import com.gastotrack.app.ui.theme.PixelCardShape
import com.gastotrack.app.ui.theme.PixelChipShape
import com.gastotrack.app.ui.theme.PixelDialogShape
import com.gastotrack.app.ui.theme.PixelFabShape
import com.gastotrack.app.ui.theme.PixelInputShape
import com.gastotrack.app.ui.theme.PixelPillShape
import com.gastotrack.app.widget.ExpenseGlanceWidget
import com.gastotrack.app.worker.WeeklySummaryWorker
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableIntStateOf

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
            // Material You: Tema dinámico de Google Pixel adaptativo a fondo y modo oscuro del sistema
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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                ExpenseGlanceWidget().updateAll(applicationContext)
            } catch (_: Exception) {}
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            try {
                ExpenseGlanceWidget().updateAll(applicationContext)
            } catch (_: Exception) {}
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

    // Solicitud de permiso de notificaciones push para Android 13+ (Pixel Notification Permission)
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

    var currentPeriod by rememberSaveable { mutableStateOf(DashboardPeriod.WEEK) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }

    val budgetPrefs = remember { BudgetPreferences(context) }
    val weeklyBudget by budgetPrefs.weeklyBudget.collectAsState()
    val monthlyBudget by budgetPrefs.monthlyBudget.collectAsState()
    val activeBudgetGoal = if (currentPeriod == DashboardPeriod.WEEK) weeklyBudget else monthlyBudget

    val thirtyDaysAgo = remember { AppDatabase.getThirtyDaysAgoMillis() }

    // Rango activo calculado según el período y el offset histórico
    val (activeStartMillis, activeEndMillis) = remember(currentPeriod, weekOffset, monthOffset) {
        if (currentPeriod == DashboardPeriod.WEEK) {
            AppDatabase.getWeekRangeMillis(weekOffset)
        } else {
            AppDatabase.getMonthRangeMillis(monthOffset)
        }
    }

    val isCurrentPeriod = if (currentPeriod == DashboardPeriod.WEEK) weekOffset == 0 else monthOffset == 0
    val hasNext = if (currentPeriod == DashboardPeriod.WEEK) weekOffset < 0 else monthOffset < 0

    // Etiqueta legible del período navegado
    val periodLabel = remember(currentPeriod, weekOffset, monthOffset, activeStartMillis, activeEndMillis) {
        val zoneId = ZoneId.systemDefault()
        if (currentPeriod == DashboardPeriod.WEEK) {
            val startLocal = Instant.ofEpochMilli(activeStartMillis).atZone(zoneId).toLocalDate()
            val endLocal = Instant.ofEpochMilli(activeEndMillis).atZone(zoneId).toLocalDate()
            val dtfDay = DateTimeFormatter.ofPattern("d MMM", Locale("es", "MX"))
            when (weekOffset) {
                0 -> "Esta Semana (${startLocal.format(dtfDay)} - ${endLocal.format(dtfDay)})"
                -1 -> "Semana Pasada (${startLocal.format(dtfDay)} - ${endLocal.format(dtfDay)})"
                else -> "${startLocal.format(dtfDay)} - ${endLocal.format(dtfDay)}"
            }
        } else {
            val monthLocal = Instant.ofEpochMilli(activeStartMillis).atZone(zoneId).toLocalDate()
            val dtfMonth = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "MX"))
            val formatted = monthLocal.format(dtfMonth)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }
            if (monthOffset == 0) "Este Mes ($formatted)" else formatted
        }
    }

    val activeSummary by database.expenseDao()
        .observeSummaryBetween(activeStartMillis, activeEndMillis)
        .collectAsState(
            initial = WeeklyExpenseSummary(total = 0.0, tarjeta = 0.0, efectivo = 0.0, txCount = 0)
        )

    val activeIncomeTotal by database.incomeDao()
        .observeIncomeTotalBetween(activeStartMillis, activeEndMillis)
        .collectAsState(initial = 0.0)

    val periodExpenses by database.expenseDao()
        .observeExpensesBetween(activeStartMillis, activeEndMillis)
        .collectAsState(initial = emptyList())

    val periodIncomes by database.incomeDao()
        .observeIncomesBetween(activeStartMillis, activeEndMillis)
        .collectAsState(initial = emptyList())

    var showQuickIncomeDialog by rememberSaveable { mutableStateOf(false) }

    val topFrequentCash by database.expenseDao()
        .getTopFrequentCashExpenses(thirtyDaysAgo)
        .collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) "GastoTrack" else "Reglas y Sandbox Bancario",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedTab == 0) {
                            Box(
                                modifier = Modifier
                                    .clip(PixelPillShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (currentPeriod == DashboardPeriod.WEEK) "Semanal" else "Mensual",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { showQuickIncomeDialog = true },
                        shape = PixelFabShape,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ Ingreso",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    ExtendedFloatingActionButton(
                        onClick = { showQuickCashDialog = true },
                        shape = PixelFabShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = {
                            Text(
                                text = "Gasto en Efectivo",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = {
                        Text(
                            text = "Resumen",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = {
                        Text(
                            text = "Bancos y Sandbox",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
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
                period = currentPeriod,
                onPeriodSelected = { currentPeriod = it },
                periodLabel = periodLabel,
                hasPrevious = true,
                hasNext = hasNext,
                onPreviousClick = {
                    if (currentPeriod == DashboardPeriod.WEEK) weekOffset-- else monthOffset--
                },
                onNextClick = {
                    if (currentPeriod == DashboardPeriod.WEEK) {
                        weekOffset = minOf(0, weekOffset + 1)
                    } else {
                        monthOffset = minOf(0, monthOffset + 1)
                    }
                },
                onResetToCurrent = {
                    if (currentPeriod == DashboardPeriod.WEEK) weekOffset = 0 else monthOffset = 0
                },
                isCurrentPeriod = isCurrentPeriod,
                summary = activeSummary,
                budgetGoal = activeBudgetGoal,
                onAdjustBudgetClick = { showBudgetDialog = true },
                periodExpenses = periodExpenses,
                frequentCashList = topFrequentCash,
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
                },
                incomeTotal = activeIncomeTotal,
                periodIncomes = periodIncomes,
                onAddIncomeClick = { showQuickIncomeDialog = true },
                onDeleteIncome = { income ->
                    coroutineScope.launch {
                        database.incomeDao().deleteIncome(income)
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

    if (showQuickIncomeDialog) {
        QuickIncomeEntryDialog(
            onDismiss = { showQuickIncomeDialog = false },
            onConfirmIncome = { amount, concept ->
                showQuickIncomeDialog = false
                coroutineScope.launch {
                    database.incomeDao().insertIncome(
                        IncomeEntity(
                            amount = amount,
                            concept = concept
                        )
                    )
                }
            }
        )
    }

    if (showBudgetDialog) {
        BudgetDialog(
            period = currentPeriod,
            currentBudget = activeBudgetGoal,
            onDismiss = { showBudgetDialog = false },
            onConfirmBudget = { newBudget ->
                showBudgetDialog = false
                if (currentPeriod == DashboardPeriod.WEEK) {
                    budgetPrefs.setWeeklyBudget(newBudget)
                } else {
                    budgetPrefs.setMonthlyBudget(newBudget)
                }
            }
        )
    }
}

private sealed class DashboardMovement(val timestamp: Long, val key: String) {
    data class Expense(val expense: ExpenseEntity) : DashboardMovement(expense.timestamp, "exp_${expense.id}")
    data class Income(val income: IncomeEntity) : DashboardMovement(income.timestamp, "inc_${income.id}")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardScreenContent(
    modifier: Modifier = Modifier,
    isListenerEnabled: Boolean,
    onOpenListenerSettings: () -> Unit,
    period: DashboardPeriod,
    onPeriodSelected: (DashboardPeriod) -> Unit,
    periodLabel: String,
    hasPrevious: Boolean = true,
    hasNext: Boolean = false,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onResetToCurrent: () -> Unit,
    isCurrentPeriod: Boolean,
    summary: WeeklyExpenseSummary,
    budgetGoal: Double,
    onAdjustBudgetClick: () -> Unit,
    periodExpenses: List<ExpenseEntity>,
    frequentCashList: List<FrequentCashExpense>,
    onQuickCashClick: (Double, String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    incomeTotal: Double,
    periodIncomes: List<IncomeEntity>,
    onAddIncomeClick: () -> Unit,
    onDeleteIncome: (IncomeEntity) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("es", "MX")) }
    var movementFilter by rememberSaveable { mutableIntStateOf(0) } // 0: Todos, 1: Gastos, 2: Ingresos

    val filteredMovements = remember(periodExpenses, periodIncomes, movementFilter) {
        val list = mutableListOf<DashboardMovement>()
        if (movementFilter == 0 || movementFilter == 1) {
            list.addAll(periodExpenses.map { DashboardMovement.Expense(it) })
        }
        if (movementFilter == 0 || movementFilter == 2) {
            list.addAll(periodIncomes.map { DashboardMovement.Income(it) })
        }
        list.sortedByDescending { it.timestamp }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isListenerEnabled) {
            item {
                Card(
                    shape = PixelCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(PixelPillShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Lectura Automática Bancaria Desactivada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Activa el acceso a notificaciones para que GastoTrack registre tus compras con tarjeta automáticamente desde apps bancarias.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onOpenListenerSettings,
                            shape = PixelPillShape
                        ) {
                            Text("Conceder Permiso en Ajustes")
                        }
                    }
                }
            }
        }

        // Selector de Período (Esta Semana / Este Mes con Navegación Histórica)
        item {
            PeriodSelector(
                selectedPeriod = period,
                onPeriodSelected = onPeriodSelected,
                periodLabel = periodLabel,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onResetToCurrent = onResetToCurrent,
                isCurrentPeriod = isCurrentPeriod
            )
        }

        // MEJORA 1: Balance y Ganancias vs Pérdidas de la Semana / Mes
        item {
            BalanceComparisonCard(
                period = period,
                incomeTotal = incomeTotal,
                expenseTotal = summary.total,
                incomeCount = periodIncomes.size,
                expenseCount = summary.txCount,
                onAddIncomeClick = onAddIncomeClick
            )
        }

        // Tarjeta Principal Hero de Resumen del Período
        item {
            Card(
                shape = PixelCardShape,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (period == DashboardPeriod.WEEK) "Gasto Total de la Semana" else "Gasto Total del Mes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = CurrencyUtils.format(summary.total),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Barra segmentada proporcional con esquinas redondeadas Pixel
                    val total = summary.total
                    val tarjetaRatio = if (total > 0) (summary.tarjeta / total).toFloat().coerceIn(0f, 1f) else 0.5f
                    val efectivoRatio = 1f - tarjetaRatio

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(PixelPillShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            if (tarjetaRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(tarjetaRatio.coerceAtLeast(0.01f))
                                        .height(12.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            if (efectivoRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(efectivoRatio.coerceAtLeast(0.01f))
                                        .height(12.dp)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Badge Tarjeta
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PixelPillShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Column {
                                Text(
                                    text = "Tarjeta",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyUtils.format(summary.tarjeta),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Badge Efectivo
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(PixelPillShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                            Column {
                                Text(
                                    text = "Efectivo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyUtils.format(summary.efectivo),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Fila de conteo y promedio diario
                    val daysDivisor = if (period == DashboardPeriod.WEEK) 7 else 30
                    val dailyAverage = if (summary.total > 0.0) summary.total / daysDivisor else 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PixelPillShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${summary.txCount} movimientos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Promedio: ${CurrencyUtils.format(dailyAverage)}/día",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Pilar 2: Presupuesto Objetivo y Semáforo de Salud
        item {
            BudgetHealthCard(
                period = period,
                spentAmount = summary.total,
                budgetGoal = budgetGoal,
                onAdjustBudgetClick = onAdjustBudgetClick
            )
        }

        // Pilar 2: Gráfico Interactivo de Barras Dobles (Ingresos vs Gastos)
        item {
            WeeklyBarChartCard(
                period = period,
                expenses = periodExpenses,
                incomes = periodIncomes
            )
        }

        // Sugerencias Rápidas en Efectivo
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Registro Rápido de Efectivo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val chips = frequentCashList.ifEmpty {
                    listOf(
                        FrequentCashExpense(50.0, "Café", 1, 0L),
                        FrequentCashExpense(73.0, "Comida", 1, 0L),
                        FrequentCashExpense(100.0, "Transporte", 1, 0L)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { item ->
                        val chipIcon = when {
                            item.concept.contains("café", ignoreCase = true) -> Icons.Default.LocalCafe
                            item.concept.contains("comida", ignoreCase = true) || item.concept.contains("almuerzo", ignoreCase = true) -> Icons.Default.Restaurant
                            item.concept.contains("transporte", ignoreCase = true) || item.concept.contains("taxi", ignoreCase = true) -> Icons.Default.DirectionsBus
                            else -> Icons.Default.Payments
                        }

                        AssistChip(
                            onClick = { onQuickCashClick(item.amount, item.concept) },
                            shape = PixelPillShape,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = chipIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = {
                                Text(
                                    text = "+${CurrencyUtils.format(item.amount)} ${item.concept}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        )
                    }
                }
            }
        }

        // Movimientos del Período
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Movimientos ($periodLabel)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(PixelPillShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${periodExpenses.size + periodIncomes.size} registros",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chips para filtrar movimientos
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = movementFilter == 0,
                        onClick = { movementFilter = 0 },
                        shape = PixelPillShape,
                        label = { Text("Todos (${periodExpenses.size + periodIncomes.size})") }
                    )
                    FilterChip(
                        selected = movementFilter == 1,
                        onClick = { movementFilter = 1 },
                        shape = PixelPillShape,
                        label = { Text("Gastos (${periodExpenses.size})") }
                    )
                    FilterChip(
                        selected = movementFilter == 2,
                        onClick = { movementFilter = 2 },
                        shape = PixelPillShape,
                        label = { Text("Ingresos (${periodIncomes.size})") }
                    )
                }
            }
        }

        if (filteredMovements.isEmpty()) {
            item {
                Card(
                    shape = PixelCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (movementFilter) {
                            1 -> "Aún no hay gastos registrados en este período."
                            2 -> "Aún no hay ingresos registrados en este período. Presiona '+ Ingreso' para añadir el dinero que obtuviste."
                            else -> "Aún no hay movimientos registrados en este período. Registra un ingreso con '+ Ingreso' o un gasto con 'Gasto en Efectivo'."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(filteredMovements, key = { it.key }) { movement ->
                when (movement) {
                    is DashboardMovement.Expense -> {
                        val expense = movement.expense
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (expense.source == ExpenseEntity.SOURCE_CARD) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (expense.source == ExpenseEntity.SOURCE_CARD) {
                                                Icons.Default.CreditCard
                                            } else {
                                                Icons.Default.Payments
                                            },
                                            contentDescription = null,
                                            tint = if (expense.source == ExpenseEntity.SOURCE_CARD) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = expense.concept ?: expense.bankName ?: "Gasto",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
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
                                        text = "-${CurrencyUtils.format(expense.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(onClick = { onDeleteExpense(expense) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar gasto",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is DashboardMovement.Income -> {
                        val income = movement.income
                        val isDark = isSystemInDarkTheme()
                        val incomeColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                        val incomeContainerColor = if (isDark) Color(0xFF1E3924) else Color(0xFFE8F5E9)

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(incomeContainerColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = incomeColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = income.concept,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Ingreso • ${dateFormat.format(Date(income.timestamp))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+${CurrencyUtils.format(income.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = incomeColor
                                    )
                                    IconButton(onClick = { onDeleteIncome(income) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Eliminar ingreso",
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
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
        shape = PixelDialogShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Registrar Gasto en Efectivo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    shape = PixelInputShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = conceptInput,
                    onValueChange = { conceptInput = it },
                    label = { Text("Concepto (Opcional)") },
                    placeholder = { Text("Ej: Comida, Taxi, Café") },
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
                        onConfirmExpense(parsed, conceptInput.trim().ifEmpty { "Efectivo" })
                    }
                },
                shape = PixelPillShape
            ) {
                Text(
                    text = "Guardar Gasto",
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