package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response
import java.math.BigDecimal

class ShippingLabelApiResponse : Response {
    val orderId: Long? = null
    val paperSize: String? = null

    val formData: JsonElement? = null
    val storeOptions: JsonElement? = null
    val labelsData: List<LabelItem>? = null

    val success: Boolean? = null

    class LabelItem {
        @SerializedName("label_id") val labelId: Long? = null
        @SerializedName("tracking") val trackingNumber: String? = null
        @SerializedName("carrier_id") val carrierId: String? = null
        @SerializedName("service_name") val serviceName: String? = null
        @SerializedName("created_date") val dateCreated: Long? = null
        @SerializedName("package_name") val packageName: String? = null
        @SerializedName("product_names") val productNames: List<String>? = emptyList()
        @SerializedName("product_ids") val productIds: List<Long>? = emptyList()
        @SerializedName("refundable_amount") val refundableAmount: BigDecimal? = null
        val status: String? = null
        val rate: BigDecimal? = null
        val currency: String? = null
        val refund: JsonElement? = null
    }
}
