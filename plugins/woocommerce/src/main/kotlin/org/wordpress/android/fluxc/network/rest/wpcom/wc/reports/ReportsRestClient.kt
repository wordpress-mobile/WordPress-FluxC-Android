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

    private fun createParameters(
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
}
