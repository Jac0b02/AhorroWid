package com.gastotrack.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_rules")
data class BankRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "bankName")
    val bankName: String,

    @ColumnInfo(name = "packageOrTitleFilter")
    val packageOrTitleFilter: String,

    @ColumnInfo(name = "positiveKeywords")
    val positiveKeywords: String,

    @ColumnInfo(name = "negativeKeywords")
    val negativeKeywords: String,

    @ColumnInfo(name = "amountRegex")
    val amountRegex: String = DEFAULT_AMOUNT_REGEX,

    @ColumnInfo(name = "merchantRegex")
    val merchantRegex: String? = DEFAULT_MERCHANT_REGEX,

    @ColumnInfo(name = "isEnabled")
    val isEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_AMOUNT_REGEX = """(?:\$|MXN\s*)([\d,]+\.?\d{0,2})"""
        const val DEFAULT_MERCHANT_REGEX = """(?:en|por)\s+([A-Za-z0-9\s]+)"""
        const val DEFAULT_POSITIVE_KEYWORDS = "compra, cargo, retiro, transferencia enviada, pago realizado, consumiste"
        const val DEFAULT_NEGATIVE_KEYWORDS = "abono, depósito, recibiste, código, token, nip, promoción, estado de cuenta"

        fun createDefaultTemplate(bankName: String = "Nuevo Banco"): BankRuleEntity {
            return BankRuleEntity(
                bankName = bankName,
                packageOrTitleFilter = bankName.lowercase(),
                positiveKeywords = DEFAULT_POSITIVE_KEYWORDS,
                negativeKeywords = DEFAULT_NEGATIVE_KEYWORDS,
                amountRegex = DEFAULT_AMOUNT_REGEX,
                merchantRegex = DEFAULT_MERCHANT_REGEX,
                isEnabled = true
            )
        }
    }
}