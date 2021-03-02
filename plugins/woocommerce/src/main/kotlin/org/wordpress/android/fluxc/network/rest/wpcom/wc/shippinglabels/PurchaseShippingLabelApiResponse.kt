package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class PurchaseShippingLabelApiResponse(
    @SerializedName("success")
    val isSuccess: Boolean = false,
    @SerializedName("labels")
    val labels: List<LabelItem>? = null
) {
    class LabelItem {
        @SerializedName("label_id") val labelId: Long? = null
        @SerializedName("tracking") val trackingNumber: String? = null
        @SerializedName("carrier_id") val carrierId: String? = null
        @SerializedName("service_name") val serviceName: String? = null
        @SerializedName("created") val dateCreated: Long? = null
        @SerializedName("package_name") val packageName: String? = null
        @SerializedName("refundable_amount") val refundableAmount: BigDecimal? = null
        @SerializedName("status") val status: String? = null
        @SerializedName("commercial_invoice_url") val invoiceUrl: String? = null
        @SerializedName("is_letter") val isLetter: Boolean? = null
        @SerializedName("product_ids") val productIds: List<Long>? = emptyList()
        @SerializedName("product_names") val productNames: List<String>? = emptyList()
    }
}
