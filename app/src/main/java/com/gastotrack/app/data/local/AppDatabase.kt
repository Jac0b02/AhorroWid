package com.gastotrack.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gastotrack.app.data.local.dao.BankRuleDao
import com.gastotrack.app.data.local.dao.ExpenseDao
import com.gastotrack.app.data.local.dao.IncomeDao
import com.gastotrack.app.data.local.entities.BankRuleEntity
import com.gastotrack.app.data.local.entities.ExpenseEntity
import com.gastotrack.app.data.local.entities.IncomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import androidx.room.migration.Migration
import java.util.concurrent.TimeUnit

@Database(
    entities = [
        ExpenseEntity::class,
        BankRuleEntity::class,
        IncomeEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun bankRuleDao(): BankRuleDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        private const val DATABASE_NAME = "gastotrack_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val databaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Migración 1 -> 2:
         * 1. Agrega la columna 'category' (TEXT NOT NULL DEFAULT 'General') a la tabla 'expenses'.
         * 2. Agrega la columna 'notes' (TEXT nullable) a la tabla 'expenses'.
         * 3. Crea el índice 'index_expenses_category' para optimizar reportes y búsquedas por categoría.
         * 4. Agrega la columna 'priority' (INTEGER NOT NULL DEFAULT 0) a la tabla 'bank_rules'.
         *
         * Preserva el 100% de los datos históricos del usuario sin pérdida ni reinicio de base de datos.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Agregar category con valor default 'General'
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")

                // 2. Agregar notes nullable
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `notes` TEXT DEFAULT NULL")

                // 3. Crear índice para category
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")

                // 4. Agregar priority con valor default 0
                db.execSQL("ALTER TABLE `bank_rules` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migración 2 -> 3:
         * 1. Crea la tabla 'incomes' para registrar ingresos/ganancias y permitir compararlos con los gastos.
         * 2. Crea los índices por timestamp y category para búsquedas reactivas optimizadas.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `incomes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `concept` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'General',
                        `notes` TEXT DEFAULT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_incomes_timestamp` ON `incomes` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_incomes_category` ON `incomes` (`category`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade() // NUNCA borrar datos en actualizaciones hacia adelante
                    .addCallback(PrepopulateCallback())
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
         * Calcula el rango de inicio (Lunes 00:00:00.000) y fin (Domingo 23:59:59.999)
         * para la semana especificada con base en un offset (0 = actual, -1 = anterior, etc.).
         */
        fun getWeekRangeMillis(offsetWeeks: Int = 0, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
            val target = LocalDate.now(zoneId).plusWeeks(offsetWeeks.toLong())
            val monday = target.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = monday.plusDays(6)
            val startMillis = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = sunday.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
            return Pair(startMillis, endMillis)
        }

        /**
         * Calcula el rango de inicio (día 1 a las 00:00:00.000) y fin (último día del mes a las 23:59:59.999)
         * para el mes especificado con base en un offset (0 = actual, -1 = anterior, etc.).
         */
        fun getMonthRangeMillis(offsetMonths: Int = 0, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
            val targetMonth = YearMonth.now(zoneId).plusMonths(offsetMonths.toLong())
            val startMillis = targetMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = targetMonth.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
            return Pair(startMillis, endMillis)
        }

        /**
         * Calcula el timestamp de hace 30 días respecto al momento actual.
         */
        fun getThirtyDaysAgoMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            return nowMillis - TimeUnit.DAYS.toMillis(30)
        }
    }
}