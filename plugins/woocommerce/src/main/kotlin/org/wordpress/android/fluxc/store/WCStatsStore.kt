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
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
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
        private const val DATE_FORMAT_DAY = "yyyy-MM-dd"
        private const val DATE_FORMAT_MONTH = "yyyy-MM"
        private const val DATE_FORMAT_YEAR = "yyyy"
        private const val DATE_FORMAT_DAY_OF_MONTH = "dd"
    }

    enum class StatsGranularity {
        DAYS, MONTHS, YEARS;

        companion object {
            fun fromOrderStatsApiUnit(apiUnit: OrderStatsApiUnit): StatsGranularity {
                return when (apiUnit) {
                    OrderStatsApiUnit.DAY -> StatsGranularity.DAYS
                    OrderStatsApiUnit.MONTH -> StatsGranularity.MONTHS
                    OrderStatsApiUnit.YEAR -> StatsGranularity.YEARS
                    OrderStatsApiUnit.WEEK -> throw AssertionError("Not implemented!")
                }
            }
        }
    }

    /**
     * Describes the parameters for fetching order stats for [site], up to the current day, month, or year
     * (depending on the given [granularity]).
     *
     * Using [StatsGranularity.DAYS] will fetch data to cover both 'week so far' and 'month so far'.
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

    override fun onRegister() {
        AppLog.d(T.API, "WCStatsStore onRegister")
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCStatsAction ?: return
        when (actionType) {
            WCStatsAction.FETCH_ORDER_STATS -> fetchOrderStats(action.payload as FetchOrderStatsPayload)
            WCStatsAction.FETCHED_ORDER_STATS ->
                handleFetchOrderStatsCompleted(action.payload as FetchOrderStatsResponsePayload)
        }
    }

    /**
     * Returns the revenue data for the month so far for the given [site], in increments of days.
     *
     * The month so far is relative to the site's own timezone, not the current device's.
     *
     * The returned map has the format:
     * {
     * "2018-05-01" -> 57.43,
     * "2018-05-02" -> 78.98,
     * ...
     * "2018-05-16" -> 68.24
     * }
     */
    fun getRevenueStatsForCurrentMonth(site: SiteModel): Map<String, Double> {
        return getCurrentMonthStatsForField(site, OrderStatsField.TOTAL_SALES)
    }

    /**
     * Returns the order volume data for the month so far for the given [site], in increments of days.
     *
     * The month so far is relative to the site's own timezone, not the current device's.
     *
     * The returned map has the format:
     * {
     * "2018-05-01" -> 15,
     * "2018-05-02" -> 7,
     * ...
     * "2018-05-16" -> 24
     * }
     */
    fun getOrderStatsForCurrentMonth(site: SiteModel): Map<String, Int> {
        return getCurrentMonthStatsForField(site, OrderStatsField.ORDERS)
    }

    private fun fetchOrderStats(payload: FetchOrderStatsPayload) {
        // TODO: Caching, and skip cache if forced == true
        when (payload.granularity) {
            StatsGranularity.DAYS -> {
                // TODO: Calculate quantity from max(day-of-the-month, day-of-the-week) - for now, 31 covers all cases
                wcOrderStatsClient.fetchStats(payload.site, OrderStatsApiUnit.DAY,
                        getFormattedDate(payload.site, StatsGranularity.DAYS), 31)
            }
            StatsGranularity.MONTHS -> TODO()
            StatsGranularity.YEARS -> TODO()
        }
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

    private fun getFormattedDate(site: SiteModel, granularity: StatsGranularity): String {
        return when (granularity) {
            StatsGranularity.DAYS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_DAY)
            StatsGranularity.MONTHS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_MONTH)
            StatsGranularity.YEARS -> SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_YEAR)
        }
    }

    // The type of the stats field relies on knowledge of the real value of the stored JSON for that field
    // It's up to the function caller to be aware of the compatible types for any given field
    @Suppress("UNCHECKED_CAST")
    private fun <T> getCurrentMonthStatsForField(site: SiteModel, field: OrderStatsField): Map<String, T> {
        val rawStats = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, OrderStatsApiUnit.DAY)
        rawStats?.let {
            val periodIndex = it.getIndexForField(OrderStatsField.PERIOD)
            val fieldIndex = it.getIndexForField(field)
            val dayOfMonth = SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_DAY_OF_MONTH).toInt()
            return it.dataList
                    .takeLast(dayOfMonth)
                    .map { it[periodIndex].toString() to it[fieldIndex] as T }.toMap()
        } ?: return mapOf()
    }
}
