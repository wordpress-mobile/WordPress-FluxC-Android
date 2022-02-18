package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
        indices = [Index("couponId", "siteId")]
)
data class CouponEmailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val couponId: Long,
    val siteId: Long,
    val email: String
)
