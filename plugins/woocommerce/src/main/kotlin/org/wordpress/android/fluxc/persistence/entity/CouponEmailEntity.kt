package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "CouponEmails",
    foreignKeys = [
        ForeignKey(
            entity = CouponEntity::class,
            parentColumns = ["id", "siteId"],
            childColumns = ["couponId", "siteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["couponId", "siteId", "email"],
    indices = [Index("couponId", "siteId", "email")]
)
data class CouponEmailEntity(
    val couponId: Long,
    val siteId: Long,
    val email: String
)
