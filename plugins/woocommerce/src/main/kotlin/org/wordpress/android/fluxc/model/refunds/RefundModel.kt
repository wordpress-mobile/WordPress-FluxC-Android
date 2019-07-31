package org.wordpress.android.fluxc.model.refunds

import java.math.BigDecimal
import java.util.Date

data class RefundModel(
    val id: Int,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<RefundItem>
) {
    data class RefundItem(
        val itemId: Int,
        val name: String,
        val productId: Int,
        val variationId: Int,
        val quantity: Int,
        val taxClass: Int,
        val subtotal: BigDecimal,
        val subtotalTax: BigDecimal,
        val total: BigDecimal,
        val totalTax: BigDecimal,
        val sku: String,
        val price: BigDecimal
    )
}
