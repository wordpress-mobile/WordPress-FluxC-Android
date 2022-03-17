package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.annotations.SerializedName

data class AddProductTagResponse(
    @SerializedName("create")
    val addedTags: List<ProductTagDto>? = null
)
