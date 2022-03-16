package org.wordpress.android.fluxc.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_4_5
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_5_6
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_6_7
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_7_8
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_8_9
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_9_10

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

    @Test
    fun testMigrate4to5() {
        helper.apply {
            createDatabase(TEST_DB, 4).close()
            runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)
        }
    }

    @Test
    fun testMigrate5to6() {
        helper.apply {
            createDatabase(TEST_DB, 5).close()
            runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)
        }
    }

    @Test
    fun testMigrate6to7() {
        helper.apply {
            val existingDb = createDatabase(TEST_DB, 6).apply {
                execSQL(
                        // language=RoomSql
                        """
                            INSERT INTO OrderEntity VALUES(1, 2, 3, '123', 'processing', '$', 'key', 'date of creation', 'date of modification', '123', '456', '789', 'card', 'by card', 'date paid', 1, 'sample customer note', '213', 'CODE', 123, 'billing first name', 'billing last name', 'billing company', 'billing address1', 'billing address2', 'billing city', 'billing state', 'billing postcode', 'billing country', 'billing email', 'billing phone', 'shipping first name', 'shipping last name', 'shipping company', 'shipping address1', 'shipping address2', 'shipping city', 'shipping state', 'shipping postcode', 'shipping country', 'shipping phone', 'line items', 'shipping lines', 'fee lines', 'meta data')
                        """.trimIndent()
                )
            }.close()

            val migratedDb = runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

            migratedDb.query(
                    // language=RoomSql
                    """
                        SELECT * FROM OrderEntity
                    """.trimIndent()
            )
        }
    }

    @Test
    fun testMigration7to8() {
        helper.apply {
            createDatabase(TEST_DB, 7).close()
            runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)
        }
    }

    @Test
    fun testMigration8to9() {
        helper.apply {
            createDatabase(TEST_DB, 8).close()
            runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)
        }
    }

    @Test
    fun testMigrate9to10() {
        helper.apply {
            createDatabase(TEST_DB, 9).close()
            runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
