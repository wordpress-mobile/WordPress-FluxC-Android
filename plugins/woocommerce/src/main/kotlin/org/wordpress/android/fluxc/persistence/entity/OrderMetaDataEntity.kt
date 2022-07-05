package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto

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

fun fromOrderDto(orderDto: OrderDto, localSiteId: LocalId): List<OrderMetaDataEntity> {
    val responseType = object : TypeToken<List<WCMetaData>>() {}.type
    val metaData = Gson().fromJson(orderDto.meta_data, responseType) as? List<WCMetaData> ?: emptyList()
    return ArrayList<OrderMetaDataEntity>().also { list ->
        metaData.forEach { meta ->
            list.add(
                OrderMetaDataEntity(
                    id = meta.id,
                    localSiteId = localSiteId,
                    orderId = orderDto.id ?: 0,
                    key = meta.key,
                    value = meta.value.toString(),
                    displayKey = meta.displayKey,
                    displayValue = meta.displayKey
                )
            )
        }
    }
}
