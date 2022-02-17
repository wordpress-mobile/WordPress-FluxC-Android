package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.WCOrderStatusModel

data class UpdateOrderRequest(
    val status: WCOrderStatusModel? = null,
    val lineItems: List<LineItem>? = null,
    val shippingAddress: OrderAddress.Shipping? = null,
    val billingAddress: OrderAddress.Billing? = null,
    val feeLines: List<FeeLine>? = null,
    val shippingLines: List<ShippingLine>? = null,
    val customerNote: String? = null
)
