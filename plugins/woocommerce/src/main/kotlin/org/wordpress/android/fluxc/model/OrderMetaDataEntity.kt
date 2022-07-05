package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "OrderMetaDataEntity",
    indices = [Index(
        value = ["localSiteId", "orderId"]
    )],
    primaryKeys = ["localSiteId", "orderId", "id"]
)
data class OrderMetaDataEntity(
    @SerializedName("id") val id: Long,
    @SerializedName("localSiteId") val localSiteId: Long,
    @SerializedName("orderId") val orderId: Long,
    @SerializedName("key") val key: String,
    @SerializedName("value") val value: Any,
    @SerializedName("display_key") val displayKey: String?,
    @SerializedName("display_value") val displayValue: Any?
)
