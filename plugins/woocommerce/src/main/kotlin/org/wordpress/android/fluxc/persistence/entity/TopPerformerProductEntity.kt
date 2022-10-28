package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity

@Entity(
    tableName = "TopPerformerProducts",
    primaryKeys = ["datePeriod", "productId", "siteId"]
)
data class TopPerformerProductEntity(
    val siteId: Long,
    val datePeriod: String,
    val productId: Long,
    val name: String,
    val imageUrl: String?,
    val quantity: Int,
    val currency: String,
    val total: Double,
    val millisSinceLastUpdated: Long
)
