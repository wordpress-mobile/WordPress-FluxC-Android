package org.wordpress.android.fluxc.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.Context
import org.wordpress.android.fluxc.persistence.StatsDao.StatsBlock

@Database(entities = arrayOf(StatsBlock::class), version = 1)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        private val migrations = listOf<Migration>()
        fun build(context: Context): StatsDatabase {
            return Room.databaseBuilder(context.applicationContext, StatsDatabase::class.java, "stats.db")
                    .addMigrations(*migrations.toTypedArray()).build()
        }
    }
}
