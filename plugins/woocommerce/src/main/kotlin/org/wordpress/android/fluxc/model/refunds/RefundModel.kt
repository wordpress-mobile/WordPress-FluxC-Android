package org.wordpress.android.fluxc.model.refunds

import java.math.BigDecimal
import java.util.Date

data class RefundModel(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<RefundItem>
) {
    data class RefundItem(
        val itemId: Long,
        val name: String,
        val productId: Long,
        val variationId: Long,
        val quantity: Float,
        val subtotal: BigDecimal,
        val total: BigDecimal,
        val totalTax: BigDecimal,
        val sku: String,
        val price: BigDecimal
    )
}
