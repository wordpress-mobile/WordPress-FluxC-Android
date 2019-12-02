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
        val name: String? = null,
        val productId: Long? = null,
        val variationId: Long? = null,
        @SerializedName("qty")
        val quantity: Int,
        val subtotal: BigDecimal? = null,
        @SerializedName("refund_total")
        val total: BigDecimal,
        @SerializedName("refund_tax")
        val totalTax: BigDecimal,
        val sku: String? = null,
        val price: BigDecimal? = null
    ) {
        constructor(itemId: Long, quantity: Int, subtotal: BigDecimal, totalTax: BigDecimal) : this(
                itemId = itemId,
                quantity = quantity,
                total = subtotal,
                totalTax = totalTax
        )
    }
}
