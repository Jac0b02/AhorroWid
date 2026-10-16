package com.gastotrack.app.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gestor de preferencias de presupuesto semanal y mensual persistidos en SharedPreferences.
 */
class BudgetPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _weeklyBudget = MutableStateFlow(getWeeklyBudget())
    val weeklyBudget: StateFlow<Double> = _weeklyBudget.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(getMonthlyBudget())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    fun getWeeklyBudget(): Double {
        return prefs.getFloat(KEY_WEEKLY_BUDGET, DEFAULT_WEEKLY_BUDGET.toFloat()).toDouble()
    }

    fun setWeeklyBudget(amount: Double) {
        prefs.edit().putFloat(KEY_WEEKLY_BUDGET, amount.toFloat()).apply()
        _weeklyBudget.value = amount
    }

    fun getMonthlyBudget(): Double {
        return prefs.getFloat(KEY_MONTHLY_BUDGET, DEFAULT_MONTHLY_BUDGET.toFloat()).toDouble()
    }

    fun setMonthlyBudget(amount: Double) {
        prefs.edit().putFloat(KEY_MONTHLY_BUDGET, amount.toFloat()).apply()
        _monthlyBudget.value = amount
    }

    companion object {
        private const val PREFS_NAME = "gastotrack_budget_prefs"
        private const val KEY_WEEKLY_BUDGET = "pref_weekly_budget"
        private const val KEY_MONTHLY_BUDGET = "pref_monthly_budget"

        const val DEFAULT_WEEKLY_BUDGET = 2500.0
        const val DEFAULT_MONTHLY_BUDGET = 10000.0
    }
}
