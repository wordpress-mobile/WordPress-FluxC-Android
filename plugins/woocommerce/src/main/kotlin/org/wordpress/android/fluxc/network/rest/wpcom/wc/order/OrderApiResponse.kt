package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName")
class OrderApiResponse : Response {
    val number: Long? = null
    val status: String? = null
    val currency: String? = null
    val date_created_gmt: String? = null
    val total: Float? = null
    val billing: Billing? = null

    class Billing {
        val first_name: String? = null
        val last_name: String? = null
    }
}
