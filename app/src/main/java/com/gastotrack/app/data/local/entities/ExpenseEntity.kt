package com.gastotrack.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["source", "timestamp"]),
        Index(value = ["category"])
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
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "category", defaultValue = "'General'")
    val category: String = DEFAULT_CATEGORY,

    @ColumnInfo(name = "notes", defaultValue = "NULL")
    val notes: String? = null
) {
    companion object {
        const val SOURCE_CARD = "CARD"
        const val SOURCE_CASH = "CASH"
        const val DEFAULT_CATEGORY = "General"
    }
}