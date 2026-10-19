package com.gastotrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gastotrack.app.data.local.entities.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: IncomeEntity): Long

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM incomes
        WHERE timestamp >= :startMillis
        """
    )
    fun observeIncomeTotalSince(startMillis: Long): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM incomes
        WHERE timestamp >= :startMillis
        """
    )
    suspend fun getIncomeTotalSinceSync(startMillis: Long): Double

    @Query(
        """
        SELECT * FROM incomes
        WHERE timestamp >= :startMillis
        ORDER BY timestamp DESC
        """
    )
    fun observeIncomesSince(startMillis: Long): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentIncomes(limit: Int = 100): Flow<List<IncomeEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0.0)
        FROM incomes
        WHERE timestamp >= :startMillis AND timestamp <= :endMillis
        """
    )
    fun observeIncomeTotalBetween(startMillis: Long, endMillis: Long): Flow<Double>

    @Query(
        """
        SELECT * FROM incomes
        WHERE timestamp >= :startMillis AND timestamp <= :endMillis
        ORDER BY timestamp DESC
        """
    )
    fun observeIncomesBetween(startMillis: Long, endMillis: Long): Flow<List<IncomeEntity>>
}
