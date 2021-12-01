package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.WCOrderModel.LineItem

data class CreateOrderRequest(
    val lineItems: List<LineItem>,
    val shippingAddress: OrderAddress.Shipping,
    val billingAddress: OrderAddress.Billing
)
