package org.wordpress.android.fluxc.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.utils.JsonElementToLongSerializerDeserializer

@Suppress("LongParameterList")
class WCBundledProduct(
    @SerializedName("bundled_item_id") val id: Long,
    @SerializedName("product_id") val bundledProductId: Long,
    @SerializedName("menu_order") val menuOrder: Int,
    @SerializedName("title") val title: String,
    @SerializedName("stock_status") val stockStatus: String,
    @JsonAdapter(JsonElementToLongSerializerDeserializer::class)
    @SerializedName("quantity_min") val quantityMin: Long?,
    @JsonAdapter(JsonElementToLongSerializerDeserializer::class)
    @SerializedName("quantity_max") val quantityMax: Long?,
    @JsonAdapter(JsonElementToLongSerializerDeserializer::class)
    @SerializedName("quantity_default") val quantityDefault: Long?,
    @SerializedName("optional") val isOptional: Boolean
)

fun WCBundledProduct.isConfigurable(): Boolean {
    return (quantityMin != quantityMax) || (quantityMin == null) || isOptional
}
