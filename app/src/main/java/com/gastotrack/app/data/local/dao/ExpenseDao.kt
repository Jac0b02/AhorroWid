package com.gastotrack.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gastotrack.app.data.local.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

data class WeeklyExpenseSummary(
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "tarjeta") val tarjeta: Double,
    @ColumnInfo(name = "efectivo") val efectivo: Double,
    @ColumnInfo(name = "txCount") val txCount: Int
)

data class FrequentCashExpense(
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "concept") val concept: String,
    @ColumnInfo(name = "frequency") val frequency: Int,
    @ColumnInfo(name = "lastUsed") val lastUsed: Long
)

data class CategoryExpenseSummary(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "count") val count: Int
)

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query(
        """
        SELECT 
            COALESCE(SUM(amount), 0.0) AS total,
            COALESCE(SUM(CASE WHEN source = 'CARD' THEN amount ELSE 0.0 END), 0.0) AS tarjeta,
            COALESCE(SUM(CASE WHEN source = 'CASH' THEN amount ELSE 0.0 END), 0.0) AS efectivo,
            COUNT(id) AS txCount
        FROM expenses
        WHERE timestamp >= :mondayStartMillis
        """
    )
    fun observeWeeklySummary(mondayStartMillis: Long): Flow<WeeklyExpenseSummary>

    @Query(
        """
        SELECT 
            COALESCE(SUM(amount), 0.0) AS total,
            COALESCE(SUM(CASE WHEN source = 'CARD' THEN amount ELSE 0.0 END), 0.0) AS tarjeta,
            COALESCE(SUM(CASE WHEN source = 'CASH' THEN amount ELSE 0.0 END), 0.0) AS efectivo,
            COUNT(id) AS txCount
        FROM expenses
        WHERE timestamp >= :mondayStartMillis
        """
    )
    suspend fun getWeeklySummarySync(mondayStartMillis: Long): WeeklyExpenseSummary

    @Query(
        """
        SELECT 
            amount,
            COALESCE(NULLIF(TRIM(concept), ''), 'Efectivo') AS concept,
            COUNT(id) AS frequency,
            MAX(timestamp) AS lastUsed
        FROM expenses
        WHERE source = 'CASH'
          AND timestamp >= :thirtyDaysAgoMillis
        GROUP BY amount, COALESCE(NULLIF(TRIM(concept), ''), 'Efectivo')
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT 3
        """
    )
    fun getTopFrequentCashExpenses(thirtyDaysAgoMillis: Long): Flow<List<FrequentCashExpense>>

    @Query(
        """
        SELECT 
            amount,
            COALESCE(NULLIF(TRIM(concept), ''), 'Efectivo') AS concept,
            COUNT(id) AS frequency,
            MAX(timestamp) AS lastUsed
        FROM expenses
        WHERE source = 'CASH'
          AND timestamp >= :thirtyDaysAgoMillis
        GROUP BY amount, COALESCE(NULLIF(TRIM(concept), ''), 'Efectivo')
        ORDER BY frequency DESC, lastUsed DESC
        LIMIT 3
        """
    )
    suspend fun getTopFrequentCashExpensesSync(thirtyDaysAgoMillis: Long): List<FrequentCashExpense>

    @Query(
        """
        SELECT * FROM expenses
        WHERE amount = :amount
          AND COALESCE(concept, '') = COALESCE(:concept, '')
          AND timestamp >= :minTimestamp
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        amount: Double,
        concept: String?,
        minTimestamp: Long
    ): ExpenseEntity?

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentExpenses(limit: Int = 100): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT 
            COALESCE(SUM(amount), 0.0) AS total,
            COALESCE(SUM(CASE WHEN source = 'CARD' THEN amount ELSE 0.0 END), 0.0) AS tarjeta,
            COALESCE(SUM(CASE WHEN source = 'CASH' THEN amount ELSE 0.0 END), 0.0) AS efectivo,
            COUNT(id) AS txCount
        FROM expenses
        WHERE timestamp >= :startMillis
        """
    )
    fun observeSummarySince(startMillis: Long): Flow<WeeklyExpenseSummary>

    @Query(
        """
        SELECT * FROM expenses
        WHERE timestamp >= :startMillis
        ORDER BY timestamp DESC
        """
    )
    fun observeExpensesSince(startMillis: Long): Flow<List<ExpenseEntity>>

    @Query(
        """
        SELECT 
            category,
            COALESCE(SUM(amount), 0.0) AS total,
            COUNT(id) AS count
        FROM expenses
        WHERE timestamp >= :startMillis
        GROUP BY category
        ORDER BY total DESC
        """
    )
    fun observeCategorySummariesSince(startMillis: Long): Flow<List<CategoryExpenseSummary>>

    @Query(
        """
        SELECT 
            COALESCE(SUM(amount), 0.0) AS total,
            COALESCE(SUM(CASE WHEN source = 'CARD' THEN amount ELSE 0.0 END), 0.0) AS tarjeta,
            COALESCE(SUM(CASE WHEN source = 'CASH' THEN amount ELSE 0.0 END), 0.0) AS efectivo,
            COUNT(id) AS txCount
        FROM expenses
        WHERE timestamp >= :startMillis AND timestamp <= :endMillis
        """
    )
    fun observeSummaryBetween(startMillis: Long, endMillis: Long): Flow<WeeklyExpenseSummary>

    @Query(
        """
        SELECT * FROM expenses
        WHERE timestamp >= :startMillis AND timestamp <= :endMillis
        ORDER BY timestamp DESC
        """
    )
    fun observeExpensesBetween(startMillis: Long, endMillis: Long): Flow<List<ExpenseEntity>>
}