package org.wordpress.android.fluxc.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.persistence.room.StatsDao.StatsBlock

const val STATS_DB_NAME = "stats_database.db"

@Database(entities = arrayOf(StatsBlock::class), version = 1, exportSchema = true)
@TypeConverters(BlockTypeConverter::class, StatsTypeConverter::class)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao

    companion object {
        fun build(context: Context): StatsDatabase {
            return Room.databaseBuilder(context.applicationContext, StatsDatabase::class.java, STATS_DB_NAME).build()
        }
    }
}
