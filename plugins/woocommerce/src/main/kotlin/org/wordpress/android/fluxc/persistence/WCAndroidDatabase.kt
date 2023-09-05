package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.taxes.TaxBasedOnSettingEntity
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity
import org.wordpress.android.fluxc.persistence.converters.BigDecimalConverter
import org.wordpress.android.fluxc.persistence.converters.LocalIdConverter
import org.wordpress.android.fluxc.persistence.converters.LongListConverter
import org.wordpress.android.fluxc.persistence.converters.RemoteIdConverter
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.dao.InboxNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.dao.TaxBasedOnDao
import org.wordpress.android.fluxc.persistence.dao.TaxRateDao
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponEntity
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration13to14
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration14to15
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration16to17
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration17to18
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration18to19
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration19to20
import org.wordpress.android.fluxc.persistence.migrations.AutoMigration23to24
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_10_11
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_11_12
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_15_16
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_20_21
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_21_22
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_22_23
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_24_25
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_3_4
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_4_5
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_5_6
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_6_7
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_7_8
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_8_9
import org.wordpress.android.fluxc.persistence.migrations.MIGRATION_9_10

@Database(
    version = 26,
    entities = [
        AddonEntity::class,
        AddonOptionEntity::class,
        CouponEntity::class,
        CouponEmailEntity::class,
        GlobalAddonGroupEntity::class,
        OrderNoteEntity::class,
        OrderEntity::class,
        OrderMetaDataEntity::class,
        InboxNoteEntity::class,
        InboxNoteActionEntity::class,
        TopPerformerProductEntity::class,
        TaxBasedOnSettingEntity::class,
        TaxRateEntity::class
    ],
    autoMigrations = [
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14, spec = AutoMigration13to14::class),
        AutoMigration(from = 14, to = 15, spec = AutoMigration14to15::class),
        AutoMigration(from = 16, to = 17, spec = AutoMigration16to17::class),
        AutoMigration(from = 17, to = 18, spec = AutoMigration17to18::class),
        AutoMigration(from = 18, to = 19, spec = AutoMigration18to19::class),
        AutoMigration(from = 19, to = 20, spec = AutoMigration19to20::class),
        AutoMigration(from = 23, to = 24, spec = AutoMigration23to24::class),
        AutoMigration(from = 25, to = 26),
    ]
)
@TypeConverters(
    value = [
        LocalIdConverter::class,
        LongListConverter::class,
        RemoteIdConverter::class,
        BigDecimalConverter::class
    ]
)
abstract class WCAndroidDatabase : RoomDatabase(), TransactionExecutor {
    abstract val addonsDao: AddonsDao
    abstract val ordersDao: OrdersDao
    abstract val orderNotesDao: OrderNotesDao
    abstract val orderMetaDataDao: OrderMetaDataDao
    abstract val couponsDao: CouponsDao
    abstract val inboxNotesDao: InboxNotesDao
    abstract val topPerformerProductsDao: TopPerformerProductsDao
    abstract val taxBasedOnSettingDao: TaxBasedOnDao
    abstract val taxRateDao: TaxRateDao

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
            .addMigrations(MIGRATION_15_16)
            .addMigrations(MIGRATION_20_21)
            .addMigrations(MIGRATION_21_22)
            .addMigrations(MIGRATION_22_23)
            .addMigrations(MIGRATION_24_25)
            .build()
    }

    override suspend fun <R> executeInTransaction(block: suspend () -> R): R =
        withTransaction(block)

    override fun <R> runInTransaction(block: () -> R): R = runInTransaction(block)
}
