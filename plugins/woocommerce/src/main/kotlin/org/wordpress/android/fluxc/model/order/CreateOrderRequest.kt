package org.wordpress.android.fluxc.model.order

data class CreateOrderRequest(
    val lineItems: List<LineItem>,
    val shippingAddress: OrderAddress.Shipping,
    val billingAddress: OrderAddress.Billing
)
