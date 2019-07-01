package org.wordpress.android.fluxc.persistance.room

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.persistence.room.StatsDatabase

@RunWith(MockitoJUnitRunner::class)
abstract class StatsDatabaseTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    val database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().context,
            StatsDatabase::class.java
    )
            .allowMainThreadQueries()
            .build()

    @After
    fun tearDown() {
        database.clearAllTables()
    }
}
