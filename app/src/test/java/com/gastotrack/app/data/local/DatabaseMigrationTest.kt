package com.gastotrack.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.gastotrack.app.data.local.entities.BankRuleEntity
import com.gastotrack.app.data.local.entities.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun `test MIGRATION_1_2 versions are correctly configured`() {
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)
    }

    @Test
    fun `test MIGRATION_1_2 executes all expected DDL statements`() {
        val executedStatements = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        // Ejecutar migración
        AppDatabase.MIGRATION_1_2.migrate(dbProxy)

        // Validar que se hayan ejecutado exactamente las 4 sentencias DDL requeridas
        assertEquals(4, executedStatements.size)

        // 1. Alter table expenses agregando category con valor default
        assertTrue(
            "Debe agregar la columna category con default General",
            executedStatements.any {
                it.contains("ALTER TABLE `expenses` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'", ignoreCase = true)
            }
        )

        // 2. Alter table expenses agregando notes nullable
        assertTrue(
            "Debe agregar la columna notes nullable",
            executedStatements.any {
                it.contains("ALTER TABLE `expenses` ADD COLUMN `notes` TEXT DEFAULT NULL", ignoreCase = true)
            }
        )

        // 3. Crear índice para category
        assertTrue(
            "Debe crear el índice index_expenses_category",
            executedStatements.any {
                it.contains("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)", ignoreCase = true)
            }
        )

        // 4. Alter table bank_rules agregando priority con valor default 0
        assertTrue(
            "Debe agregar la columna priority con default 0",
            executedStatements.any {
                it.contains("ALTER TABLE `bank_rules` ADD COLUMN `priority` INTEGER NOT NULL DEFAULT 0", ignoreCase = true)
            }
        )
    }

    @Test
    fun `test default entity values match migration defaults`() {
        val expense = ExpenseEntity(
            amount = 150.0,
            source = ExpenseEntity.SOURCE_CASH,
            concept = "Tacos"
        )

        assertEquals("General", expense.category)
        assertEquals(null, expense.notes)

        val bankRule = BankRuleEntity(
            bankName = "BBVA",
            packageOrTitleFilter = "bbva",
            positiveKeywords = "compra",
            negativeKeywords = "abono"
        )
        assertEquals(0, bankRule.priority)
    }

    @Test
    fun `test MIGRATION_2_3 versions are correctly configured`() {
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun `test MIGRATION_2_3 creates incomes table and indices`() {
        val executedStatements = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        // Ejecutar migración 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(dbProxy)

        // Validar creación de tabla e índices
        assertEquals(3, executedStatements.size)
        assertTrue(
            "Debe crear la tabla incomes",
            executedStatements.any {
                it.contains("CREATE TABLE IF NOT EXISTS `incomes`", ignoreCase = true)
            }
        )
        assertTrue(
            "Debe crear el índice index_incomes_timestamp",
            executedStatements.any {
                it.contains("CREATE INDEX IF NOT EXISTS `index_incomes_timestamp` ON `incomes` (`timestamp`)", ignoreCase = true)
            }
        )
        assertTrue(
            "Debe crear el índice index_incomes_category",
            executedStatements.any {
                it.contains("CREATE INDEX IF NOT EXISTS `index_incomes_category` ON `incomes` (`category`)", ignoreCase = true)
            }
        )
    }
}

