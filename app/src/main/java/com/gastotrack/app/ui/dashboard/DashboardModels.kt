package com.gastotrack.app.ui.dashboard

import java.text.NumberFormat
import java.time.DayOfWeek
import java.util.Locale

enum class DashboardPeriod(val title: String, val subtitle: String) {
    WEEK("Esta Semana", "Lun - Dom"),
    MONTH("Este Mes", "Mes Actual")
}

data class DailySpendData(
    val dayOfWeek: DayOfWeek,
    val shortLabel: String,
    val fullLabel: String,
    val expenseTotal: Double,
    val incomeTotal: Double = 0.0,
    val expenseCount: Int,
    val incomeCount: Int = 0,
    val isToday: Boolean
) {
    val total: Double get() = expenseTotal
    val count: Int get() = expenseCount
    val netBalance: Double get() = incomeTotal - expenseTotal
}

data class WeeklyBucketData(
    val weekNumber: Int,
    val label: String,
    val expenseTotal: Double,
    val incomeTotal: Double = 0.0,
    val expenseCount: Int,
    val incomeCount: Int = 0
) {
    val total: Double get() = expenseTotal
    val count: Int get() = expenseCount
    val netBalance: Double get() = incomeTotal - expenseTotal
}

object CurrencyUtils {
    private val mxLocale = Locale("es", "MX")

    fun format(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(mxLocale)
        format.maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        return format.format(amount)
    }

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000 -> "$${String.format(Locale.US, "%.1f", amount / 1_000_000)}M"
            amount >= 10_000 -> "$${(amount / 1000).toInt()}k"
            amount >= 1_000 -> "$${String.format(Locale.US, "%.1f", amount / 1000)}k"
            else -> format(amount)
        }
    }
}
