package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class OrderSummaryApiResponse : Response {
    val id: Long? = null

    @SerializedName("date_created_gmt")
    val dateCreatedGmt: String? = null
}
