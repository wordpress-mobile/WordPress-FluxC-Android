package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.persistence.converters.LocalIdConverter
import org.wordpress.android.fluxc.persistence.converters.LongListConverter
import org.wordpress.android.fluxc.persistence.converters.RemoteIdConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.SSRDao
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.SSREntity

@Database(
        version = 4,
        entities = [
            AddonEntity::class,
            AddonOptionEntity::class,
            GlobalAddonGroupEntity::class,
            WCOrderModel::class,
            SSREntity::class
        ]
)
@TypeConverters(
        value = [
            LocalIdConverter::class,
            LongListConverter::class,
            RemoteIdConverter::class
        ]
)
abstract class WCAndroidDatabase : RoomDatabase() {
    internal abstract fun addonsDao(): AddonsDao
    abstract fun ssrDao(): SSRDao
    abstract fun ordersDao(): OrdersDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WCAndroidDatabase::class.java,
                "wc-android-database"
        ).allowMainThreadQueries()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
