package org.wordpress.android.fluxc.persistence.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            // language=RoomSql
            execSQL(
                    """
                              CREATE TABLE IF NOT EXISTS `OrderEntity` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `localSiteId` INTEGER NOT NULL,
                                `remoteOrderId` INTEGER NOT NULL,
                                `number` TEXT NOT NULL, 
                                `status` TEXT NOT NULL,
                                `currency` TEXT NOT NULL,
                                `orderKey` TEXT NOT NULL,
                                `dateCreated` TEXT NOT NULL,
                                `dateModified` TEXT NOT NULL,
                                `total` TEXT NOT NULL, 
                                `totalTax` TEXT NOT NULL,
                                `shippingTotal` TEXT NOT NULL,
                                `paymentMethod` TEXT NOT NULL,
                                `paymentMethodTitle` TEXT NOT NULL,
                                `datePaid` TEXT NOT NULL,
                                `pricesIncludeTax` INTEGER NOT NULL,
                                `customerNote` TEXT NOT NULL,
                                `discountTotal` TEXT NOT NULL,
                                `discountCodes` TEXT NOT NULL,
                                `refundTotal` REAL NOT NULL,
                                `billingFirstName` TEXT NOT NULL,
                                `billingLastName` TEXT NOT NULL,
                                `billingCompany` TEXT NOT NULL,
                                `billingAddress1` TEXT NOT NULL,
                                `billingAddress2` TEXT NOT NULL,
                                `billingCity` TEXT NOT NULL,
                                `billingState` TEXT NOT NULL,
                                `billingPostcode` TEXT NOT NULL,
                                `billingCountry` TEXT NOT NULL,
                                `billingEmail` TEXT NOT NULL,
                                `billingPhone` TEXT NOT NULL,
                                `shippingFirstName` TEXT NOT NULL,
                                `shippingLastName` TEXT NOT NULL,
                                `shippingCompany` TEXT NOT NULL,
                                `shippingAddress1` TEXT NOT NULL,
                                `shippingAddress2` TEXT NOT NULL,
                                `shippingCity` TEXT NOT NULL,
                                `shippingState` TEXT NOT NULL,
                                `shippingPostcode` TEXT NOT NULL,
                                `shippingCountry` TEXT NOT NULL,
                                `shippingPhone` TEXT NOT NULL,
                                `lineItems` TEXT NOT NULL,
                                `shippingLines` TEXT NOT NULL,
                                `feeLines` TEXT NOT NULL,
                                `metaData` TEXT NOT NULL
                            );
                            """.trimIndent()
            )
            // language=RoomSql
            execSQL(
                    """
                            CREATE UNIQUE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_remoteOrderId` 
                            ON `OrderEntity` (`localSiteId`, `remoteOrderId`);
                    """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE SSREntity")
    }
}
