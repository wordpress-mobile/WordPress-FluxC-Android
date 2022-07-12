package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData

/**
 * The OrderMetaDataEntity table is used to store viewable order metadata. Order metadata
 * can potentially be quite large, so we keep it separate from the order.
 */
@Entity(
    tableName = "OrderMetaData",
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
) {
    constructor(orderId: Long, localSiteId: LocalId, sourceMetadata: WCMetaData): this(
        localSiteId = localSiteId,
        id = sourceMetadata.id,
        orderId = orderId,
        key = sourceMetadata.key,
        value = sourceMetadata.value.toString(),
        displayKey = sourceMetadata.displayKey,
        displayValue = sourceMetadata.displayValue.toString()
    )
}
