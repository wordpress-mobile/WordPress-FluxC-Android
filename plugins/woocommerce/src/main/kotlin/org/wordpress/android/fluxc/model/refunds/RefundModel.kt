package org.wordpress.android.fluxc.model.refunds

import java.math.BigDecimal
import java.util.Date

data class RefundModel(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean
)
