package org.wordpress.android.fluxc.persistance.room

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.persistence.room.Room
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.runner.RunWith
import org.wordpress.android.fluxc.persistence.room.StatsDatabase

@RunWith(AndroidJUnit4::class)
abstract class StatsDatabaseTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()
    val database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(), StatsDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @After
    fun tearDown() {
        database.clearAllTables()
    }
}
