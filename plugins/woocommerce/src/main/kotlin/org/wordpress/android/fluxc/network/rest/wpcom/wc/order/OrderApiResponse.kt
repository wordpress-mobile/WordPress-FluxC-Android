package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class OrderApiResponse : Response {
    class Billing {
        val first_name: String? = null
        val last_name: String? = null
        val company: String? = null
        val address_1: String? = null
        val address_2: String? = null
        val city: String? = null
        val state: String? = null
        val postcode: String? = null
        val country: String? = null
        val email: String? = null
        val phone: String? = null
    }

    class Shipping {
        val first_name: String? = null
        val last_name: String? = null
        val company: String? = null
        val address_1: String? = null
        val address_2: String? = null
        val city: String? = null
        val state: String? = null
        val postcode: String? = null
        val country: String? = null
    }

    val number: Long? = null
    val status: String? = null
    val currency: String? = null
    val date_created_gmt: String? = null
    val total: String? = null

    val billing: Billing? = null
    val shipping: Shipping? = null
}
