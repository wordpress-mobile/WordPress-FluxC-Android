package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.logging.FluxCCrashLoggerProvider.crashLogger
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WCVisitorStatsSqlUtils
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
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
        private const val DATE_FORMAT_DAY = "yyyy-MM-dd"

        // Use WordPress's maximum per page quantity
        private const val ORDER_REVENUE_QUANTITY = 100
    }

    enum class StatsGranularity {
        HOURS, DAYS, WEEKS, MONTHS, YEARS;

        companion object {
            fun fromOrderStatsApiUnit(apiUnit: OrderStatsApiUnit): StatsGranularity {
                return when (apiUnit) {
                    OrderStatsApiUnit.HOUR -> StatsGranularity.HOURS
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
        val startDate: String,
        val endDate: String,
        val forced: Boolean = false,
        val revenueRangeId: String = ""
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
     * (depending on the given [granularity]). These classes are used exclusively for the new v4 stats API changes
     *
     * @param[granularity] the time units for the requested data
     * @param[forced] if true, ignores any cached result and forces a refresh from the server
     */
    class FetchNewVisitorStatsPayload(
        val site: SiteModel,
        val granularity: StatsGranularity,
        val startDate: String,
        val endDate: String,
        val forced: Boolean = false
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

    override fun onRegister() = AppLog.d(T.API, "WCStatsStore onRegister")

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCStatsAction ?: return
        when (actionType) {
            WCStatsAction.FETCH_REVENUE_STATS_AVAILABILITY ->
                fetchRevenueStatsAvailability(action.payload as FetchRevenueStatsAvailabilityPayload)
            WCStatsAction.FETCHED_REVENUE_STATS_AVAILABILITY -> handleFetchRevenueStatsAvailabilityCompleted(
                    action.payload as FetchRevenueStatsAvailabilityResponsePayload
            )
        }
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
                getVisitorsMap(
                    periodIndex = periodIndex,
                    fieldIndex = fieldIndex,
                    dataList = visitorStatsModel.dataList
                )
            }
        } ?: return mapOf()
    }

    private fun getVisitorsMap(
        periodIndex: Int, fieldIndex: Int, dataList: List<List<Any>>
    ): Map<String, Int> {
        return dataList.associate {
            // Years are returned as numbers by the API, and Gson interprets them as floats - clean up the decimal
            val period = it[periodIndex].toString().removeSuffix(".0")

            // Some plugins can change the field type
            val visitsRawValue = it[fieldIndex]
            val visits = if (visitsRawValue is Number) {
                visitsRawValue.toInt()
            } else {
                crashLogger?.recordException(
                    exception = NumberFormatException("$visitsRawValue is not a valid number"),
                    category = null
                )
                (visitsRawValue as? String)?.toDoubleOrNull()?.toInt() ?: 0
            }

            period to visits
        }
    }

    suspend fun fetchNewVisitorStats(payload: FetchNewVisitorStatsPayload): OnWCStatsChanged {
        if (payload.granularity == StatsGranularity.HOURS) {
            error("Visitor stats do not support hours granularity")
        }
        val startDate = payload.startDate
        val endDate = payload.endDate
        val quantity = getVisitorStatsQuantity(payload.granularity, startDate, endDate)
        return coroutineEngine.withDefaultContext(T.API, this, "fetchNewVisitorStats") {
            val result = wcOrderStatsClient.fetchNewVisitorStats(
                    site = payload.site,
                    granularity = payload.granularity,
                    date = DateUtils.getDateTimeForSite(payload.site, DATE_FORMAT_DAY, endDate),
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

    /**
     * returns the quantity (how far back to go) to use when requesting visitor stats for a specific granularity
     * and the date range
     */
    fun getVisitorStatsQuantity(
        granularity: StatsGranularity,
        startDateString: String,
        endDateString: String
    ): Int {
        val startDate = DateUtils.getDateFromString(startDateString)
        val endDate = DateUtils.getDateFromString(endDateString)

        val startDateCalendar = DateUtils.getStartDateCalendar(if (startDate.before(endDate)) startDate else endDate)
        val endDateCalendar = DateUtils.getEndDateCalendar(if (startDate.before(endDate)) endDate else startDate)

        return when (granularity) {
            StatsGranularity.DAYS -> DateUtils.getQuantityInDays(startDateCalendar, endDateCalendar)
            StatsGranularity.WEEKS -> DateUtils.getQuantityInWeeks(startDateCalendar, endDateCalendar)
            StatsGranularity.MONTHS -> DateUtils.getQuantityInMonths(startDateCalendar, endDateCalendar)
            StatsGranularity.YEARS -> DateUtils.getQuantityInYears(startDateCalendar, endDateCalendar)
            else -> error("Visitor stats do not support hours granularity")
        }.toInt()
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
        val startDate = payload.startDate
        val endDate = payload.endDate

        return coroutineEngine.withDefaultContext(API, this, "fetchRevenueStats") {
            val result = wcOrderStatsClient.fetchRevenueStats(
                site = payload.site,
                granularity = payload.granularity,
                startDate = startDate,
                endDate = endDate,
                perPage = ORDER_REVENUE_QUANTITY,
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
        revenueRangeId: String
    ) = WCStatsSqlUtils.getRevenueStatsFromRangeId(site, revenueRangeId)
}
