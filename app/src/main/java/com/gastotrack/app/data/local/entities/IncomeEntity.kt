package com.gastotrack.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "incomes",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "concept")
    val concept: String = DEFAULT_CONCEPT,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "category", defaultValue = "'General'")
    val category: String = DEFAULT_CATEGORY,

    @ColumnInfo(name = "notes", defaultValue = "NULL")
    val notes: String? = null
) {
    companion object {
        const val DEFAULT_CONCEPT = "Ingreso"
        const val DEFAULT_CATEGORY = "General"
    }
}
