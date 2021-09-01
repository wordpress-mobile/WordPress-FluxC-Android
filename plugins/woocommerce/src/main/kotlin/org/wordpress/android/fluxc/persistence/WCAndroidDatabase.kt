package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.persistence.converters.LongListConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity

@Database(
        version = 1,
        entities = [
            AddonEntity::class,
            AddonOptionEntity::class,
            GlobalAddonGroupEntity::class
        ]
)
@TypeConverters(value = [LongListConverter::class])
abstract class WCAndroidDatabase : RoomDatabase() {
    internal abstract fun addonsDao(): AddonsDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WCAndroidDatabase::class.java,
                "wc-android-database"
        ).fallbackToDestructiveMigration()
                .build()
    }
}
