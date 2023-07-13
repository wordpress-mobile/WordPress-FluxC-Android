package org.wordpress.android.fluxc.network.rest.wpcom.wc.reports

import com.google.gson.annotations.SerializedName

data class ReportsProductApiResponse(
    @SerializedName("product_id")
    val productId: Long? = null,
    @SerializedName("items_sold")
    val itemsSold: Int? = null,
    @SerializedName("net_revenue")
    val netRevenue: Double? = null,
    @SerializedName("orders_count")
    val ordersCount: Long? = null,
    @SerializedName("extended_info")
    val product: ReportProductItem? = null
)

data class ReportProductItem(
    val name: String? = null,
    val price: Double? = null,
    @SerializedName("image")
    val imageHTML: String? = null
) {
    companion object {
        private val SRC_REGEX = Regex("src=\"(.*?)\"")
    }

    fun getImageUrl() = imageHTML
        ?.let {
            SRC_REGEX.find(it)?.value
                ?.replace("src=", "")
                ?.replace("\"", "")
        }
}

