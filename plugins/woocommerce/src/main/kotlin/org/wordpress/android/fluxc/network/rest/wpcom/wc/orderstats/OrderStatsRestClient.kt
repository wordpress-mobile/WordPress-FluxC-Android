package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
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

        companion object {
            fun fromStatsGranularity(granularity: StatsGranularity): OrderStatsApiUnit {
                return when (granularity) {
                    StatsGranularity.DAYS -> DAY
                    StatsGranularity.WEEKS -> WEEK
                    StatsGranularity.MONTHS -> MONTH
                    StatsGranularity.YEARS -> YEAR
                }
            }
        }

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
     *
     * Possible non-generic errors:
     * [OrderStatsErrorType.INVALID_PARAM] if [unit], [date], or [quantity] are invalid or incompatible
     */
    fun fetchStats(site: SiteModel, unit: OrderStatsApiUnit, date: String, quantity: Int, force: Boolean = false) {
        val url = WPCOMV2.sites.site(site.siteId).stats.orders.url
        val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString())

        val request = WPComGsonRequest.buildGetRequest(url, params, OrderStatsApiResponse::class.java,
                { apiResponse ->
                    val model = WCOrderStatsModel().apply {
                        this.localSiteId = site.id
                        this.unit = unit.toString()
                        this.fields = apiResponse.fields.toString()
                        this.data = apiResponse.data.toString()
                    }
                    val payload = FetchOrderStatsResponsePayload(site, unit, model)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedOrderStatsAction(payload))
                },
                { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrderStatsResponsePayload(orderError, site, unit)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedOrderStatsAction(payload))
                })

        request.enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
        if (force) request.setShouldForceUpdate()

        add(request)
    }

    fun fetchTopEarners(site: SiteModel, unit: OrderStatsApiUnit, date: String, quantity: Int, force: Boolean = false) {
        val url = WPCOMV2.sites.site(site.siteId).stats.top_earners.url
        val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString())

        val request = WPComGsonRequest.buildGetRequest(url, params, TopEarnersStatsApiResponse::class.java,
                { response: TopEarnersStatsApiResponse ->
                    val payload = FetchTopEarnersStatsResponsePayload(site, unit, response.topEarners)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedTopEarnersStatsAction(payload))
                },
                { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrderStatsResponsePayload(orderError, site, unit)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedOrderStatsAction(payload))
                })

        request.enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
        if (force) request.setShouldForceUpdate()

        add(request)
    }

    private fun networkErrorToOrderError(wpComError: WPComGsonNetworkError): OrderStatsError {
        val orderStatsErrorType = when (wpComError.apiError) {
            "rest_invalid_param" -> OrderStatsErrorType.INVALID_PARAM
            else -> OrderStatsErrorType.fromString(wpComError.apiError)
        }
        return OrderStatsError(orderStatsErrorType, wpComError.message)
    }
}
