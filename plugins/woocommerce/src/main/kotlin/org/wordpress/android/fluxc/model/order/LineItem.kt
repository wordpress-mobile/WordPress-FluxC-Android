package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.WCMetaData

data class LineItem(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("parent_name")
    val parentName: String? = null,
    @SerializedName("product_id")
    var productId: Long? = null,
    @SerializedName("variation_id")
    val variationId: Long? = null,
    var quantity: Float? = null,
    val subtotal: String? = null,
    val total: String? = null, // Price x quantity
    @SerializedName("total_tax")
    val totalTax: String? = null,
    val sku: String? = null,
    val price: String? = null, // The per-item price

    @SerializedName("meta_data")
    val metaData: List<WCMetaData>? = null
) {
    class Attribute(val key: String?, val value: String?)

    fun getAttributeList(): List<Attribute> {
        return metaData?.filter {
            it.displayKey is String && it.displayValue is String
        }?.map {
            Attribute(it.displayKey, it.displayValue as String)
        } ?: emptyList()
    }
}
