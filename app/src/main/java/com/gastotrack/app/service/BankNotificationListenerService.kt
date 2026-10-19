package com.gastotrack.app.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.data.local.entities.BankRuleEntity
import com.gastotrack.app.data.local.entities.ExpenseEntity
import com.gastotrack.app.widget.ExpenseGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resultado estructurado de la evaluación de una notificación contra una BankRuleEntity.
 * Compartido entre el servicio en segundo plano y el Sandbox en vivo (SettingsScreen).
 */
data class RuleEvaluationResult(
    val isMatched: Boolean,
    val ruleName: String,
    val discardReason: String? = null,
    val extractedAmount: Double? = null,
    val extractedConcept: String? = null,
    val matchedPositiveKeyword: String? = null
)

/**
 * Motor determinista de evaluación y extracción para reglas bancarias dinámicas.
 */
object NotificationRuleEvaluator {

    /**
     * Verifica si el paquete de la app o el título de la notificación coincide con el filtro de la regla.
     * Soporta múltiples identificadores separados por coma.
     */
    fun matchesPackageOrTitle(
        rule: BankRuleEntity,
        packageName: String,
        notificationTitle: String
    ): Boolean {
        val tokens = rule.packageOrTitleFilter
            .split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) return false

        val normalizedPkg = packageName.trim().lowercase(Locale.ROOT)
        val normalizedTitle = notificationTitle.trim().lowercase(Locale.ROOT)

        return tokens.any { filterToken ->
            normalizedPkg.contains(filterToken) || normalizedTitle.contains(filterToken)
        }
    }

    /**
     * Evalúa el texto completo de la notificación contra las palabras clave y expresiones regulares de la regla.
     */
    fun evaluateNotificationText(
        rule: BankRuleEntity,
        fullNotificationText: String
    ): RuleEvaluationResult {
        val cleanText = fullNotificationText.trim()
        if (cleanText.isEmpty()) {
            return RuleEvaluationResult(
                isMatched = false,
                ruleName = rule.bankName,
                discardReason = "El texto de la notificación está vacío."
            )
        }

        val normalizedText = cleanText.lowercase(Locale.ROOT)

        // 1. Verificar palabras negativas (exclusión inmediata)
        val negativeWords = rule.negativeKeywords
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (negWord in negativeWords) {
            if (normalizedText.contains(negWord.lowercase(Locale.ROOT))) {
                return RuleEvaluationResult(
                    isMatched = false,
                    ruleName = rule.bankName,
                    discardReason = "Se detectó la palabra de exclusión '$negWord'."
                )
            }
        }

        // 2. Verificar al menos una palabra positiva
        val positiveWords = rule.positiveKeywords
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val matchedPositive = positiveWords.firstOrNull { posWord ->
            normalizedText.contains(posWord.lowercase(Locale.ROOT))
        }

        if (matchedPositive == null) {
            return RuleEvaluationResult(
                isMatched = false,
                ruleName = rule.bankName,
                discardReason = "No contiene ninguna palabra clave positiva (${positiveWords.joinToString(", ")})."
            )
        }

        // 3. Ejecutar expresión regular de monto
        val amountRegexPattern = rule.amountRegex.trim()
        if (amountRegexPattern.isEmpty()) {
            return RuleEvaluationResult(
                isMatched = false,
                ruleName = rule.bankName,
                discardReason = "La expresión regular de monto está vacía."
            )
        }

        val amountRegex = try {
            Regex(amountRegexPattern, RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            return RuleEvaluationResult(
                isMatched = false,
                ruleName = rule.bankName,
                discardReason = "Expresión regular de monto inválida: ${e.localizedMessage ?: "error de sintaxis"}"
            )
        }

        val amountMatch = amountRegex.find(cleanText)
            ?: return RuleEvaluationResult(
                isMatched = false,
                ruleName = rule.bankName,
                discardReason = "Se detectó '$matchedPositive', pero no se encontró un monto válido con el patrón configurado."
            )

        val rawAmountStr = if (amountMatch.groupValues.size > 1 && amountMatch.groupValues[1].isNotBlank()) {
            amountMatch.groupValues[1]
        } else {
            amountMatch.value.replace(Regex("[^\\d.,]"), "")
        }

        val parsedAmount = rawAmountStr
            .replace(",", "")
            .trim()
            .toDoubleOrNull()

        if (parsedAmount == null || parsedAmount <= 0.0) {
            return RuleEvaluationResult(
                isMatched = false,
                ruleName = rule.bankName,
                discardReason = "No se pudo convertir el monto extraído ('$rawAmountStr') a un valor numérico mayor a $0.00."
            )
        }

        // 4. Extraer comercio/concepto mediante merchantRegex (opcional) o usar bankName por defecto
        val extractedMerchant = extractMerchantOrDefault(rule, cleanText)

        return RuleEvaluationResult(
            isMatched = true,
            ruleName = rule.bankName,
            discardReason = null,
            extractedAmount = parsedAmount,
            extractedConcept = extractedMerchant,
            matchedPositiveKeyword = matchedPositive
        )
    }

    private fun extractMerchantOrDefault(rule: BankRuleEntity, fullText: String): String {
        val pattern = rule.merchantRegex?.trim()
        if (pattern.isNullOrEmpty()) {
            return rule.bankName
        }

        return try {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(fullText)
            val candidate = if (match != null) {
                if (match.groupValues.size > 1 && match.groupValues[1].isNotBlank()) {
                    match.groupValues[1]
                } else {
                    match.value
                }
            } else {
                null
            }

            val cleaned = candidate
                ?.trim()
                ?.trimEnd('.', ',', ';', ':', '-')
                ?.replace(Regex("\\s+(?:con\\s+tu|con\\s+tarjeta|el\\s+\\d).*$", RegexOption.IGNORE_CASE), "")
                ?.replace(Regex("\\s+"), " ")
                ?.take(45)

            if (!cleaned.isNullOrBlank()) cleaned else rule.bankName
        } catch (e: Exception) {
            Log.w("RuleEvaluator", "merchantRegex inválido en regla ${rule.bankName}: ${e.message}")
            rule.bankName
        }
    }
}

