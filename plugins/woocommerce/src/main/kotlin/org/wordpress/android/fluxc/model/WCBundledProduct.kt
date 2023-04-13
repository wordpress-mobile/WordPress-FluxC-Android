package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

class WCBundledProduct(
    @SerializedName("bundled_item_id") val id: Long,
    @SerializedName("product_id") val bundledProductId: Long,
    @SerializedName("menu_order") val menuOrder: Int,
    @SerializedName("title") val title: String,
    @SerializedName("stock_status") val stockStatus: String
)
