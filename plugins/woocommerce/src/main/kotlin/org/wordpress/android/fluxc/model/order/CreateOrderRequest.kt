package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.WCOrderStatusModel

data class CreateOrderRequest(
    val status: WCOrderStatusModel,
    val lineItems: List<LineItem>,
    val shippingAddress: OrderAddress.Shipping,
    val billingAddress: OrderAddress.Billing
)
