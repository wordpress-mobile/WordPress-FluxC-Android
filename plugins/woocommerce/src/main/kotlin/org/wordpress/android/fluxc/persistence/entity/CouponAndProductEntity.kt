package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "CouponsAndProducts",
    foreignKeys = [
        ForeignKey(
                entity = CouponEntity::class,
                parentColumns = arrayOf("id", "siteId"),
                childColumns = arrayOf("couponId", "siteId"),
                onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
                entity = ProductEntity::class,
                parentColumns = arrayOf("id", "siteId"),
                childColumns = arrayOf("productId", "siteId"),
                onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["couponId", "productId"],
    indices = [Index("couponId", "siteId"), Index("productId", "siteId")]
)
data class CouponAndProductEntity(
    val couponId: Long,
    val siteId: Long,
    val productId: Long,
    val isExcluded: Boolean
)
