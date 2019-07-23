package org.wordpress.android.fluxc.model.order

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCOrderModel

@Parcelize
data class OrderAddress(
    val firstName: String,
    val lastName: String,
    val company: String,
    val address1: String,
    val address2: String,
    val city: String,
    val state: String,
    val postcode: String,
    val country: String
): Parcelable {
    companion object {
        fun getBillingAddress(orderModel: WCOrderModel): OrderAddress {
            return OrderAddress(
                firstName = orderModel.billingFirstName,
                lastName = orderModel.billingLastName,
                company = orderModel.billingCompany,
                address1 = orderModel.billingAddress1,
                address2 = orderModel.billingAddress2,
                city = orderModel.billingCity,
                state = orderModel.billingState,
                postcode = orderModel.billingPostcode,
                country = orderModel.billingCountry
            )
        }

        fun getShippingAddress(orderModel: WCOrderModel): OrderAddress {
            return OrderAddress(
                firstName = orderModel.shippingFirstName,
                lastName = orderModel.shippingLastName,
                company = orderModel.shippingCompany,
                address1 = orderModel.shippingAddress1,
                address2 = orderModel.shippingAddress2,
                city = orderModel.shippingCity,
                state = orderModel.shippingState,
                postcode = orderModel.shippingPostcode,
                country = orderModel.shippingCountry
            )
        }
    }
}
