package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_1_3
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_4_5

@RunWith(AndroidJUnit4::class)
class MigrationTests {
    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_3, MIGRATION_3_4, MIGRATION_4_5)

    @Rule
    @JvmField
    val helper: MigrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WCAndroidDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateAll() {
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        Room.databaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                WCAndroidDatabase::class.java,
                TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase.close()
        }
    }

    @Test
    fun testMigrate1To3() {
        helper.apply {
            createDatabase(TEST_DB, 1).close()
            runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_3)
        }
    }

    @Test
    fun testMigrate3To4() {
        helper.apply {
            createDatabase(TEST_DB, 3).close()
            runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        }
    }

    @Test
    fun testMigrate4to5() {
        helper.apply {
            createDatabase(TEST_DB, 4).close()
            runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
