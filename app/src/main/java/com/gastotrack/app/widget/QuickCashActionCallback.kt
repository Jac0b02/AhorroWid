package com.gastotrack.app.widget

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.data.local.entities.ExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuickCashActionCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val amount = parameters[KEY_AMOUNT] ?: return
        if (amount <= 0.0) return

        val concept = parameters[KEY_CONCEPT]?.takeIf { it.isNotBlank() } ?: "Efectivo"

        try {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(context)
                db.expenseDao().insertExpense(
                    ExpenseEntity(
                        amount = amount,
                        source = ExpenseEntity.SOURCE_CASH,
                        concept = concept,
                        bankName = "Efectivo",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            // Activar feedback háptico inmediato al registrar desde el widget
            triggerHapticFeedback(context)

            // Refrescar todas las instancias del widget
            ExpenseGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("QuickCashCallback", "Error al insertar gasto rápido desde el widget", e)
        }
    }

    private fun triggerHapticFeedback(context: Context) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(55L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            Log.w("QuickCashCallback", "No se pudo ejecutar la vibración háptica: ${e.message}")
        }
    }

    companion object {
        val KEY_AMOUNT = ActionParameters.Key<Double>("quick_cash_amount")
        val KEY_CONCEPT = ActionParameters.Key<String>("quick_cash_concept")
    }
}