package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.isInternalAttribute

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
    @SerializedName("id") val id: Long,
    @SerializedName("localSiteId") val localSiteId: LocalOrRemoteId,
    @SerializedName("orderId") val orderId: Long,
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: String,
    @SerializedName("display_key") val displayKey: String?,
    @SerializedName("display_value") val displayValue: String?
)

/**
 * Creates a list of OrderMetaDataEntity from a "fat" order model, which is the order before
 * calling [StripOrder] to remove most of the metadata
 */
fun fromFatOrder(fatModel: OrderEntity): List<OrderMetaDataEntity> {
    val metaData = fatModel.getMetaDataList()
        .filter {
            it.isInternalAttribute.not()
        }
    return ArrayList<OrderMetaDataEntity>().also { list ->
        metaData.forEach { meta ->
            list.add(
                OrderMetaDataEntity(
                    id = meta.id,
                    localSiteId = fatModel.localSiteId,
                    orderId = fatModel.orderId,
                    key = meta.key,
                    value = meta.value.toString(),
                    displayKey = meta.displayKey,
                    displayValue = meta.displayKey
                )
            )
        }
    }
}
