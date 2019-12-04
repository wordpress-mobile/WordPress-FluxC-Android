package org.wordpress.android.fluxc.model.refunds

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.util.Date

data class WCRefundModel(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<WCRefundItem>
) {
    data class WCRefundItem(
        val itemId: Long,
        @SerializedName("qty")
        val quantity: Int,
        @SerializedName("refund_total")
        val subtotal: BigDecimal,
        @SerializedName("refund_tax")
        val totalTax: BigDecimal,
        val name: String? = null,
        val productId: Long? = null,
        val variationId: Long? = null,
        val total: BigDecimal? = null,
        val sku: String? = null,
        val price: BigDecimal? = null
    )
}
