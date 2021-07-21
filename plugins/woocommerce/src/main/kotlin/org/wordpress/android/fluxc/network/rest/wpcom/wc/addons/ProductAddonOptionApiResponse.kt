package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.ProductAddonApiResponse.AddOnPriceType

class ProductAddonOptionApiResponse {
    @SerializedName("price_type")
    val priceType: AddOnPriceType? = null

    val label: String? = null
    val price: String? = null
    val image: String? = null
}