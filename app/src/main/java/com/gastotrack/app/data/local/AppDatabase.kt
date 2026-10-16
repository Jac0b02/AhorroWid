package com.gastotrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gastotrack.app.data.local.dao.BankRuleDao
import com.gastotrack.app.data.local.dao.ExpenseDao
import com.gastotrack.app.data.local.entities.BankRuleEntity
import com.gastotrack.app.data.local.entities.ExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

@Database(
    entities = [
        ExpenseEntity::class,
        BankRuleEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun bankRuleDao(): BankRuleDao

    companion object {
        private const val DATABASE_NAME = "gastotrack_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(PrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                // Garantiza que las reglas iniciales estén presentes incluso en el primer acceso inmediato
                databaseScope.launch {
                    ensureSeedRulesInserted(instance.bankRuleDao())
                }
                instance
            }
        }

        private class PrepopulateCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    databaseScope.launch {
                        ensureSeedRulesInserted(database.bankRuleDao())
                    }
                }
            }
        }

        suspend fun ensureSeedRulesInserted(bankRuleDao: BankRuleDao) {
            if (bankRuleDao.getRulesCountSync() == 0) {
                bankRuleDao.insertAllRules(defaultBankRules())
            }
        }

        fun defaultBankRules(): List<BankRuleEntity> = listOf(
            BankRuleEntity(
                bankName = "BBVA",
                packageOrTitleFilter = "com.bancomer.mbanking, bbva",
                positiveKeywords = "compra, cargo, retiro, pago, transferencia enviada, consumiste",
                negativeKeywords = "abono, depósito, recibiste, código, puntos, promoción, apartado",
                amountRegex = BankRuleEntity.DEFAULT_AMOUNT_REGEX,
                merchantRegex = """\b(?:en|comercio|por)\s+([A-Za-z0-9][A-Za-z0-9\s\-\.&*]+?)(?:\s+con\s+|\s+el\s+|\s+por\s+\$|\.|$)""",
                isEnabled = true
            ),
            BankRuleEntity(
                bankName = "BanCoppel",
                packageOrTitleFilter = "com.bancoppel.bancoppelmovil, bancoppel",
                positiveKeywords = "compra, cargo, retiro, transferencia enviada, pago realizado",
                negativeKeywords = "abono, depósito, recibiste, código, nip, clave, estado de cuenta",
                amountRegex = BankRuleEntity.DEFAULT_AMOUNT_REGEX,
                merchantRegex = """\b(?:en|comercio|por)\s+([A-Za-z0-9][A-Za-z0-9\s\-\.&*]+?)(?:\s+con\s+|\s+fecha|\s+aut|\.|$)""",
                isEnabled = true
            ),
            BankRuleEntity(
                bankName = "Nu",
                packageOrTitleFilter = "com.nu.production, nu, nubank",
                positiveKeywords = "compra aprobada, compra, cargo, retiro, transferencia enviada, pago",
                negativeKeywords = "abono, depósito, recibiste, código, cajita, reembolso, límite",
                amountRegex = BankRuleEntity.DEFAULT_AMOUNT_REGEX,
                merchantRegex = """\b(?:en|comercio|por)\s+([A-Za-z0-9][A-Za-z0-9\s\-\.&*]+?)(?:\s+con\s+|\s+fue\s+|\.|$)""",
                isEnabled = true
            )
        )

        /**
         * Calcula el inicio del lunes de la semana actual a las 00:00:00.000 en la zona horaria del dispositivo.
         */
        fun getStartOfCurrentWeekMondayMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val today = LocalDate.now(zoneId)
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }

        /**
         * Calcula el inicio del primer día del mes actual a las 00:00:00.000 en la zona horaria del dispositivo.
         */
        fun getStartOfCurrentMonthMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
            val today = LocalDate.now(zoneId)
            val firstDay = today.with(TemporalAdjusters.firstDayOfMonth())
            return firstDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }

        /**
         * Calcula el timestamp de hace 30 días respecto al momento actual.
         */
        fun getThirtyDaysAgoMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            return nowMillis - TimeUnit.DAYS.toMillis(30)
        }
    }
}