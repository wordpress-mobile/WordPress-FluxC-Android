package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

class ShippingLine {
    val id: Long? = null
    val total: String? = null
    @SerializedName("total_tax")
    val totalTax: String? = null
    @SerializedName("method_id")
    val methodId: String? = null
    @SerializedName("method_title")
    val methodTitle: String? = null
}
