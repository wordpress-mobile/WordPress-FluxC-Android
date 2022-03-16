package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ProductImageDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("date_created") val dateCreated: Date? = null,
    @SerializedName("src") val url: String? = null,
    @SerializedName("alt") var alternativeInformation: String? = null,
    @SerializedName("name") var name: String? = null
)
