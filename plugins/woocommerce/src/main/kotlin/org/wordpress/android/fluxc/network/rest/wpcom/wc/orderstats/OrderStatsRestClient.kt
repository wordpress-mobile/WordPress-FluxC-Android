package org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.model.WCVisitorStatsModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class OrderStatsRestClient @Inject constructor(
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wooNetwork: WooNetwork,
    private val wpComNetwork: WPComNetwork,
    private val coroutineEngine: CoroutineEngine
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

            /**
             * Based on the design changes, when:
             *  `Today` tab is selected: [OrderStatsApiUnit] field passed to the visitor stats API should be [DAY]
             *  `This week` tab is selected: [OrderStatsApiUnit] field passed to the visitor stats API should be [DAY]
             *  `This month` tab is selected: [OrderStatsApiUnit] field passed to the visitor stats API should be [DAY]
             *  `This year` tab is selected: [OrderStatsApiUnit] field passed to the visitor stats API should be [MONTH]
             */
            fun convertToVisitorsStatsApiUnit(granularity: StatsGranularity): OrderStatsApiUnit {
                return when (granularity) {
                    StatsGranularity.DAYS -> DAY
                    StatsGranularity.WEEKS -> DAY
                    StatsGranularity.MONTHS -> DAY
                    StatsGranularity.YEARS -> MONTH
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
    fun fetchStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        date: String,
        quantity: Int,
        force: Boolean = false,
        startDate: String? = null,
        endDate: String? = null
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchStats") {
            val url = WPCOMV2.sites.site(site.siteId).stats.orders.url
            val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString(),
                "_fields" to "data,fields"
            )

            val response = wpComNetwork.executeGetGsonRequest(
                url = url,
                params = params,
                clazz = OrderStatsApiResponse::class.java,
                forced = force
            )

            when (response) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    response.data.let {
                        val model = WCOrderStatsModel().apply {
                            this.localSiteId = site.id
                            this.unit = unit.toString()
                            this.fields = it.fields.toString()
                            this.data = it.data.toString()
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
                    }
                }
                is WPComGsonRequestBuilder.Response.Error -> {
                    val orderError = response.error.toOrderError()
                    val payload = FetchOrderStatsResponsePayload(orderError, site, unit)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedOrderStatsAction(payload))
                }
            }
        }
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
    suspend fun fetchRevenueStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String,
        perPage: Int,
        forceRefresh: Boolean = false
    ): FetchRevenueStatsResponsePayload {
        val url = WOOCOMMERCE.reports.revenue.stats.pathV4Analytics
        val params = mapOf(
            "interval" to OrderStatsApiUnit.convertToRevenueStatsInterval(granularity).toString(),
            "after" to startDate,
            "before" to endDate,
            "per_page" to perPage.toString(),
            "order" to "asc",
            "force_cache_refresh" to forceRefresh.toString()
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = RevenueStatsApiResponse::class.java,
            enableCaching = true,
            forced = forceRefresh
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                        val model = WCRevenueStatsModel().apply {
                            this.localSiteId = site.id
                            this.interval = granularity.toString()
                            this.data = it.intervals.toString()
                            this.total = it.totals.toString()
                            this.startDate = startDate
                            this.endDate = endDate
                        }

                    FetchRevenueStatsResponsePayload(site, granularity, model)
                } ?: FetchRevenueStatsResponsePayload(
                        OrderStatsError(type = OrderStatsErrorType.GENERIC_ERROR,
                                message = "Success response with empty data"
                        ),
                        site,
                        granularity
                )
            }

            is WPAPIResponse.Error -> {
                val orderError = response.error.toOrderError()
                FetchRevenueStatsResponsePayload(orderError, site, granularity)
            }
        }
    }

    /**
     * Makes a GET call to `/wc/v4/reports/revenue/stats`, to check if the site supports the v4 stats api.
     * If v4 stats is not available for the site, returns [OrderStatsErrorType.PLUGIN_NOT_ACTIVE]
     *
     * @param[site] the site to fetch stats data for
     * @param[startDate] the current date to include in ISO format (YYYY-MM-dd'T'HH:mm:ss)
     *
     * Since only the response code is needed to verify if the v4 stats is supported or not,
     * this method has been optimised:
     * The interval param is set to [OrderStatsApiUnit.YEAR] by default
     * The after param is set to the current date by default
     * The _fields param is added to retrieve only the `Totals` field from the api
     */
    fun fetchRevenueStatsAvailability(site: SiteModel, startDate: String) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchRevenueStatsAvailability") {
            val url = WOOCOMMERCE.reports.revenue.stats.pathV4Analytics
            val params = mapOf(
                "interval" to OrderStatsApiUnit.YEAR.toString(),
                "after" to startDate,
                "_fields" to "totals"
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = RevenueStatsApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val payload = FetchRevenueStatsAvailabilityResponsePayload(site, true)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAvailabilityAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = response.error.toOrderError()
                    val payload = FetchRevenueStatsAvailabilityResponsePayload(orderError, site, false)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedRevenueStatsAvailabilityAction(payload))
                }
            }
        }
    }

    fun fetchVisitorStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        date: String,
        quantity: Int,
        force: Boolean = false,
        startDate: String? = null,
        endDate: String? = null
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchVisitorStats") {
            val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1
            val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "quantity" to quantity.toString(),
                "stat_fields" to "visitors")

            val response = wpComNetwork.executeGetGsonRequest(
                url = url,
                params = params,
                clazz = VisitorStatsApiResponse::class.java,
                enableCaching = true,
                forced = force
            )

            when (response) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    val model = WCVisitorStatsModel().apply {
                        this.localSiteId = site.id
                        this.unit = unit.toString()
                        this.fields = response.data.fields.toString()
                        this.data = response.data.data.toString()
                        this.quantity = quantity.toString()
                        this.date = date
                        endDate?.let { this.endDate = it }
                        startDate?.let {
                            this.startDate = startDate
                            this.isCustomField = true
                        }
                    }
                    val payload = FetchVisitorStatsResponsePayload(site, unit, model)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedVisitorStatsAction(payload))
                }
                is WPComGsonRequestBuilder.Response.Error -> {
                    val orderError = response.error.toOrderError()
                    val payload = FetchVisitorStatsResponsePayload(orderError, site, unit)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedVisitorStatsAction(payload))
                }
            }
        }
    }

    suspend fun fetchNewVisitorStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        granularity: StatsGranularity,
        date: String,
        quantity: Int,
        force: Boolean = false,
        startDate: String? = null,
        endDate: String? = null
    ): FetchNewVisitorStatsResponsePayload {
        val url = WPCOMREST.sites.site(site.siteId).stats.visits.urlV1_1
        val params = mapOf(
            "unit" to unit.toString(),
            "date" to date,
            "quantity" to quantity.toString(),
            "stat_fields" to "visitors"
        )

        val response = wpComNetwork.executeGetGsonRequest(
            url = url,
            params = params,
            clazz = VisitorStatsApiResponse::class.java,
            enableCaching = true,
            forced = force
        )

        return when (response) {
            is WPComGsonRequestBuilder.Response.Success -> {
                val statsData = response.data
                val model = WCNewVisitorStatsModel().apply {
                    this.localSiteId = site.id
                    this.granularity = granularity.toString()
                    this.fields = statsData.fields.toString()
                    this.data = statsData.data.toString()
                    this.quantity = quantity.toString()
                    this.date = date
                    endDate?.let { this.endDate = it }
                    startDate?.let {
                        this.startDate = startDate
                        this.isCustomField = true
                    }
                }

                FetchNewVisitorStatsResponsePayload(site, granularity, model)
            }

            is WPComGsonRequestBuilder.Response.Error -> {
                val orderError = response.error.toOrderError()
                FetchNewVisitorStatsResponsePayload(orderError, site, granularity)
            }
        }
    }

    fun fetchTopEarnersStats(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        date: String,
        limit: Int,
        force: Boolean = false
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchTopEarnersStats") {
            val url = WPCOMV2.sites.site(site.siteId).stats.top_earners.url
            val params = mapOf(
                "unit" to unit.toString(),
                "date" to date,
                "limit" to limit.toString()
            )

            val response = wpComNetwork.executeGetGsonRequest(
                url = url,
                params = params,
                clazz = TopEarnersStatsApiResponse::class.java,
                enableCaching = true,
                forced = force
            )

            when (response) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    val wcTopEarners = response.data.data?.map {
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
                }
                is WPComGsonRequestBuilder.Response.Error -> {
                    val orderError = response.error.toOrderError()
                    val payload = FetchTopEarnersStatsResponsePayload(orderError, site, unit)
                    mDispatcher.dispatch(WCStatsActionBuilder.newFetchedTopEarnersStatsAction(payload))
                }
            }
        }
    }

    private fun WPAPINetworkError.toOrderError() = networkErrorToOrderError(errorCode, message)
    private fun WPComGsonNetworkError.toOrderError() = networkErrorToOrderError(apiError, message)
    
    private fun networkErrorToOrderError(errorCode: String?, message: String?): OrderStatsError {
        val orderStatsErrorType = when (errorCode) {
            "rest_invalid_param" -> OrderStatsErrorType.INVALID_PARAM
            "rest_no_route" -> OrderStatsErrorType.PLUGIN_NOT_ACTIVE
            else -> OrderStatsErrorType.fromString(errorCode.orEmpty())
        }
        return OrderStatsError(orderStatsErrorType, message.orEmpty())
    }
}
