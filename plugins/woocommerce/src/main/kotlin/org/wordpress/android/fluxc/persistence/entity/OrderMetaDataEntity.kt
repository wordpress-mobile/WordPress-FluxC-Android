package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

/**
 * The OrderMetaDataEntity table is used to store viewable order meta data separately from the order
 */
@Entity(
    tableName = "OrderMetaDataEntity",
    indices = [Index(
        value = ["localSiteId", "orderId"]
    )],
    primaryKeys = ["localSiteId", "orderId", "id"]
)
data class OrderMetaDataEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: LocalId,
    val id: Long,
    val orderId: Long,
    val key: String,
    val value: String,
    val displayKey: String?,
    val displayValue: String?
)
