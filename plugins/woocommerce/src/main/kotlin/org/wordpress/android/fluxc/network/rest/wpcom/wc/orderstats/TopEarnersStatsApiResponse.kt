package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class TopEarnersStatsApiResponse : Response {
    val date: String? = null
    val unit: String? = null
    val data: List<TopEarner>? = null

    class TopEarner {
        @SerializedName("ID")
        val id: Long? = 0
        val currency: String? = ""
        val image: String? = ""
        val name: String? = ""
        val price: Double? = 0.0
        val quantity: Int? = 0
        val total: Double? = 0.0
    }
}
