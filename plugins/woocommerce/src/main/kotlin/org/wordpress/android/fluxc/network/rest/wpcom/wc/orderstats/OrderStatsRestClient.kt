package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
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
        HOUR, DAY, WEEK, MONTH, YEAR;

        companion object {
            fun fromStatsGranularity(granularity: StatsGranularity): OrderStatsApiUnit {
                return when (granularity) {
                    StatsGranularity.DAYS -> DAY
                    StatsGranularity.WEEKS -> WEEK
                    StatsGranularity.MONTHS -> MONTH
                    StatsGranularity.YEARS -> YEAR
                }
            }

            /**
             * Based on the design changes, when:
             *  `Today` tab is selected: [OrderStatsApiUnit] field passed to the API should be [HOUR]
             *  `This week` tab is selected: [OrderStatsApiUnit] field passed to the API should be [DAY]
             *  `This month` tab is selected: [OrderStatsApiUnit] field passed to the API should be [DAY]
             *  `This year` tab is selected: [OrderStatsApiUnit] field passed to the API should be [MONTH]
             */
            fun convertToRevenueStatsInterval(granularity: StatsGranularity): OrderStatsApiUnit {
                return when (granularity) {
                    StatsGranularity.DAYS -> HOUR
                    StatsGranularity.WEEKS -> DAY
                    StatsGranularity.MONTHS -> DAY
                    StatsGranularity.YEARS -> MONTH
                }
            }
        }

        override fun toString() = name.toLowerCase()
    }

    private final val STATS_FIELDS = "data,fields"
    private val STATS_DEFAULT_ORDER = "asc"

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
    fun fetchStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        date: String,
        quantity: Int,
        force: Boolean = false,
        startDate: String? = null,
        endDate: String? = null
    ) {
        val url = WPCOMV2.sites.site(site.siteId).stats.orders.url
        val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString(),
                "_fields" to STATS_FIELDS)

        val request = WPComGsonRequest.buildGetRequest(url, params, OrderStatsApiResponse::class.java,
                { apiResponse ->
                    apiResponse?.let {
                        val model = WCOrderStatsModel().apply {
                            this.localSiteId = site.id
                            this.unit = unit.toString()
                            this.fields = apiResponse.fields.toString()
                            this.data = apiResponse.data.toString()
                            this.quantity = quantity.toString()
                            this.date = date
                            endDate?.let { this.endDate = it }
                            startDate?.let {
                                this.startDate = startDate
                                this.isCustomField = true
                            }
                        }
                        val payload = FetchOrderStatsResponsePayload(site, unit, model)
                        mDispatcher.dispatch(WCStatsActionBuilder.newFetchedOrderStatsAction(payload))
                    } ?: run {
                        AppLog.e(T.API, "Response for url $url with param $params is null: $apiResponse")
                        val orderError = OrderStatsError(OrderStatsErrorType.RESPONSE_NULL, "Response object is null")
                        val payload = FetchOrderStatsResponsePayload(orderError, site, unit)
                        mDispatcher.dispatch(WCStatsActionBuilder.newFetchedOrderStatsAction(payload))
                    }
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

    /**
     * Makes a GET call to `/wc/v4/reports/revenue/stats`, retrieving data for the given
     * WooCommerce [SiteModel].
     *
     * @param[site] the site to fetch stats data for
     * @param[granularity] one of 'hour', 'day', 'week', 'month', or 'year'
     * @param[startDate] the start date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     * @param[endDate] the end date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     *
     * Possible non-generic errors:
     * [OrderStatsErrorType.INVALID_PARAM] if [granularity], [startDate], or [endDate] are invalid or incompatible
     */
    fun fetchStatsV4(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        perPage: Int,
        force: Boolean = false
    ) {
        val url = WOOCOMMERCE.reports.revenue.stats.pathV4
        val responseType = object : TypeToken<RevenueStatsApiResponse>() {}.type
        val params = mapOf(
                "interval" to OrderStatsApiUnit.convertToRevenueStatsInterval(granularity).toString(),
                "after" to startDate,
                "before" to endDate,
                "per_page" to perPage.toString(),
                "order" to STATS_DEFAULT_ORDER)

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: RevenueStatsApiResponse? ->
                    response?.let {
                        val model = WCRevenueStatsModel().apply {
                            this.localSiteId = site.id
                            this.interval = granularity.toString()
                            this.data = response.intervals.toString()
                            this.startDate = startDate
                            this.endDate = endDate
                        }
                        val payload = FetchRevenueStatsResponsePayload(site, granularity, model)
                        mDispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAction(payload))
                    } ?: run {
                        AppLog.e(T.API, "Response for url $url with param $params is null: $response")
                        val orderError = OrderStatsError(OrderStatsErrorType.RESPONSE_NULL, "Response object is null")
                        val payload = FetchRevenueStatsResponsePayload(orderError, site, granularity)
                        mDispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchRevenueStatsResponsePayload(orderError, site, granularity)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })

        request?.enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
        if (force) request?.setShouldForceUpdate()

        add(request)
    }

    fun fetchVisitorStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        date: String,
        quantity: Int,
        force: Boolean = false
    ) {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1
        val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString(),
                "stat_fields" to "visitors")
        val request = WPComGsonRequest
                .buildGetRequest(url, params, VisitorStatsApiResponse::class.java,
                        { response ->
                            val visits = getVisitorsFromResponse(response)
                            val payload = FetchVisitorStatsResponsePayload(
                                    site = site,
                                    apiUnit = unit,
                                    visits = visits
                            )
                            mDispatcher.dispatch(WCStatsActionBuilder.newFetchedVisitorStatsAction(payload))
                        },
                        { networkError ->
                            val orderError = networkErrorToOrderError(networkError)
                            val payload = FetchVisitorStatsResponsePayload(orderError, site, unit)
                            mDispatcher.dispatch(WCStatsActionBuilder.newFetchedVisitorStatsAction(payload))
                        })

        request.enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
        if (force) request.setShouldForceUpdate()

        add(request)
    }

    /**
     * Returns the number of visitors from the VisitorStatsApiResponse data, which is an array of items for
     * each period, the first element of which contains the date and the second contains the visitor count
     */
    private fun getVisitorsFromResponse(response: VisitorStatsApiResponse): Int {
        return try {
            response.data?.asJsonArray?.map { it.asJsonArray?.get(1)?.asInt ?: 0 }?.sum() ?: 0
        } catch (e: Exception) {
            AppLog.e(T.API, "${e.javaClass.simpleName} parsing visitor stats", e)
            0
        }
    }

    fun fetchTopEarnersStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        date: String,
        limit: Int,
        force: Boolean = false
    ) {
        val url = WPCOMV2.sites.site(site.siteId).stats.top_earners.url
        val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "limit" to limit.toString())

        val request = WPComGsonRequest.buildGetRequest(url, params, TopEarnersStatsApiResponse::class.java,
                { response: TopEarnersStatsApiResponse ->
                    val wcTopEarners = response.data?.map {
                        WCTopEarnerModel().apply {
                            id = it.id ?: 0
                            currency = it.currency ?: ""
                            image = it.image ?: ""
                            name = it.name ?: ""
                            price = it.price ?: 0.0
                            quantity = it.quantity ?: 0
                            total = it.total ?: 0.0
                        }
                    } ?: emptyList()

                    val payload = FetchTopEarnersStatsResponsePayload(site, unit, wcTopEarners)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedTopEarnersStatsAction(payload))
                },
                { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchTopEarnersStatsResponsePayload(orderError, site, unit)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedTopEarnersStatsAction(payload))
                })

        request.enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
        if (force) request.setShouldForceUpdate()

        add(request)
    }

    private fun networkErrorToOrderError(wpComError: WPComGsonNetworkError): OrderStatsError {
        val orderStatsErrorType = when (wpComError.apiError) {
            "rest_invalid_param" -> OrderStatsErrorType.INVALID_PARAM
            "rest_no_route" -> OrderStatsErrorType.PLUGIN_NOT_ACTIVE
            else -> OrderStatsErrorType.fromString(wpComError.apiError)
        }
        return OrderStatsError(orderStatsErrorType, wpComError.message)
    }
}
