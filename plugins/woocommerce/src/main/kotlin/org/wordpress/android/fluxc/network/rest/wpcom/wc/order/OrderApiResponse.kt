package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
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
    val total_tax: String? = null
    val shipping_total: String? = null
    val payment_method: String? = null
    val payment_method_title: String? = null
    val prices_include_tax: Boolean = false

    val billing: Billing? = null
    val shipping: Shipping? = null

    // This is actually a list of objects. We're storing this as JSON initially, and it will be deserialized on demand.
    // See WCOrderModel.LineItem
    val line_items: JsonElement? = null
}
