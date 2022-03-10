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

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            execSQL("DROP TABLE OrderEntity")
            execSQL(
                    // language=RoomSql
                    """ CREATE TABLE IF NOT EXISTS OrderEntity (
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
                        `refundTotal` TEXT NOT NULL,
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
                        `metaData` TEXT NOT NULL)
                    """.trimIndent()
            )
            execSQL(
                    // language=RoomSql
                    """
                            CREATE UNIQUE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_remoteOrderId` 
                            ON `OrderEntity` (`localSiteId`, `remoteOrderId`);
                    """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE OrderEntity ADD taxLines TEXT NOT NULL DEFAULT ''")
    }
}

internal val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS OrderNotes (
                siteId INTEGER NOT NULL,
                noteId INTEGER NOT NULL,
                orderId INTEGER NOT NULL,
                dateCreated TEXT,
                note TEXT,
                author TEXT,
                isSystemNote INTEGER NOT NULL,
                isCustomerNote INTEGER NOT NULL,
                PRIMARY KEY(`siteId`, `noteId`))
        """.trimIndent())
    }
}

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `Coupons` (`id` INTEGER NOT NULL,
                        `siteId` INTEGER NOT NULL,
                        `code` TEXT,
                        `dateCreated` TEXT,
                        `dateCreatedGmt` TEXT,
                        `dateModified` TEXT,
                        `dateModifiedGmt` TEXT,
                        `discountType` TEXT,
                        `description` TEXT,
                        `dateExpires` TEXT,
                        `dateExpiresGmt` TEXT,
                        `usageCount` INTEGER,
                        `isForIndividualUse` INTEGER,
                        `usageLimit` INTEGER,
                        `usageLimitPerUser` INTEGER,
                        `limitUsageToXItems` INTEGER,
                        `isShippingFree` INTEGER,
                        `areSaleItemsExcluded` INTEGER,
                        `minimumAmount` TEXT,
                        `maximumAmount` TEXT,
                        PRIMARY KEY(`id`,`siteId`))
                        """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `Products` (
                    `id` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `name` TEXT,
                    `slug` TEXT,
                    `permalink` TEXT,
                    `dateCreated` TEXT,
                    `dateModified` TEXT,
                    `type` TEXT,
                    `status` TEXT,
                    `isFeatured` INTEGER,
                    `catalogVisibility` TEXT,
                    `description` TEXT,
                    `shortDescription` TEXT,
                    `sku` TEXT,
                    `price` TEXT,
                    `regularPrice` TEXT,
                    `salePrice` TEXT,
                    `isOnSale` INTEGER,
                    `totalSales` INTEGER,
                    `isPurchasable` INTEGER,
                    `dateOnSaleFrom` TEXT,
                    `dateOnSaleTo` TEXT,
                    `dateOnSaleFromGmt` TEXT,
                    `dateOnSaleToGmt` TEXT,
                    `isVirtual` INTEGER,
                    `isDownloadable` INTEGER,
                    `downloadLimit` INTEGER,
                    `downloadExpiry` INTEGER,
                    `isSoldIndividually` INTEGER,
                    `externalUrl` TEXT,
                    `buttonText` TEXT,
                    `taxStatus` TEXT,
                    `taxClass` TEXT,
                    `isStockManaged` INTEGER,
                    `stockQuantity` REAL,
                    `stockStatus` TEXT,
                    `backorders` TEXT,
                    `areBackordersAllowed` INTEGER,
                    `isBackordered` INTEGER,
                    `isShippingRequired` INTEGER,
                    `isShippingTaxable` INTEGER,
                    `shippingClass` TEXT,
                    `shippingClassId` INTEGER,
                    `areReviewsAllowed` INTEGER,
                    `averageRating` TEXT,
                    `ratingCount` INTEGER,
                    `parentId` INTEGER,
                    `purchaseNote` TEXT,
                    `menuOrder` INTEGER,
                    PRIMARY KEY(`id`,`siteId`))
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `ProductCategories` (
                `id` INTEGER NOT NULL,
                `siteId` INTEGER NOT NULL,
                `parentId` INTEGER,
                `name` TEXT,
                `slug` TEXT,
                PRIMARY KEY(`id`,`siteId`))
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `CouponEmails` (
                    `couponId` INTEGER NOT NULL, 
                    `siteId` INTEGER NOT NULL, 
                    `email` TEXT NOT NULL, 
                    PRIMARY KEY(`couponId`, `siteId`, `email`), 
                    FOREIGN KEY(`couponId`, `siteId`) 
                    REFERENCES `Coupons`(`id`, `siteId`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `CouponsAndProducts` (`couponId` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `isExcluded` INTEGER NOT NULL,
                    PRIMARY KEY(`couponId`,`productId`),
                    FOREIGN KEY(`couponId`,`siteId`) 
                    REFERENCES `Coupons`(`id`,`siteId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`productId`,`siteId`) 
                    REFERENCES `Products`(`id`,`siteId`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE TABLE IF NOT EXISTS `CouponsAndProductCategories` (
                    `couponId` INTEGER NOT NULL,
                    `siteId` INTEGER NOT NULL,
                    `productCategoryId` INTEGER NOT NULL,
                    `isExcluded` INTEGER NOT NULL,
                    PRIMARY KEY(`couponId`, `productCategoryId`),
                    FOREIGN KEY(`couponId`, `siteId`) REFERENCES `Coupons`(`id`, `siteId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE ,
                    FOREIGN KEY(`productCategoryId`, `siteId`) 
                    REFERENCES `ProductCategories`(`id`, `siteId`) 
                    ON UPDATE NO ACTION ON DELETE CASCADE )
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_Coupons_id_siteId` ON `Coupons` (`id`, `siteId`);
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponEmails_couponId_siteId_email` 
                    ON `CouponEmails` (`couponId`, `siteId`, `email`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProducts_couponId_siteId` 
                    ON `CouponsAndProducts` (`couponId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProducts_productId_siteId` 
                    ON `CouponsAndProducts` (`productId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProductCategories_couponId_siteId` 
                    ON `CouponsAndProductCategories` (`couponId`, `siteId`)
                """.trimIndent()
            )

            execSQL(
                // language=RoomSql
                """CREATE INDEX IF NOT EXISTS `index_CouponsAndProductCategories_productCategoryId_siteId` 
                    ON `CouponsAndProductCategories` (`productCategoryId`, `siteId`)
                """.trimIndent()
            )
        }
    }
}

internal val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.apply {
            execSQL("DROP TABLE OrderEntity")
            execSQL(
                    // language=RoomSql
                    """ 
                        CREATE TABLE IF NOT EXISTS OrderEntity (
                        `localSiteId` INTEGER NOT NULL,
                        `orderId` INTEGER NOT NULL,
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
                        `refundTotal` TEXT NOT NULL,
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
                        `metaData` TEXT NOT NULL,
                        `taxLines` TEXT NOT NULL,
                         PRIMARY KEY(`localSiteId`, `orderId`)
                        )
                    """.trimIndent()
            )
            execSQL(
                    // language=RoomSql
                    """
                        CREATE INDEX IF NOT EXISTS `index_OrderEntity_localSiteId_orderId` 
                        ON `OrderEntity` (`localSiteId`, `orderId`);
                    """.trimIndent()
            )
        }
    }
}
