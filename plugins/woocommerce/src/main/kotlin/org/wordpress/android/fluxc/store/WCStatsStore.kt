package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel.OrderStatsField
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCStatsStore @Inject constructor(
    dispatcher: Dispatcher,
    private val wcOrderStatsClient: OrderStatsRestClient
) : Store(dispatcher) {
    companion object {
        const val STATS_QUANTITY_DAYS = 30
        const val STATS_QUANTITY_WEEKS = 17
        const val STATS_QUANTITY_MONTHS = 12

        private const val DATE_FORMAT_DAY = "yyyy-MM-dd"
        private const val DATE_FORMAT_WEEK = "yyyy-'W'ww"
        private const val DATE_FORMAT_MONTH = "yyyy-MM"
        private const val DATE_FORMAT_YEAR = "yyyy"
    }

    enum class StatsGranularity {
        DAYS, WEEKS, MONTHS, YEARS;

        companion object {
            fun fromOrderStatsApiUnit(apiUnit: OrderStatsApiUnit): StatsGranularity {
                return when (apiUnit) {
                    OrderStatsApiUnit.DAY -> StatsGranularity.DAYS
                    OrderStatsApiUnit.WEEK -> StatsGranularity.WEEKS
                    OrderStatsApiUnit.MONTH -> StatsGranularity.MONTHS
                    OrderStatsApiUnit.YEAR -> StatsGranularity.YEARS
                }
            }
        }
    }

    /**
     * Describes the parameters for fetching order stats for [site], up to the current day, month, or year
     * (depending on the given [granularity]).
     *
     * The amount of data fetched depends on the granularity:
     * [StatsGranularity.DAYS]: the last 30 days, in increments of a day
     * [StatsGranularity.WEEKS]: the last 17 weeks (about a quarter), in increments of a week
     * [StatsGranularity.MONTHS]: the last 12 months, in increments of a month
     * [StatsGranularity.YEARS]: all data since 2011, in increments on a year
     *
     * @param[granularity] the time units for the requested data
     * @param[forced] if true, ignores any cached result and forces a refresh from the server (defaults to false)
     */
    class FetchOrderStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val forced: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchOrderStatsResponsePayload(
        val site: SiteModel,
        val apiUnit: OrderStatsApiUnit,
        val stats: WCOrderStatsModel? = null
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, apiUnit: OrderStatsApiUnit) : this(site, apiUnit) {
            this.error = error
        }
    }

    class FetchTopEarnersStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val quantity: Int,
        val forced: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchTopEarnersStatsResponsePayload(
        val site: SiteModel,
        val apiUnit: OrderStatsApiUnit,
        val topEarners: List<WCTopEarnerModel> = emptyList()
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, apiUnit: OrderStatsApiUnit) : this(site, apiUnit) {
            this.error = error
        }
    }

    class OrderStatsError(val type: OrderStatsErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class OrderStatsErrorType {
        INVALID_PARAM,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = OrderStatsErrorType.values().associateBy(OrderStatsErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    // OnChanged events
    class OnWCStatsChanged(val rowsAffected: Int, val granularity: StatsGranularity) : OnChanged<OrderStatsError>() {
        var causeOfChange: WCStatsAction? = null
    }

    class OnWCTopEarnersChanged(val topEarners: List<WCTopEarnerModel>, val granularity: StatsGranularity) :
            OnChanged<OrderStatsError>() {
        var causeOfChange: WCStatsAction? = null
    }

    override fun onRegister() = AppLog.d(T.API, "WCStatsStore onRegister")

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCStatsAction ?: return
        when (actionType) {
            WCStatsAction.FETCH_ORDER_STATS -> fetchOrderStats(action.payload as FetchOrderStatsPayload)
            WCStatsAction.FETCH_TOP_EARNERS_STATS -> fetchTopEarnersStats(action.payload as FetchTopEarnersStatsPayload)
            WCStatsAction.FETCHED_ORDER_STATS ->
                handleFetchOrderStatsCompleted(action.payload as FetchOrderStatsResponsePayload)
            WCStatsAction.FETCHED_TOP_EARNERS_STATS ->
                handleFetchTopEarrnersStatsCompleted(action.payload as FetchTopEarnersStatsResponsePayload)
        }
    }

    /**
     * Returns the revenue data by date for the given [site], in units of [granularity].
     *
     * The returned map has the format: "2018-05-01" -> 57.43
     *
     * The amount of data returned depends on the granularity:
     *
     * [StatsGranularity.DAYS]: the last 30 days, in increments of a day
     * [StatsGranularity.WEEKS]: the last 17 weeks (about a quarter), in increments of a week
     * [StatsGranularity.MONTHS]: the last 12 months, in increments of a month
     * [StatsGranularity.YEARS]: all data since 2011, in increments on a year
     *
     * The start date is the current day/week/month/year, relative to the site's own timezone (not the current device's).
     *
     * The format of the date key in the returned map depends on the [granularity]:
     * [StatsGranularity.DAYS]: "2018-05-01"
     * [StatsGranularity.WEEKS]: "2018-W16"
     * [StatsGranularity.MONTHS]: "2018-05"
     * [StatsGranularity.YEARS]: "2018"
     */
    fun getRevenueStats(site: SiteModel, granularity: StatsGranularity): Map<String, Double> {
        return getStatsForField(site, OrderStatsField.TOTAL_SALES, granularity)
    }

    /**
     * Returns the order volume data by date for the given [site], in units of [granularity].
     *
     * The returned map has the format: "2018-05-01" -> 15
     *
     * See [getRevenueStats] for detail on the date formatting of the map keys.
     */
    fun getOrderStats(site: SiteModel, granularity: StatsGranularity): Map<String, Int> {
        return getStatsForField(site, OrderStatsField.ORDERS, granularity)
    }

    /**
     * Returns the currency code associated with stored stats for the [site], as an ISO 4217 currency code (eg. USD).
     */
    fun getStatsCurrencyForSite(site: SiteModel): String? {
        val rawStats = WCStatsSqlUtils.getFirstRawStatsForSite(site)
        rawStats?.let { statsModel ->
            statsModel.dataList.firstOrNull()?.let {
                val currencyIndex = statsModel.getIndexForField(OrderStatsField.CURRENCY)
                if (currencyIndex == -1) {
                    // The server didn't return the currency field
                    reportMissingFieldError(statsModel)
                    return null
                }
                return it[currencyIndex] as String
            }
        } ?: return null
    }

    private fun fetchOrderStats(payload: FetchOrderStatsPayload) {
        val quantity = when (payload.granularity) {
            StatsGranularity.DAYS -> STATS_QUANTITY_DAYS
            StatsGranularity.WEEKS -> STATS_QUANTITY_WEEKS
            StatsGranularity.MONTHS -> STATS_QUANTITY_MONTHS
            StatsGranularity.YEARS -> {
                // Years since 2011 (WooCommerce initial release), inclusive
                SiteUtils.getCurrentDateTimeForSite(payload.site, DATE_FORMAT_YEAR).toInt() - 2011 + 1
            }
        }

        wcOrderStatsClient.fetchStats(payload.site, OrderStatsApiUnit.fromStatsGranularity(payload.granularity),
                getFormattedDate(payload.site, payload.granularity), quantity, payload.forced)
    }

    private fun fetchTopEarnersStats(payload: FetchTopEarnersStatsPayload) {
        wcOrderStatsClient.fetchTopEarnersStats(
                payload.site,
                OrderStatsApiUnit.fromStatsGranularity(payload.granularity),
                payload.quantity,
                payload.forced)
    }

    private fun handleFetchOrderStatsCompleted(payload: FetchOrderStatsResponsePayload) {
        val onStatsChanged = with (payload) {
            val granularity = StatsGranularity.fromOrderStatsApiUnit(apiUnit)
            if (isError || stats == null) {
                return@with OnWCStatsChanged(0, granularity).also { it.error = payload.error }
            } else {
                val rowsAffected = WCStatsSqlUtils.insertOrUpdateStats(stats)
                return@with OnWCStatsChanged(rowsAffected, granularity)
            }
        }

        onStatsChanged.causeOfChange = WCStatsAction.FETCH_ORDER_STATS
        emitChange(onStatsChanged)
    }

    private fun handleFetchTopEarrnersStatsCompleted(payload: FetchTopEarnersStatsResponsePayload) {
        val granularity = StatsGranularity.fromOrderStatsApiUnit(payload.apiUnit)
        val onTopEarnersChanged = OnWCTopEarnersChanged(payload.topEarners, granularity)
        if (payload.isError) {
            onTopEarnersChanged.error = payload.error
        }
        onTopEarnersChanged.causeOfChange = WCStatsAction.FETCH_TOP_EARNERS_STATS
        emitChange(onTopEarnersChanged)
    }

    private fun getFormattedDate(site: SiteModel, granularity: StatsGranularity): String {
        return when (granularity) {
            StatsGranularity.DAYS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_DAY)
            StatsGranularity.WEEKS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_WEEK)
            StatsGranularity.MONTHS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_MONTH)
            StatsGranularity.YEARS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_YEAR)
        }
    }

    // The type of the stats field relies on knowledge of the real value of the stored JSON for that field
    // It's up to the function caller to be aware of the compatible types for any given field
    @Suppress("UNCHECKED_CAST")
    private fun <T> getStatsForField(
        site: SiteModel,
        field: OrderStatsField,
        granularity: StatsGranularity
    ): Map<String, T> {
        val apiUnit = OrderStatsApiUnit.fromStatsGranularity(granularity)
        val rawStats = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, apiUnit)
        rawStats?.let {
            val periodIndex = it.getIndexForField(OrderStatsField.PERIOD)
            val fieldIndex = it.getIndexForField(field)
            if (periodIndex == -1 || fieldIndex == -1) {
                // One of the fields we need wasn't returned by the server
                reportMissingFieldError(it)
                return mapOf()
            }

            // Years are returned as numbers by the API, and Gson interprets them as floats - clean up the decimal
            return it.dataList.map { it[periodIndex].toString().removeSuffix(".0") to it[fieldIndex] as T }.toMap()
        } ?: return mapOf()
    }

    private fun reportMissingFieldError(orderStatsModel: WCOrderStatsModel) {
        AppLog.e(T.API, "Missing field from stats endpoint - returned fields: " + orderStatsModel.fields)
        val unexpectedError = OnUnexpectedError(
                IllegalStateException("Missing field from stats endpoint"),
                orderStatsModel.fields)
        mDispatcher.emitChange(unexpectedError)
    }
}
