package com.gastotrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gastotrack.app.data.local.entities.BankRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankRuleDao {

    @Query("SELECT * FROM bank_rules ORDER BY bankName ASC")
    fun observeAllRules(): Flow<List<BankRuleEntity>>

    @Query("SELECT * FROM bank_rules WHERE isEnabled = 1 ORDER BY id ASC")
    suspend fun getActiveRulesSync(): List<BankRuleEntity>

    @Query("SELECT COUNT(id) FROM bank_rules")
    suspend fun getRulesCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: BankRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllRules(rules: List<BankRuleEntity>)

    @Update
    suspend fun updateRule(rule: BankRuleEntity)

    @Delete
    suspend fun deleteRule(rule: BankRuleEntity)

    @Query("UPDATE bank_rules SET isEnabled = :isEnabled WHERE id = :ruleId")
    suspend fun updateRuleEnabledState(ruleId: Long, isEnabled: Boolean)
}