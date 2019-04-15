package org.wordpress.android.fluxc.persistence.room

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.content.Context
import org.wordpress.android.fluxc.persistence.room.StatsDao.StatsBlock

const val STATS_DB_NAME = "stats.db"

@Database(entities = arrayOf(StatsBlock::class), version = 2, exportSchema = true)
@TypeConverters(BlockTypeConverter::class, StatsTypeConverter::class)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        fun build(context: Context): StatsDatabase {
            return Room.databaseBuilder(context.applicationContext, StatsDatabase::class.java, STATS_DB_NAME)
                    .addMigrations(Migration1to2).build()
        }
    }
}