class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Caché en memoria para deduplicación rápida dentro de la ventana de 10 segundos
    private val recentProcessedFingerprints = ConcurrentHashMap<String, Long>()

    // Receptor dinámico para actualizar inmediatamente el widget al bloquear o desbloquear la pantalla
    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_SCREEN_OFF || action == Intent.ACTION_SCREEN_ON || action == Intent.ACTION_USER_PRESENT) {
                serviceScope.launch {
                    try {
                        context?.let { ExpenseGlanceWidget().updateAll(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error actualizando widget ante evento $action", e)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenLockReceiver, filter)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notification = sbn?.notification ?: return

        // Ignorar notificaciones en curso (reproductores, descargas) o resúmenes de grupo vacíos
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val packageName = sbn.packageName.orEmpty()
        // Evitar procesar notificaciones emitidas por el propio GastoTrack
        if (packageName == applicationContext.packageName) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().trim()

        val bodyContent = if (bigText.isNotEmpty()) bigText else text
        val fullNotificationText = listOf(title, bodyContent)
            .filter { it.isNotEmpty() }
            .joinToString(" - ")

        if (fullNotificationText.isBlank()) return

        serviceScope.launch {
            processNotificationAsync(
                packageName = packageName,
                title = title,
                fullText = fullNotificationText
            )
        }
    }

    private suspend fun processNotificationAsync(
        packageName: String,
        title: String,
        fullText: String
    ) {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val activeRules = db.bankRuleDao().getActiveRulesSync()
            if (activeRules.isEmpty()) return

            for (rule in activeRules) {
                val filterMatches = NotificationRuleEvaluator.matchesPackageOrTitle(
                    rule = rule,
                    packageName = packageName,
                    notificationTitle = title
                )
                if (!filterMatches) continue

                val evaluation = NotificationRuleEvaluator.evaluateNotificationText(
                    rule = rule,
                    fullNotificationText = fullText
                )

                if (evaluation.isMatched && evaluation.extractedAmount != null) {
                    val amount = evaluation.extractedAmount
                    val concept = evaluation.extractedConcept ?: rule.bankName
                    val now = System.currentTimeMillis()

                    // Ventana anti-duplicados de 10 segundos (en memoria + verificación en Room)
                    if (isDuplicateWithinWindow(db, amount, concept, now)) {
                        Log.i(TAG, "Gasto duplicado ignorado ($amount - $concept) en ventana de 10s.")
                        return
                    }

                    val expense = ExpenseEntity(
                        amount = amount,
                        source = ExpenseEntity.SOURCE_CARD,
                        concept = concept,
                        bankName = rule.bankName,
                        timestamp = now
                    )

                    db.expenseDao().insertExpense(expense)
                    Log.i(TAG, "Gasto registrado automáticamente: $${amount} en $concept (${rule.bankName})")

                    // Actualizar el Widget de Jetpack Glance inmediatamente
                    ExpenseGlanceWidget().updateAll(applicationContext)
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar notificación bancaria", e)
        }
    }

    private suspend fun isDuplicateWithinWindow(
        db: AppDatabase,
        amount: Double,
        concept: String,
        nowMillis: Long
    ): Boolean {
        val windowStart = nowMillis - DUPLICATE_WINDOW_MS
        val fingerprint = String.format(Locale.US, "%.2f|%s", amount, concept.lowercase(Locale.ROOT))

        // Limpiar huellas expiradas en memoria
        recentProcessedFingerprints.entries.removeIf { (_, timestamp) ->
            timestamp < windowStart
        }

        val previousTimestamp = recentProcessedFingerprints[fingerprint]
        if (previousTimestamp != null && previousTimestamp >= windowStart) {
            return true
        }

        // Verificación secundaria en Room por si el servicio fue recreado
        val existingInDb = db.expenseDao().findRecentDuplicate(
            amount = amount,
            concept = concept,
            minTimestamp = windowStart
        )
        if (existingInDb != null) {
            recentProcessedFingerprints[fingerprint] = existingInDb.timestamp
            return true
        }

        recentProcessedFingerprints[fingerprint] = nowMillis
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenLockReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "screenLockReceiver ya no estaba registrado al destruir servicio: ${e.message}")
        }
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "BankNotifListener"
        private const val DUPLICATE_WINDOW_MS = 10_000L // 10 segundos
    }
}