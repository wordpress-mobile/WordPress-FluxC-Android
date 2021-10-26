package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.WCOrderModel

sealed class OrderAddress {
    abstract val firstName: String
    abstract val lastName: String
    abstract val company: String
    abstract val address1: String
    abstract val address2: String
    abstract val city: String
    abstract val state: String
    abstract val postcode: String
    abstract val country: String
    abstract val phone: String

    data class Shipping(
        override val firstName: String,
        override val lastName: String,
        override val company: String,
        override val address1: String,
        override val address2: String,
        override val city: String,
        override val state: String,
        override val postcode: String,
        override val country: String,
        override val phone: String
    ) : OrderAddress() {
        constructor(orderModel: WCOrderModel) : this(
                firstName = orderModel.shippingFirstName,
                lastName = orderModel.shippingLastName,
                company = orderModel.shippingCompany,
                address1 = orderModel.shippingAddress1,
                address2 = orderModel.shippingAddress2,
                city = orderModel.shippingCity,
                state = orderModel.shippingState,
                postcode = orderModel.shippingPostcode,
                country = orderModel.shippingCountry,
                phone = orderModel.shippingPhone
        )
    }

    data class Billing(
        val email: String,
        override val firstName: String,
        override val lastName: String,
        override val company: String,
        override val address1: String,
        override val address2: String,
        override val city: String,
        override val state: String,
        override val postcode: String,
        override val country: String,
        override val phone: String
    ) : OrderAddress() {
        constructor(orderModel: WCOrderModel) : this(
                email = orderModel.billingEmail,
                firstName = orderModel.billingFirstName,
                lastName = orderModel.billingLastName,
                company = orderModel.billingCompany,
                address1 = orderModel.billingAddress1,
                address2 = orderModel.billingAddress2,
                city = orderModel.billingCity,
                state = orderModel.billingState,
                postcode = orderModel.billingPostcode,
                country = orderModel.billingCountry,
                phone = orderModel.billingPhone
        )
    }
}
