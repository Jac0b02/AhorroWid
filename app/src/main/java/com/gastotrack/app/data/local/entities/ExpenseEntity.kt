package com.gastotrack.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["source", "timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "source")
    val source: String, // "CARD" o "CASH"

    @ColumnInfo(name = "concept")
    val concept: String? = null,

    @ColumnInfo(name = "bankName")
    val bankName: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_CARD = "CARD"
        const val SOURCE_CASH = "CASH"
    }
}