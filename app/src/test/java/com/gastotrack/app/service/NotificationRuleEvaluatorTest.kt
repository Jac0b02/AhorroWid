package com.gastotrack.app.service

import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.data.local.entities.BankRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRuleEvaluatorTest {

    private val rules = AppDatabase.defaultBankRules().associateBy { it.bankName }
    private val bbvaRule = rules["BBVA"]!!
    private val bancoCoppelRule = rules["BanCoppel"]!!
    private val nuRule = rules["Nu"]!!

    @Test
    fun `test BBVA purchase extracts amount and merchant accurately`() {
        val sample = "BBVA: Compra realizada por $1,249.90 MXN en AMAZON MEXICO con tu tarjeta terminación 4012."
        val result = NotificationRuleEvaluator.evaluateNotificationText(bbvaRule, sample)

        assertTrue("Debe coincidir con la regla de BBVA", result.isMatched)
        assertEquals(1249.90, result.extractedAmount!!, 0.001)
        assertEquals("AMAZON MEXICO", result.extractedConcept)
        assertNull("No debe haber razón de descarte", result.discardReason)
    }

    @Test
    fun `test BBVA abono or SPEI is rejected by negative keywords`() {
        val sample = "BBVA: Recibiste un abono de $3,500.00 MXN por transferencia SPEI."
        val result = NotificationRuleEvaluator.evaluateNotificationText(bbvaRule, sample)

        assertFalse("El abono debe ser descartado", result.isMatched)
        assertNotNull(result.discardReason)
        assertTrue(result.discardReason!!.contains("abono"))
        assertNull(result.extractedAmount)
    }

    @Test
    fun `test BanCoppel charge extracts amount and merchant`() {
        val sample = "BanCoppel: Se realizó un cargo por $320.00 en OXXO CULIACAN con tu tarjeta de débito."
        val result = NotificationRuleEvaluator.evaluateNotificationText(bancoCoppelRule, sample)

        assertTrue("Debe coincidir con la regla de BanCoppel", result.isMatched)
        assertEquals(320.00, result.extractedAmount!!, 0.001)
        assertEquals("OXXO CULIACAN", result.extractedConcept)
    }

    @Test
    fun `test Nu purchase with integer amount is parsed correctly`() {
        val sample = "Nu: Compra aprobada por $185.00 en UBER TRIP con tu tarjeta Morada."
        val result = NotificationRuleEvaluator.evaluateNotificationText(nuRule, sample)

        assertTrue("Debe coincidir con la regla de Nu", result.isMatched)
        assertEquals(185.00, result.extractedAmount!!, 0.001)
        assertEquals("UBER TRIP", result.extractedConcept)
    }

    @Test
    fun `test empty or blank notification text is rejected safely`() {
        val result = NotificationRuleEvaluator.evaluateNotificationText(bbvaRule, "   ")
        assertFalse(result.isMatched)
        assertEquals("El texto de la notificación está vacío.", result.discardReason)
    }

    @Test
    fun `test package and title matching supports multiple tokens`() {
        assertTrue(
            NotificationRuleEvaluator.matchesPackageOrTitle(
                rule = bbvaRule,
                packageName = "com.bancomer.mbanking",
                notificationTitle = "Notificación"
            )
        )

        assertTrue(
            NotificationRuleEvaluator.matchesPackageOrTitle(
                rule = bbvaRule,
                packageName = "com.other.app",
                notificationTitle = "BBVA Móvil"
            )
        )

        assertFalse(
            NotificationRuleEvaluator.matchesPackageOrTitle(
                rule = bbvaRule,
                packageName = "com.facebook.katana",
                notificationTitle = "Nuevo mensaje"
            )
        )
    }
}
