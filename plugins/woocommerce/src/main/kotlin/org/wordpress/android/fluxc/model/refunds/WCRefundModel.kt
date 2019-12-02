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
        val name: String,
        val productId: Long,
        val variationId: Long,
        @SerializedName("qty")
        val quantity: Int,
        val subtotal: BigDecimal,
        @SerializedName("refund_total")
        val total: BigDecimal,
        @SerializedName("refund_tax")
        val totalTax: BigDecimal,
        val sku: String,
        val price: BigDecimal
    ) {
        constructor(itemId: Long, quantity: Int, total: BigDecimal, totalTax: BigDecimal) : this(
                itemId,
                "",
                0,
                0,
                quantity,
                total,
                totalTax,
                BigDecimal.ZERO,
                "",
                BigDecimal.ZERO
        )
    }
}
