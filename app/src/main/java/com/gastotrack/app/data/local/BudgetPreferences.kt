package com.gastotrack.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

// Extensión singleton de DataStore con migración automática desde SharedPreferences
val Context.budgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = BudgetPreferences.PREFS_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = BudgetPreferences.PREFS_NAME
            )
        )
    }
)

/**
 * Gestor reactivo y asíncrono de preferencias de presupuesto migrado a Jetpack DataStore Preferences.
 *
 * MIGRACIÓN TRANSPARENTE:
 * Utiliza [SharedPreferencesMigration] para importar automáticamente cualquier valor guardado
 * previamente en SharedPreferences sin pérdida de datos del usuario, eliminando bloqueos en el hilo UI.
 */
class BudgetPreferences(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val dataStore = context.applicationContext.budgetDataStore

    // Flow con manejo seguro de excepciones de I/O
    private val safePreferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    /**
     * Flujo reactivo del presupuesto semanal.
     * Lee primero la clave Double moderna; si vino de SharedPreferences (donde era Float), hace fallback al Float.
     */
    val weeklyBudgetFlow: Flow<Double> = safePreferencesFlow.map { prefs ->
        prefs[KEY_WEEKLY_DOUBLE]
            ?: prefs[KEY_WEEKLY_FLOAT]?.toDouble()
            ?: DEFAULT_WEEKLY_BUDGET
    }

    /**
     * Flujo reactivo del presupuesto mensual.
     */
    val monthlyBudgetFlow: Flow<Double> = safePreferencesFlow.map { prefs ->
        prefs[KEY_MONTHLY_DOUBLE]
            ?: prefs[KEY_MONTHLY_FLOAT]?.toDouble()
            ?: DEFAULT_MONTHLY_BUDGET
    }

    /**
     * StateFlows para consumo reactivo directo en Jetpack Compose.
     */
    val weeklyBudget: StateFlow<Double> = weeklyBudgetFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = DEFAULT_WEEKLY_BUDGET
    )

    val monthlyBudget: StateFlow<Double> = monthlyBudgetFlow.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = DEFAULT_MONTHLY_BUDGET
    )

    /**
     * Actualización asíncrona suspendida del presupuesto semanal.
     */
    suspend fun updateWeeklyBudget(amount: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_WEEKLY_DOUBLE] = amount
            // Limpia la clave float heredada si existía
            prefs.remove(KEY_WEEKLY_FLOAT)
        }
    }

    /**
     * Actualización asíncrona suspendida del presupuesto mensual.
     */
    suspend fun updateMonthlyBudget(amount: Double) {
        dataStore.edit { prefs ->
            prefs[KEY_MONTHLY_DOUBLE] = amount
            prefs.remove(KEY_MONTHLY_FLOAT)
        }
    }

    /**
     * Métodos puente síncronos compatibles con las llamadas existentes en la UI.
     */
    fun setWeeklyBudget(amount: Double) {
        scope.launch {
            updateWeeklyBudget(amount)
        }
    }

    fun setMonthlyBudget(amount: Double) {
        scope.launch {
            updateMonthlyBudget(amount)
        }
    }

    companion object {
        const val PREFS_NAME = "gastotrack_budget_prefs"

        // Claves heredadas de SharedPreferences (guardadas como Float en v1)
        val KEY_WEEKLY_FLOAT = floatPreferencesKey("pref_weekly_budget")
        val KEY_MONTHLY_FLOAT = floatPreferencesKey("pref_monthly_budget")

        // Claves modernas de DataStore (Double con alta precisión)
        val KEY_WEEKLY_DOUBLE = doublePreferencesKey("pref_weekly_budget_double")
        val KEY_MONTHLY_DOUBLE = doublePreferencesKey("pref_monthly_budget_double")

        const val DEFAULT_WEEKLY_BUDGET = 2500.0
        const val DEFAULT_MONTHLY_BUDGET = 10000.0
    }
}
