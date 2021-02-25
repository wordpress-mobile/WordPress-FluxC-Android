package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto

import org.wordpress.android.fluxc.model.WCCustomerModel
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class CustomerApiResponse : Response {
    val avatar_url: String? = null
    val billing: Billing? = null
    val date_created: String? = null
    val date_created_gmt: String? = null
    val date_modified: String? = null
    val date_modified_gmt: String? = null
    val email: String? = null
    val first_name: String? = null
    val id: Long = 0
    val is_paying_customer: Boolean = false
    val last_name: String? = null
    val role: String? = null
    val shipping: Shipping? = null
    val username: String? = null

    data class Billing(
        val address_1: String? = null,
        val address_2: String? = null,
        val city: String? = null,
        val company: String? = null,
        val country: String? = null,
        val email: String? = null,
        val first_name: String? = null,
        val last_name: String? = null,
        val phone: String? = null,
        val postcode: String? = null,
        val state: String? = null
    )

    data class Shipping(
        val address_1: String? = null,
        val address_2: String? = null,
        val city: String? = null,
        val company: String? = null,
        val country: String? = null,
        val first_name: String? = null,
        val last_name: String? = null,
        val postcode: String? = null,
        val state: String? = null
    )

    fun asCustomerModel(): WCCustomerModel {
        val resp = this
        return WCCustomerModel().apply {
            avatarUrl = resp.avatar_url
            dateCreated = resp.date_created
            dateCreatedGmt = resp.date_created_gmt
            dateModified = resp.date_modified
            dateModifiedGmt = resp.date_modified_gmt
            email = resp.email
            firstName = resp.first_name
            remoteCustomerId = resp.id
            isPayingCustomer = resp.is_paying_customer
            lastName = resp.last_name
            role = resp.role
            username = resp.username

            billingAddress1 = resp.billing?.address_1
            billingAddress2 = resp.billing?.address_2
            billingCompany = resp.billing?.company
            billingCountry = resp.billing?.country
            billingCity = resp.billing?.city
            billingEmail = resp.billing?.email
            billingFirstName = resp.billing?.first_name
            billingLastName = resp.billing?.last_name
            billingPhone = resp.billing?.phone
            billingPostcode = resp.billing?.postcode
            billingState = resp.billing?.state

            shippingAddress1 = resp.billing?.address_1
            shippingAddress2 = resp.billing?.address_2
            shippingCity = resp.billing?.city
            shippingCompany = resp.billing?.company
            shippingCountry = resp.billing?.country
            shippingFirstName = resp.billing?.first_name
            shippingLastName = resp.billing?.last_name
            shippingPostcode = resp.billing?.postcode
            shippingState = resp.billing?.state
        }
    }
}
