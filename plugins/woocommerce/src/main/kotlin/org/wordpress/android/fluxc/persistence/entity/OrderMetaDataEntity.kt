package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
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
    primaryKeys = ["localSiteId", "orderId", "id"],
    foreignKeys = [ForeignKey(
        entity = OrderEntity::class,
        parentColumns = ["localSiteId", "orderId"],
        childColumns = ["localSiteId", "orderId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class OrderMetaDataEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: LocalId,
    val id: Long,
    val orderId: Long,
    val key: String,
    val value: String,
    @ColumnInfo(defaultValue = "1")
    val isDisplayable: Boolean = true
) {
    fun toDomainModel() = WCMetaData(
        id = id,
        key = key,
        value = JsonParser().parse(value)
    )
}
