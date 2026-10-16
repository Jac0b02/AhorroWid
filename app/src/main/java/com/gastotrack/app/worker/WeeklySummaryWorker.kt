package com.gastotrack.app.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gastotrack.app.data.local.AppDatabase
import com.gastotrack.app.ui.MainActivity
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeeklySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val mondayStartMillis = AppDatabase.getStartOfCurrentWeekMondayMillis()
            val summary = db.expenseDao().getWeeklySummarySync(mondayStartMillis)

            sendWeeklySummaryNotification(
                total = summary.total,
                cardTotal = summary.tarjeta,
                cashTotal = summary.efectivo,
                txCount = summary.txCount
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendWeeklySummaryNotification(
        total: Double,
        cardTotal: Double,
        cashTotal: Double,
        txCount: Int
    ) {
        createNotificationChannelIfNeeded()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val currency = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        val totalFormatted = currency.format(total)
        val cardFormatted = currency.format(cardTotal)
        val cashFormatted = currency.format(cashTotal)

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryBody = buildString {
            append("Total acumulado: $totalFormatted ($txCount movimientos)\n")
            append("💳 Tarjeta: $cardFormatted  •  💵 Efectivo: $cashFormatted")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Resumen Semanal GastoTrack: $totalFormatted")
            .setContentText("💳 Tarjeta: $cardFormatted | 💵 Efectivo: $cashFormatted")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannelIfNeeded() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Resumen Semanal de Gastos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificación dominical a las 20:00 con el balance semanal consolidado"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "GastoTrackWeeklySummaryWork"
        private const val CHANNEL_ID = "gastotrack_weekly_summary_channel"
        private const val NOTIFICATION_ID = 7020
        private const val REQUEST_CODE_OPEN_APP = 2001

        /**
         * Programa el Worker para ejecutarse cada domingo a las 20:00 hora local.
         */
        fun scheduleWeeklySummary(context: Context) {
            val now = ZonedDateTime.now()
            var nextSundayAt8Pm = now
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                .withHour(20)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            if (!nextSundayAt8Pm.isAfter(now)) {
                nextSundayAt8Pm = nextSundayAt8Pm.plusWeeks(1)
            }

            val initialDelayMillis = Duration.between(now, nextSundayAt8Pm).toMillis()

            val workRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(
                7, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}