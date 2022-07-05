package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.LocalOrRemoteId

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
