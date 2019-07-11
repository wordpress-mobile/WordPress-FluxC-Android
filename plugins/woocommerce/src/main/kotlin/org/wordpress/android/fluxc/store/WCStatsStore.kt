package org.wordpress.android.fluxc.store

import android.content.Context
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel.OrderStatsField
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCRevenueStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WCStatsStore @Inject constructor(
    dispatcher: Dispatcher,
    private val context: Context,
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

        const val STATS_API_V4_PER_PAGE_PARAM = "STATS_V4_API_PER_PAGE_PARAM_PREF_KEY"
        const val STATS_API_V4_MIN_PER_PAGE_PARAM = 31
        const val STATS_API_V4_MAX_PER_PAGE_PARAM = 100
    }

    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    enum class StatsGranularity {
        DAYS, WEEKS, MONTHS, YEARS;

        companion object {
            fun fromOrderStatsApiUnit(apiUnit: OrderStatsApiUnit): StatsGranularity {
                return when (apiUnit) {
                    OrderStatsApiUnit.HOUR -> StatsGranularity.DAYS
                    OrderStatsApiUnit.DAY -> StatsGranularity.DAYS
                    OrderStatsApiUnit.WEEK -> StatsGranularity.WEEKS
                    OrderStatsApiUnit.MONTH -> StatsGranularity.MONTHS
                    OrderStatsApiUnit.YEAR -> StatsGranularity.YEARS
                }
            }

            fun fromString(value: String): StatsGranularity {
                return fromOrderStatsApiUnit(OrderStatsApiUnit.valueOf(value.toUpperCase()))
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
        val startDate: String? = null,
        val endDate: String? = null,
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

    /**
     * Describes the parameters for fetching new stats for [site], up to the current day, month, or year
     * (depending on the given [apiInterval], [startDate]).
     *
     * @param[apiInterval] the time interval for the requested data
     * @param[startDate] The start date of the data
     * @param[forced] if true, ignores any cached result and forces a refresh from the server (defaults to false)
     */
    class FetchRevenueStatsPayload(
        val site: SiteModel,
        val apiInterval: OrderStatsApiUnit,
        val startDate: String,
        val forced: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchRevenueStatsResponsePayload(
        val site: SiteModel,
        val apiInterval: OrderStatsApiUnit,
        val stats: WCRevenueStatsModel? = null
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, apiInterval: OrderStatsApiUnit) : this(site, apiInterval) {
            this.error = error
        }
    }

    /**
     * Describes the parameters for fetching visitor stats for [site], up to the current day, month, or year
     * (depending on the given [granularity]).
     *
     * @param[granularity] the time units for the requested data
     * @param[forced] if true, ignores any cached result and forces a refresh from the server
     */
    class FetchVisitorStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val forced: Boolean = false,
        val startDate: String? = null,
        val endDate: String? = null
    ) : Payload<BaseNetworkError>()

    class FetchVisitorStatsResponsePayload(
        val site: SiteModel,
        val apiUnit: OrderStatsApiUnit,
        val visits: Int = 0
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, apiUnit: OrderStatsApiUnit) : this(site, apiUnit) {
            this.error = error
        }
    }

    class FetchTopEarnersStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val limit: Int = 10,
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
        RESPONSE_NULL,
        INVALID_PARAM,
        PLUGIN_NOT_ACTIVE,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = OrderStatsErrorType.values().associateBy(OrderStatsErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    // OnChanged events
    class OnWCStatsChanged(
        val rowsAffected: Int,
        val granularity: StatsGranularity,
        val quantity: String? = null,
        val date: String? = null,
        val isCustomField: Boolean = false
    ) : OnChanged<OrderStatsError>() {
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
            WCStatsAction.FETCH_REVENUE_STATS -> fetchRevenueStats(action.payload as FetchRevenueStatsPayload)
            WCStatsAction.FETCH_VISITOR_STATS -> fetchVisitorStats(action.payload as FetchVisitorStatsPayload)
            WCStatsAction.FETCH_TOP_EARNERS_STATS -> fetchTopEarnersStats(action.payload as FetchTopEarnersStatsPayload)
            WCStatsAction.FETCHED_ORDER_STATS ->
                handleFetchOrderStatsCompleted(action.payload as FetchOrderStatsResponsePayload)
            WCStatsAction.FETCHED_REVENUE_STATS ->
                handleFetchRevenueStatsCompleted(action.payload as FetchRevenueStatsResponsePayload)
            WCStatsAction.FETCHED_VISITOR_STATS ->
                handleFetchVisitorStatsCompleted(action.payload as FetchVisitorStatsResponsePayload)
            WCStatsAction.FETCHED_TOP_EARNERS_STATS ->
                handleFetchTopEarnersStatsCompleted(action.payload as FetchTopEarnersStatsResponsePayload)
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
    fun getRevenueStats(
        site: SiteModel,
        granularity: StatsGranularity,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): Map<String, Double> {
        return getStatsForField(site, OrderStatsField.GROSS_SALES, granularity, quantity, date, isCustomField)
    }

    /**
     * Returns the order volume data by date for the given [site], in units of [granularity].
     *
     * The returned map has the format: "2018-05-01" -> 15
     *
     * See [getRevenueStats] for detail on the date formatting of the map keys.
     */
    fun getOrderStats(
        site: SiteModel,
        granularity: StatsGranularity,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): Map<String, Int> {
        return getStatsForField(site, OrderStatsField.ORDERS, granularity, quantity, date, isCustomField)
    }

    fun getCustomStatsForSite(
        site: SiteModel
    ): WCOrderStatsModel? {
        return WCStatsSqlUtils.getCustomStatsForSite(site)
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
                    reportMissingFieldError(statsModel, OrderStatsField.CURRENCY)
                    return null
                }
                return it[currencyIndex] as String
            }
        } ?: return null
    }

    /**
     * returns the quantity (how far back to go) to use when requesting stats for a specific granularity
     * and the date range
     */
    private fun getQuantityForGranularity(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String?,
        endDate: String?
    ): Int {
        val defaultValue = when (granularity) {
            StatsGranularity.DAYS -> STATS_QUANTITY_DAYS
            StatsGranularity.WEEKS -> STATS_QUANTITY_WEEKS
            StatsGranularity.MONTHS -> STATS_QUANTITY_MONTHS
            StatsGranularity.YEARS -> {
                // Years since 2011 (WooCommerce initial release), inclusive
                SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_YEAR).toInt() - 2011 + 1
            }
        }
        return getQuantityByGranularity(startDate, endDate, granularity, defaultValue).toInt()
    }

    /**
     * Given a {@param d1} start date, {@param d2} end date and the {@param granularity} granularity,
     * returns a quantity value.
     * If the start date or end date is empty, returns {@param defaultValue}
     */
    fun getQuantityByGranularity(
        startDateString: String?,
        endDateString: String?,
        granularity: StatsGranularity,
        defaultValue: Int
    ): Long {
        if (startDateString.isNullOrEmpty() || endDateString.isNullOrEmpty()) return defaultValue.toLong()

        val startDate = DateUtils.getDateFromString(startDateString)
        val endDate = DateUtils.getDateFromString(endDateString)

        val startDateCalendar = DateUtils.getStartDateCalendar(if (startDate.before(endDate)) startDate else endDate)
        val endDateCalendar = DateUtils.getEndDateCalendar(if (startDate.before(endDate)) endDate else startDate)

        return when (granularity) {
            StatsGranularity.WEEKS -> DateUtils.getQuantityInWeeks(startDateCalendar, endDateCalendar)
            StatsGranularity.MONTHS -> DateUtils.getQuantityInMonths(startDateCalendar, endDateCalendar)
            StatsGranularity.YEARS -> DateUtils.getQuantityInYears(startDateCalendar, endDateCalendar)
            else -> DateUtils.getQuantityInDays(startDateCalendar, endDateCalendar)
        }
    }

    private fun fetchOrderStats(payload: FetchOrderStatsPayload) {
        val quantity = getQuantityForGranularity(payload.site, payload.granularity, payload.startDate, payload.endDate)
        wcOrderStatsClient.fetchStats(
                payload.site,
                OrderStatsApiUnit.fromStatsGranularity(payload.granularity),
                getFormattedDate(payload.site, payload.granularity, payload.endDate),
                quantity,
                payload.forced,
                payload.startDate,
                payload.endDate)
    }

    private fun fetchVisitorStats(payload: FetchVisitorStatsPayload) {
        val quantity = getQuantityForGranularity(payload.site,
                payload.granularity, payload.startDate, payload.endDate)
        wcOrderStatsClient.fetchVisitorStats(
                payload.site,
                OrderStatsApiUnit.fromStatsGranularity(payload.granularity),
                getFormattedDate(payload.site, payload.granularity, payload.endDate),
                quantity,
                payload.forced
        )
    }

    private fun fetchTopEarnersStats(payload: FetchTopEarnersStatsPayload) {
        wcOrderStatsClient.fetchTopEarnersStats(
                payload.site,
                OrderStatsApiUnit.fromStatsGranularity(payload.granularity),
                getFormattedDate(payload.site, payload.granularity),
                payload.limit,
                payload.forced
        )
    }

    private fun handleFetchOrderStatsCompleted(payload: FetchOrderStatsResponsePayload) {
        val onStatsChanged = with(payload) {
            val granularity = StatsGranularity.fromOrderStatsApiUnit(apiUnit)
            if (isError || stats == null) {
                return@with OnWCStatsChanged(0, granularity).also { it.error = payload.error }
            } else {
                val rowsAffected = WCStatsSqlUtils.insertOrUpdateStats(stats)
                return@with OnWCStatsChanged(rowsAffected, granularity, stats.quantity, stats.date, stats.isCustomField)
            }
        }

        onStatsChanged.causeOfChange = WCStatsAction.FETCH_ORDER_STATS
        emitChange(onStatsChanged)
    }

    private fun handleFetchVisitorStatsCompleted(payload: FetchVisitorStatsResponsePayload) {
        val granularity = StatsGranularity.fromOrderStatsApiUnit(payload.apiUnit)
        val onStatsChanged = OnWCStatsChanged(payload.visits, granularity)
        if (payload.isError) {
            onStatsChanged.error = payload.error
        }
        onStatsChanged.causeOfChange = WCStatsAction.FETCH_VISITOR_STATS
        emitChange(onStatsChanged)
    }

    private fun handleFetchTopEarnersStatsCompleted(payload: FetchTopEarnersStatsResponsePayload) {
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

    /**
     * Given a {@param endDate} end date, formats the end date based on the site's timezone
     * If the start date or end date is empty, formats the current date
     */
    private fun getFormattedDate(site: SiteModel, granularity: StatsGranularity, endDate: String?): String {
        return when (granularity) {
            StatsGranularity.DAYS -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, endDate)
            StatsGranularity.WEEKS -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_WEEK, endDate)
            StatsGranularity.MONTHS -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_MONTH, endDate)
            StatsGranularity.YEARS -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_YEAR, endDate)
        }
    }

    // The type of the stats field relies on knowledge of the real value of the stored JSON for that field
    // It's up to the function caller to be aware of the compatible types for any given field
    @Suppress("UNCHECKED_CAST")
    private fun <T> getStatsForField(
        site: SiteModel,
        field: OrderStatsField,
        granularity: StatsGranularity,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): Map<String, T> {
        val apiUnit = OrderStatsApiUnit.fromStatsGranularity(granularity)
        val rawStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(
                site, apiUnit, quantity, date, isCustomField)
        rawStats?.let {
            val periodIndex = it.getIndexForField(OrderStatsField.PERIOD)
            val fieldIndex = it.getIndexForField(field)
            if (periodIndex == -1 || fieldIndex == -1) {
                // One of the fields we need wasn't returned by the server
                reportMissingFieldError(it, field)
                return mapOf()
            }

            // Years are returned as numbers by the API, and Gson interprets them as floats - clean up the decimal
            return it.dataList.map { it[periodIndex].toString().removeSuffix(".0") to it[fieldIndex] as T }.toMap()
        } ?: return mapOf()
    }

    private fun reportMissingFieldError(orderStatsModel: WCOrderStatsModel, missingField: OrderStatsField) {
        AppLog.e(T.API, "Missing field from stats endpoint - missing field: $missingField, " +
                "returned fields: ${orderStatsModel.fields}")
        val unexpectedError = OnUnexpectedError(
                IllegalStateException("Missing field from stats endpoint"),
                "Missing field: $missingField, returned fields: ${orderStatsModel.fields}"
        )
        mDispatcher.emitChange(unexpectedError)
    }

    /**
     * Methods to support v4 api changes
     */
    class OnWCRevenueStatsChanged(
        val rowsAffected: Int,
        val apiInterval: OrderStatsApiUnit,
        val startDate: String? = null,
        val endDate: String? = null
    ) : OnChanged<OrderStatsError>() {
        var causeOfChange: WCStatsAction? = null
    }

    private fun fetchRevenueStats(payload: FetchRevenueStatsPayload) {
        val startDate = DateUtils.getStartDateForSite(payload.site, payload.startDate)
        val endDate = DateUtils.getEndDateForSite(payload.site)
        val perPage = getRandomPageInt(payload.forced)
        wcOrderStatsClient.fetchRevenueStats(
                payload.site,
                payload.apiInterval,
                startDate,
                endDate,
                perPage,
                payload.forced
        )
    }

    /**
     * The default data count in `v4 revenue api` is 10.
     * so if we need to get data for an entire month without pagination, the per_page value should be 30 or 31.
     * But, due to caching in the api, if the per_page value static, the api is not providing refreshed data
     * when a new order is completed.
     * So this logic generates a random value between 31 to 100 only if the [forced] is set to true
     * And storing this value locally to be used when the [forced] flag is set to false.
     * */
    private fun getRandomPageInt(forced: Boolean): Int {
        val randomInt = Random.nextInt(STATS_API_V4_MIN_PER_PAGE_PARAM, STATS_API_V4_MAX_PER_PAGE_PARAM)
        return if (!forced) {
            val prefsValue = preferences.getInt(STATS_API_V4_PER_PAGE_PARAM, 0)
            if (prefsValue == 0) {
                preferences.edit().putInt(STATS_API_V4_PER_PAGE_PARAM, randomInt).apply()
                randomInt
            } else {
                prefsValue
            }
        } else {
            preferences.edit().putInt(STATS_API_V4_PER_PAGE_PARAM, randomInt).apply()
            randomInt
        }
    }

    private fun handleFetchRevenueStatsCompleted(payload: FetchRevenueStatsResponsePayload) {
        val onStatsChanged = with(payload) {
            if (isError || stats == null) {
                return@with OnWCRevenueStatsChanged(0, apiInterval)
                        .also { it.error = payload.error }
            } else {
                val rowsAffected = WCRevenueStatsSqlUtils.insertOrUpdateStats(stats)
                return@with OnWCRevenueStatsChanged(rowsAffected, apiInterval, stats.startDate, stats.endDate)
            }
        }

        onStatsChanged.causeOfChange = WCStatsAction.FETCH_REVENUE_STATS
        emitChange(onStatsChanged)
    }

    /**
     * Returns the revenue data by date for the given [site], in units of [interval].
     *
     * The returned map has the format: "2018-05-01" -> 57.43
     *
     * The [startDate] and [endDate] will be passed by the function caller
     *
     * The format of the date key in the returned map depends on the [interval]:
     * [OrderStatsApiUnit.HOUR]: "2018-07-06 23"
     * [OrderStatsApiUnit.DAY]: "2018-07-06"
     * [OrderStatsApiUnit.WEEK]: "2018-W16"
     * [OrderStatsApiUnit.MONTH]: "2018-05"
     * [OrderStatsApiUnit.YEAR]: "2018"
     */
    fun getWCRevenueStats(
        site: SiteModel,
        interval: OrderStatsApiUnit,
        startDate: String,
        endDate: String
    ): Map<String, Double> {
        val rawStats = getRawStats(site, interval, startDate, endDate)
        return rawStats?.getIntervalList()?.map {
            it.interval!! to it.subtotals?.gross_revenue!!
        }?.toMap() ?: mapOf()
    }

    fun getWCOrderCountStats(
        site: SiteModel,
        interval: OrderStatsApiUnit,
        startDate: String,
        endDate: String
    ): Map<String, Long> {
        val rawStats = getRawStats(site, interval, startDate, endDate)
        return rawStats?.getIntervalList()?.map {
            it.interval!! to it.subtotals?.orders_count!!
        }?.toMap() ?: mapOf()
    }

    private fun getRawStats(
        site: SiteModel,
        interval: OrderStatsApiUnit,
        startDate: String,
        endDate: String
    ): WCRevenueStatsModel? {
        return WCRevenueStatsSqlUtils.getRawStatsForSiteIntervalAndDate(
                site, interval, startDate, endDate)
    }
}
