package org.wordpress.android.fluxc.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4

@RunWith(AndroidJUnit4::class)
class MigrationTests {
    @Rule
    @JvmField
    val helper: MigrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            WCAndroidDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigrate3To4() {
        helper.apply {
            createDatabase(TEST_DB, 3).close()
            runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
