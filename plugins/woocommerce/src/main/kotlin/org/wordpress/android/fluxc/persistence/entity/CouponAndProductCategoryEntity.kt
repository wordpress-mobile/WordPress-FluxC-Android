package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "CouponsAndProductCategories",
    foreignKeys = [
        ForeignKey(
                entity = CouponEntity::class,
                parentColumns = ["id", "siteId"],
                childColumns = ["couponId", "siteId"],
                onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
                entity = ProductCategoryEntity::class,
                parentColumns = ["id", "siteId"],
                childColumns = ["productCategoryId", "siteId"],
                onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["couponId", "productCategoryId"],
    indices = [Index("couponId", "siteId"), Index("productCategoryId", "siteId")]
)
data class CouponAndProductCategoryEntity(
    val couponId: Long,
    val siteId: Long,
    val productCategoryId: Long,
    val isExcluded: Boolean
)
