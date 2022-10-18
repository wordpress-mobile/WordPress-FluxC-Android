package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

data class WCMetaData(
    @SerializedName("id") val id: Long,
    @SerializedName("key") val key: String?,
    @SerializedName("value") val value: Any,
    @SerializedName("display_key") val displayKey: String?,
    @SerializedName("display_value") val displayValue: Any?
)
