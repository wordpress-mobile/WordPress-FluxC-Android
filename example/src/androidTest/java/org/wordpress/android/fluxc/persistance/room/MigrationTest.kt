package org.wordpress.android.fluxc.persistance.room

import android.arch.persistence.room.testing.MigrationTestHelper
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.persistence.room.StatsDatabase

@RunWith(AndroidJUnit4::class)
open class MigrationTest {
    @Rule
    @JvmField
    val testHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            StatsDatabase::class.java.canonicalName
    )
}
