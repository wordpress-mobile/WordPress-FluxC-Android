package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel.OrderStatsField
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.model.WCVisitorStatsModel
import org.wordpress.android.fluxc.model.WCVisitorStatsModel.VisitorStatsField
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WCVisitorStatsSqlUtils
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCStatsStore @Inject constructor(
    dispatcher: Dispatcher,
    private val wcOrderStatsClient: OrderStatsRestClient,
    private val coroutineEngine: CoroutineEngine
) : Store(dispatcher) {
    companion object {
        const val WOO_COMMERCE_INITIAL_RELEASE = 2011

        const val STATS_QUANTITY_DAYS = 30
        const val STATS_QUANTITY_WEEKS = 17
        const val STATS_QUANTITY_MONTHS = 12

        const val STATS_GRANULARITY_DAYS = 1
        const val STATS_GRANULARITY_YEARS = 12

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

        fun startDateTime(site: SiteModel) = when (this) {
            DAYS -> DateUtils.getStartDateForSite(site, DateUtils.getStartOfCurrentDay())
            WEEKS -> DateUtils.getFirstDayOfCurrentWeekBySite(site)
            MONTHS -> DateUtils.getFirstDayOfCurrentMonthBySite(site)
            YEARS -> DateUtils.getFirstDayOfCurrentYearBySite(site)
        }

        fun endDateTime(site: SiteModel) = when (this) {
            DAYS -> DateUtils.getEndDateForSite(site)
            WEEKS -> DateUtils.getLastDayOfCurrentWeekForSite(site)
            MONTHS -> DateUtils.getLastDayOfCurrentMonthForSite(site)
            YEARS -> DateUtils.getLastDayOfCurrentYearForSite(site)
        }

        fun datePeriod(site: SiteModel): String {
            val startDate = startDateTime(site)
            val endDate = endDateTime(site)
            return DateUtils.getDatePeriod(startDate, endDate)
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
     * (depending on the given [granularity], [startDate]).
     *
     * @param[granularity] the time interval for the requested data (days, weeks, months, years)
     * @param[startDate] The start date of the data
     * @param[endDate] The end date of the data
     * @param[forced] if true, ignores any cached result and forces a refresh from the server (defaults to false)
     */
    class FetchRevenueStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val startDate: String? = null,
        val endDate: String? = null,
        val forced: Boolean = false,
        val revenueRangeId: Int = 0
    ) : Payload<BaseNetworkError>()

    class FetchRevenueStatsResponsePayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val stats: WCRevenueStatsModel? = null
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, granularity: StatsGranularity) : this(site, granularity) {
            this.error = error
        }
    }

    /**
     * Describes the parameters for fetching checking if the v4 stats for [site] is supported
     */
    class FetchRevenueStatsAvailabilityPayload(
        val site: SiteModel
    ) : Payload<BaseNetworkError>()

    class FetchRevenueStatsAvailabilityResponsePayload(
        val site: SiteModel,
        val available: Boolean = false
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, available: Boolean) : this(site, available) {
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
        val stats: WCVisitorStatsModel? = null
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, apiUnit: OrderStatsApiUnit) : this(site, apiUnit) {
            this.error = error
        }
    }

    /**
     * Describes the parameters for fetching visitor stats for [site], up to the current day, month, or year
     * (depending on the given [granularity]). These classes are used exclusively for the new v4 stats API changes
     *
     * @param[granularity] the time units for the requested data
     * @param[forced] if true, ignores any cached result and forces a refresh from the server
     */
    class FetchNewVisitorStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val forced: Boolean = false,
        val startDate: String? = null,
        val endDate: String? = null
    ) : Payload<BaseNetworkError>()

    class FetchNewVisitorStatsResponsePayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val stats: WCNewVisitorStatsModel? = null
    ) : Payload<OrderStatsError>() {
        constructor(error: OrderStatsError, site: SiteModel, granularity: StatsGranularity) : this(site, granularity) {
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
            WCStatsAction.FETCH_REVENUE_STATS_AVAILABILITY ->
                fetchRevenueStatsAvailability(action.payload as FetchRevenueStatsAvailabilityPayload)
            WCStatsAction.FETCH_VISITOR_STATS -> fetchVisitorStats(action.payload as FetchVisitorStatsPayload)
            WCStatsAction.FETCH_TOP_EARNERS_STATS -> fetchTopEarnersStats(action.payload as FetchTopEarnersStatsPayload)
            WCStatsAction.FETCHED_ORDER_STATS ->
                handleFetchOrderStatsCompleted(action.payload as FetchOrderStatsResponsePayload)
            WCStatsAction.FETCHED_REVENUE_STATS_AVAILABILITY -> handleFetchRevenueStatsAvailabilityCompleted(
                    action.payload as FetchRevenueStatsAvailabilityResponsePayload
            )
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
     * The start date is the current day/week/month/year, relative to the site's own timezone
     * (not the current device's).
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
     * Returns the visitor data by date for the given [site], in units of [granularity].
     * The returned map has the format: "2018-05-01" -> 15
     */
    fun getVisitorStats(
        site: SiteModel,
        granularity: StatsGranularity,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): Map<String, Int> {
        val apiUnit = OrderStatsApiUnit.fromStatsGranularity(granularity)
        val rawStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
            site, apiUnit, quantity, date, isCustomField
        )
        rawStats?.let { visitorStatsModel ->
            val periodIndex = visitorStatsModel.getIndexForField(VisitorStatsField.PERIOD)
            val fieldIndex = visitorStatsModel.getIndexForField(VisitorStatsField.VISITORS)
            return if (periodIndex == -1 || fieldIndex == -1) {
                mapOf()
            } else {
                // Years are returned as numbers by the API, and Gson interprets them as floats - clean up the decimal
                visitorStatsModel.dataList.map {
                    it[periodIndex].toString().removeSuffix(".0") to (it[fieldIndex] as Number).toInt()
                }.toMap()
            }
        } ?: return mapOf()
    }

    /**
     * Returns the visitor data by date for the given [site] and [granularity].
     * The returned map has the format: "2018-05-01" -> 15
     */
    fun getNewVisitorStats(
        site: SiteModel,
        granularity: StatsGranularity,
        quantity: String? = null,
        date: String? = null,
        isCustomField: Boolean = false
    ): Map<String, Int> {
        val rawStats = WCVisitorStatsSqlUtils.getNewRawVisitorStatsForSiteGranularityQuantityAndDate(
            site, granularity, quantity, date, isCustomField
        )
        rawStats?.let { visitorStatsModel ->
            val periodIndex = visitorStatsModel.getIndexForField(WCNewVisitorStatsModel.VisitorStatsField.PERIOD)
            val fieldIndex = visitorStatsModel.getIndexForField(WCNewVisitorStatsModel.VisitorStatsField.VISITORS)
            return if (periodIndex == -1 || fieldIndex == -1) {
                mapOf()
            } else {
                // Years are returned as numbers by the API, and Gson interprets them as floats - clean up the decimal
                visitorStatsModel.dataList.map {
                    it[periodIndex].toString().removeSuffix(".0") to (it[fieldIndex] as Number).toInt()
                }.toMap()
            }
        } ?: return mapOf()
    }

    /**
     * Returns the currency code associated with stored stats for the [site], as an ISO 4217 currency code (eg. USD).
     */
    fun getStatsCurrencyForSite(site: SiteModel): String? {
        val rawStats = WCStatsSqlUtils.getFirstRawStatsForSite(site)
        rawStats?.let { statsModel ->
            statsModel.dataList.firstOrNull()?.let {
                val currencyIndex = statsModel.getIndexForField(OrderStatsField.CURRENCY)
                return if (currencyIndex == -1) {
                    // The server didn't return the currency field
                    reportMissingFieldError(statsModel, OrderStatsField.CURRENCY)
                    null
                } else {
                    it[currencyIndex] as String
                }
            }
        } ?: return null
    }

    /**
     * returns the quantity (how far back to go) to use when requesting stats for a specific granularity
     * and the date range
     */
    private fun getQuantityForOrderStatsApiUnit(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        startDate: String?,
        endDate: String?
    ): Int {
        val defaultValue = when (unit) {
            OrderStatsApiUnit.WEEK -> STATS_QUANTITY_WEEKS
            OrderStatsApiUnit.MONTH -> STATS_QUANTITY_MONTHS
            OrderStatsApiUnit.YEAR -> {
                // Years since 2011 (WooCommerce initial release), inclusive
                SiteUtils.getCurrentDateTimeForSite(site, DATE_FORMAT_YEAR).toInt() - WOO_COMMERCE_INITIAL_RELEASE + 1
            }
            else -> STATS_QUANTITY_DAYS
        }
        return getQuantityByOrderStatsApiUnit(startDate, endDate, unit, defaultValue).toInt()
    }

    /**
     * Given a {@param d1} start date, {@param d2} end date and the {@param unit} unit,
     * returns a quantity value.
     * If the start date or end date is empty, returns {@param defaultValue}
     */
    fun getQuantityByOrderStatsApiUnit(
        startDateString: String?,
        endDateString: String?,
        unit: OrderStatsApiUnit,
        defaultValue: Int
    ): Long {
        if (startDateString.isNullOrEmpty() || endDateString.isNullOrEmpty()) return defaultValue.toLong()

        val startDate = DateUtils.getDateFromString(startDateString)
        val endDate = DateUtils.getDateFromString(endDateString)

        val startDateCalendar = DateUtils.getStartDateCalendar(if (startDate.before(endDate)) startDate else endDate)
        val endDateCalendar = DateUtils.getEndDateCalendar(if (startDate.before(endDate)) endDate else startDate)

        return when (unit) {
            OrderStatsApiUnit.WEEK -> DateUtils.getQuantityInWeeks(startDateCalendar, endDateCalendar)
            OrderStatsApiUnit.MONTH -> DateUtils.getQuantityInMonths(startDateCalendar, endDateCalendar)
            OrderStatsApiUnit.YEAR -> DateUtils.getQuantityInYears(startDateCalendar, endDateCalendar)
            else -> DateUtils.getQuantityInDays(startDateCalendar, endDateCalendar)
        }
    }

    private fun fetchOrderStats(payload: FetchOrderStatsPayload) {
        val apiUnit = OrderStatsApiUnit.fromStatsGranularity(payload.granularity)
        val quantity = getQuantityForOrderStatsApiUnit(payload.site, apiUnit, payload.startDate, payload.endDate)
        wcOrderStatsClient.fetchStats(
                payload.site,
                apiUnit,
                getFormattedDateByOrderStatsApiUnit(payload.site, apiUnit, payload.endDate),
                quantity,
                payload.forced,
                payload.startDate,
                payload.endDate)
    }

    private fun fetchVisitorStats(payload: FetchVisitorStatsPayload) {
        val apiUnit = OrderStatsApiUnit.fromStatsGranularity(payload.granularity)
        val quantity = getQuantityForOrderStatsApiUnit(payload.site, apiUnit, payload.startDate, payload.endDate)
        wcOrderStatsClient.fetchVisitorStats(
                payload.site,
                apiUnit,
                getFormattedDateByOrderStatsApiUnit(payload.site, apiUnit, payload.endDate),
                quantity,
                payload.forced,
                payload.startDate,
                payload.endDate
        )
    }

    suspend fun fetchNewVisitorStats(payload: FetchNewVisitorStatsPayload): OnWCStatsChanged {
        val apiUnit = OrderStatsApiUnit.convertToVisitorsStatsApiUnit(payload.granularity)
        val startDate = payload.startDate?.takeIf { it.isNotEmpty() } ?: getStartDate(payload.granularity)
        val endDate = payload.endDate?.takeIf { it.isNotEmpty() } ?: getEndDate(payload.granularity, payload.site)
        val quantity = getQuantityForOrderStatsApiUnit(payload.site, apiUnit, startDate, endDate)
        return coroutineEngine.withDefaultContext(T.API, this, "fetchNewVisitorStats") {
            val result = wcOrderStatsClient.fetchNewVisitorStats(
                    site = payload.site,
                    unit = apiUnit,
                    granularity = payload.granularity,
                    date = getFormattedDateByOrderStatsApiUnit(payload.site, apiUnit, endDate),
                    quantity = quantity,
                    force = payload.forced,
                    startDate = startDate,
                    endDate = endDate
            )
            return@withDefaultContext if (result.isError || result.stats == null) {
                OnWCStatsChanged(0, payload.granularity).also {
                    it.error = result.error
                    it.causeOfChange = WCStatsAction.FETCH_NEW_VISITOR_STATS
                }
            } else {
                val rowsAffected = WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(result.stats)
                OnWCStatsChanged(
                        rowsAffected,
                        payload.granularity,
                        result.stats.quantity,
                        result.stats.date,
                        result.stats.isCustomField
                ).also {
                    it.causeOfChange = WCStatsAction.FETCH_NEW_VISITOR_STATS
                }
            }
        }
    }

    private fun getStartDate(granularity: StatsGranularity) =
        when (granularity) {
            StatsGranularity.DAYS -> DateUtils.getStartOfCurrentDay()
            StatsGranularity.WEEKS -> DateUtils.getFirstDayOfCurrentWeek(Calendar.getInstance(Locale.getDefault()))
            StatsGranularity.MONTHS -> DateUtils.getFirstDayOfCurrentMonth()
            StatsGranularity.YEARS -> DateUtils.getFirstDayOfCurrentYear()
        }

    private fun getEndDate(granularity: StatsGranularity, site: SiteModel) =
        when (granularity) {
            StatsGranularity.DAYS -> DateUtils.getStartOfCurrentDay()
            StatsGranularity.WEEKS -> DateUtils.getLastDayOfCurrentWeekForSite(site)
            StatsGranularity.MONTHS -> DateUtils.getLastDayOfCurrentMonthForSite(site)
            StatsGranularity.YEARS -> DateUtils.getLastDayOfCurrentYearForSite(site)
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
        val onStatsChanged = with(payload) {
            val granularity = StatsGranularity.fromOrderStatsApiUnit(apiUnit)
            if (isError || stats == null) {
                return@with OnWCStatsChanged(0, granularity).also { it.error = payload.error }
            } else {
                val rowsAffected = WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(stats)
                return@with OnWCStatsChanged(rowsAffected, granularity, stats.quantity, stats.date, stats.isCustomField)
            }
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
    private fun getFormattedDateByOrderStatsApiUnit(
        site: SiteModel,
        unit: OrderStatsApiUnit,
        endDate: String?
    ): String {
        return when (unit) {
            OrderStatsApiUnit.WEEK -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_WEEK, endDate)
            OrderStatsApiUnit.MONTH -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_MONTH, endDate)
            OrderStatsApiUnit.YEAR -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_YEAR, endDate)
            else -> DateUtils.getDateTimeForSite(site, DATE_FORMAT_DAY, endDate)
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
            site, apiUnit, quantity, date, isCustomField
        )
        rawStats?.let {
            val periodIndex = it.getIndexForField(OrderStatsField.PERIOD)
            val fieldIndex = it.getIndexForField(field)
            return if (periodIndex == -1 || fieldIndex == -1) {
                // One of the fields we need wasn't returned by the server
                reportMissingFieldError(it, field)
                mapOf()
            } else {
                // Years are returned as numbers by the API, and Gson interprets them as floats - clean up the decimal
                it.dataList.map { it[periodIndex].toString().removeSuffix(".0") to it[fieldIndex] as T }.toMap()
            }
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
     * Methods to support v4 revenue api changes
     */
    class OnWCRevenueStatsChanged(
        val rowsAffected: Int,
        val granularity: StatsGranularity,
        val startDate: String? = null,
        val endDate: String? = null,
        val availability: Boolean = false
    ) : OnChanged<OrderStatsError>() {
        var causeOfChange: WCStatsAction? = null
    }

    suspend fun fetchRevenueStats(payload: FetchRevenueStatsPayload): OnWCRevenueStatsChanged {
        val startDate = payload.startDate?.takeIf { it.isNotEmpty() }
            ?: getStartDateForRevenueStatsGranularity(payload.site, payload.granularity, payload.startDate)
        val endDate = payload.endDate?.takeIf { it.isNotEmpty() }
            ?: getEndDateForRevenueStatsGranularity(payload.site, payload.granularity, payload.endDate)

        return coroutineEngine.withDefaultContext(API, this, "fetchRevenueStats") {
            val result = wcOrderStatsClient.fetchRevenueStats(
                site = payload.site,
                granularity = payload.granularity,
                startDate = startDate,
                endDate = endDate,
                perPage = getPerPageQuantityForRevenueStatsGranularity(payload.granularity),
                forceRefresh = payload.forced,
                revenueRangeId = payload.revenueRangeId
            )

            with(result) {
                return@withDefaultContext if (isError || stats == null) {
                    OnWCRevenueStatsChanged(0, granularity)
                        .also { it.error = error }
                } else {
                    val rowsAffected = WCStatsSqlUtils.insertOrUpdateRevenueStats(stats)
                    OnWCRevenueStatsChanged(
                        rowsAffected,
                        granularity,
                        stats.startDate,
                        stats.endDate
                    )
                }
            }
        }
    }

    /**
     * Given a [startDate], formats the date based on the site's timezone in format yyyy-MM-dd'T'hh:mm:ss
     * If the start date is empty, fetches the date based on the [granularity]
     */
    private fun getStartDateForRevenueStatsGranularity(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String?
    ): String {
        return if (startDate.isNullOrEmpty()) {
            when (granularity) {
                StatsGranularity.DAYS -> DateUtils.getStartDateForSite(site, DateUtils.getStartOfCurrentDay())
                StatsGranularity.WEEKS -> DateUtils.getFirstDayOfCurrentWeekBySite(site)
                StatsGranularity.MONTHS -> DateUtils.getFirstDayOfCurrentMonthBySite(site)
                StatsGranularity.YEARS -> DateUtils.getFirstDayOfCurrentYearBySite(site)
            }
        } else {
            DateUtils.getStartDateForSite(site, startDate)
        }
    }

    /**
     * Given a [endDate], formats the date based on the site's timezone in format yyyy-MM-dd'T'hh:mm:ss
     * If the end date is empty, fetches the date based on the [granularity]
     */
    private fun getEndDateForRevenueStatsGranularity(
        site: SiteModel,
        granularity: StatsGranularity,
        endDate: String?
    ): String {
        return if (endDate.isNullOrEmpty()) {
            when (granularity) {
                StatsGranularity.DAYS -> DateUtils.getEndDateForSite(site)
                StatsGranularity.WEEKS -> DateUtils.getLastDayOfCurrentWeekForSite(site)
                StatsGranularity.MONTHS -> DateUtils.getLastDayOfCurrentMonthForSite(site)
                StatsGranularity.YEARS -> DateUtils.getLastDayOfCurrentYearForSite(site)
            }
        } else {
            DateUtils.getEndDateForSite(site, endDate)
        }
    }

    /**
     * Returns the page size in days depending on the provided [granularity],
     * to use for fetching revenue stats.
     */
    private fun getPerPageQuantityForRevenueStatsGranularity(
        granularity: StatsGranularity
    ) = when (granularity) {
        StatsGranularity.DAYS -> STATS_GRANULARITY_DAYS
        StatsGranularity.WEEKS -> Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_WEEK)
        StatsGranularity.MONTHS -> Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        StatsGranularity.YEARS -> STATS_GRANULARITY_YEARS
    }

    /**
     * Method to check if the v4 revenue stats api is available for this site.
     * The startDate passed to the api is the current date
     */
    private fun fetchRevenueStatsAvailability(payload: FetchRevenueStatsAvailabilityPayload) {
        val startDate = DateUtils.getStartDateForSite(payload.site, DateUtils.getStartOfCurrentDay())
        wcOrderStatsClient.fetchRevenueStatsAvailability(payload.site, startDate)
    }

    /**
     * Method returns a [payload] with the [Boolean] flag to indicate if the v4 revenue stats api
     * is available for this site.
     */
    private fun handleFetchRevenueStatsAvailabilityCompleted(payload: FetchRevenueStatsAvailabilityResponsePayload) {
        val onStatsChanged = with(payload) {
            if (isError) {
                return@with OnWCRevenueStatsChanged(
                        0, granularity = StatsGranularity.YEARS, availability = payload.available
                ).also { it.error = payload.error }
            } else {
                return@with OnWCRevenueStatsChanged(
                        0, granularity = StatsGranularity.YEARS, availability = payload.available
                )
            }
        }
        onStatsChanged.causeOfChange = WCStatsAction.FETCH_REVENUE_STATS_AVAILABILITY
        emitChange(onStatsChanged)
    }

    fun getGrossRevenueStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String
    ): Map<String, Double> {
        val rawStats = getRawRevenueStats(site, granularity, startDate, endDate)
        return rawStats?.getIntervalList()?.map {
            it.interval!! to it.subtotals?.totalSales!!
        }?.toMap() ?: mapOf()
    }

    fun getOrderCountStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String
    ): Map<String, Long> {
        val rawStats = getRawRevenueStats(site, granularity, startDate, endDate)
        return rawStats?.getIntervalList()?.map {
            it.interval!! to it.subtotals?.ordersCount!!
        }?.toMap() ?: mapOf()
    }

    fun getRawRevenueStats(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String
    ): WCRevenueStatsModel? {
        return WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                site, granularity, startDate, endDate)
    }

    fun getRawRevenueStatsFromRangeId(
        site: SiteModel,
        revenueRangeId: Int
    ) = WCStatsSqlUtils.getRevenueStatsFromRangeId(site, revenueRangeId)
}
