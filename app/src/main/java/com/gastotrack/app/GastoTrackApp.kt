package com.gastotrack.app

import android.app.Application
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.gastotrack.app.widget.ExpenseGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Aplicación principal de GastoTrack.
 *
 * Registra y mantiene activa la escucha global de eventos de pantalla y bloqueo
 * (ACTION_SCREEN_OFF, ACTION_SCREEN_ON, ACTION_USER_PRESENT) para garantizar
 * que el widget de escritorio y de pantalla de bloqueo oculte de inmediato
 * cualquier dato financiero sensible cuando el dispositivo no esté desbloqueado.
 */
class GastoTrackApp : Application() {

    companion object {
        private const val TAG = "GastoTrackApp"

        @Volatile
        var isScreenOffOrLocked: Boolean = false
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lockStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.d(TAG, "lockStateReceiver detectó evento: $action")
            when (action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOffOrLocked = true
                    triggerWidgetUpdate()
                }
                Intent.ACTION_SCREEN_ON -> {
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    isScreenOffOrLocked = km?.isKeyguardLocked == true || km?.isDeviceLocked == true
                    triggerWidgetUpdate()
                }
                Intent.ACTION_USER_PRESENT -> {
                    isScreenOffOrLocked = false
                    triggerWidgetUpdate()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isScreenOffOrLocked = km?.isKeyguardLocked == true || km?.isDeviceLocked == true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(lockStateReceiver, filter)
    }

    private fun triggerWidgetUpdate() {
        applicationScope.launch {
            try {
                ExpenseGlanceWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando widget tras cambio de estado de pantalla", e)
            }
        }
    }
}
