package org.wordpress.android.fluxc.network.rest.wpcom.wc.reports

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class ReportsRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchTopPerformerProducts(
        site: SiteModel,
        startDate: String,
        endDate: String,
        quantity: Int = 5
    ): WooPayload<Array<ReportsProductApiResponse>> {
        fun createParameters(
            startDate: String,
            endDate: String,
            quantity: Int
        ) = mapOf(
            "before" to endDate,
            "after" to startDate,
            "per_page" to quantity.toString(),
            "extended_info" to "true",
            "orderby" to "items_sold",
            "order" to "desc"
        ).filter { it.value.isNotEmpty() }

        val url = WOOCOMMERCE.reports.products.pathV4Analytics
        val parameters = createParameters(startDate, endDate, quantity)

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ReportsProductApiResponse>::class.java,
            params = parameters
        )

        return response.toWooPayload()
    }

    suspend fun fetchProductStockReport(
        site: SiteModel,
        stockStatus: String,
        page: Int = 1,
        quantity: Int = 3
    ): WooPayload<Array<ProductStockItemApiResponse>> {
        val url = WOOCOMMERCE.reports.stock.pathV4Analytics
        val parameters = mapOf(
            "page" to page.toString(),
            "per_page" to quantity.toString(),
            "order" to "asc",
            "type" to stockStatus
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ProductStockItemApiResponse>::class.java,
            params = parameters
        )

        return response.toWooPayload()
    }
}
