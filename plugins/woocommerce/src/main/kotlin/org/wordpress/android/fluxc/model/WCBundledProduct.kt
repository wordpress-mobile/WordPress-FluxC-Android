package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

@Suppress("LongParameterList")
class WCBundledProduct(
    @SerializedName("bundled_item_id") val id: Long,
    @SerializedName("product_id") val bundledProductId: Long,
    @SerializedName("menu_order") val menuOrder: Int,
    @SerializedName("title") val title: String,
    @SerializedName("stock_status") val stockStatus: String,
    @SerializedName("quantity_min") val quantityMin: Long,
    @SerializedName("quantity_max") val quantityMax: Long,
    @SerializedName("quantity_default") val quantityDefault: Long = 0,
    @SerializedName("optional") val isOptional: Boolean = false,
)
