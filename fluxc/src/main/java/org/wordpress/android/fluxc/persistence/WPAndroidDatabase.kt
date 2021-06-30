package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.persistence.PluginCapabilitiesDao.PluginCapabilities

@Database(entities = [BloggingReminders::class, PluginCapabilities::class], version = 2)
abstract class WPAndroidDatabase : RoomDatabase() {
    abstract fun bloggingRemindersDao(): BloggingRemindersDao
    abstract fun pluginCapabilitiesDao(): PluginCapabilitiesDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WPAndroidDatabase::class.java,
                "wp-android-database"
        ).fallbackToDestructiveMigration().build()
    }
}
