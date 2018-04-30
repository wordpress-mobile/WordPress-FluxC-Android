package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Singleton

@Singleton
class OrderStatsRestClient(
    appContext: Context,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    enum class OrderStatsApiUnit {
        DAY, WEEK, MONTH, YEAR;

        override fun toString() = name.toLowerCase()
    }

    /**
     * Makes a GET call to `/wpcom/v2/sites/$site/data/orders/`, retrieving data for the given
     * WooCommerce [SiteModel].
     *
     * @param[site] the site to fetch order data for
     * @param[unit] one of 'day', 'week', 'month', or 'year'
     * @param[date] the latest date to include in the results. Should match the [unit], e.g.:
     * 'day':'1955-11-05', 'week':'1955-W44', 'month':'1955-11', 'year':'1955'
     * @param[quantity] how many [unit]s to fetch
     */
    fun fetchStats(site: SiteModel, unit: OrderStatsApiUnit, date: String, quantity: Int) {
        val url = WPCOMV2.sites.site(site.siteId).stats.orders.url
        val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString())

        val request = WPComGsonRequest.buildGetRequest(url, params, OrderStatsApiResponse::class.java,
                { apiResponse ->
                    // TODO: Process response and dispatch event
                },
                { networkError ->
                    // TODO: Dispatch error event
                })
        add(request)
    }
}
