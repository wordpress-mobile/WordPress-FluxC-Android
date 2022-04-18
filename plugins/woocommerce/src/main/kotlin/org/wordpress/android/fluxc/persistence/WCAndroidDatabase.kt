package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.persistence.converters.BigDecimalConverter
import org.wordpress.android.fluxc.persistence.converters.DiscountTypeConverter
import org.wordpress.android.fluxc.persistence.converters.ISO8601DateConverter
import org.wordpress.android.fluxc.persistence.converters.LocalIdConverter
import org.wordpress.android.fluxc.persistence.converters.LongListConverter
import org.wordpress.android.fluxc.persistence.converters.RemoteIdConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.ProductCategoriesDao
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.CouponAndProductEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.ProductCategoryEntity
import org.wordpress.android.fluxc.persistence.entity.ProductEntity
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_10_11
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_11_12
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_4_5
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_5_6
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_6_7
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_7_8
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_8_9
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_9_10

@Database(
        version = 13,
        entities = [
            AddonEntity::class,
            AddonOptionEntity::class,
            CouponEntity::class,
            CouponEmailEntity::class,
            CouponAndProductEntity::class,
            CouponAndProductCategoryEntity::class,
            GlobalAddonGroupEntity::class,
            OrderNoteEntity::class,
            ProductEntity::class,
            ProductCategoryEntity::class,
            OrderEntity::class,
            InboxNoteEntity::class,
            InboxNoteActionEntity::class
        ],
        autoMigrations = [
            AutoMigration(from = 12, to = 13)
        ]
)
@TypeConverters(
        value = [
            LocalIdConverter::class,
            LongListConverter::class,
            RemoteIdConverter::class,
            BigDecimalConverter::class,
            DiscountTypeConverter::class,
            ISO8601DateConverter::class
        ]
)
abstract class WCAndroidDatabase : RoomDatabase(), TransactionExecutor {
    abstract val addonsDao: AddonsDao
    abstract val ordersDao: OrdersDao
    abstract val orderNotesDao: OrderNotesDao
    abstract val couponsDao: CouponsDao
    abstract val productsDao: ProductsDao
    abstract val productCategoriesDao: ProductCategoriesDao
    abstract val inboxNotesDao: InboxNotesDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WCAndroidDatabase::class.java,
                "wc-android-database"
        ).allowMainThreadQueries()
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigrationFrom(1, 2)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_6_7)
                .addMigrations(MIGRATION_7_8)
                .addMigrations(MIGRATION_8_9)
                .addMigrations(MIGRATION_9_10)
                .addMigrations(MIGRATION_10_11)
                .addMigrations(MIGRATION_11_12)
                .build()
    }

    override suspend fun <R> executeInTransaction(block: suspend () -> R): R =
        withTransaction(block)

    override fun <R> runInTransaction(block: () -> R): R = runInTransaction(block)
}
