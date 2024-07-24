package org.wordpress.android.fluxc.example.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.databinding.FragmentWooRevenueStatsBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import javax.inject.Inject

class WooRevenueStatsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcStatsStore: WCStatsStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooRevenueStatsBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooRevenueStatsBinding.bind(view)) {
            fetchRevenueStatsAvailability.setOnClickListener {
                selectedSite?.let {
                    val payload = FetchRevenueStatsAvailabilityPayload(it)
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAvailabilityAction(payload))
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentDayRevenueStats.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.DAYS,
                            startDate = DateUtils.getStartDateForSite(it, DateUtils.getStartOfCurrentDay()),
                            endDate = DateUtils.getEndDateForSite(it)
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog("Revenue stats ${it.rowsAffected != 0}")
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentDayRevenueStatsForced.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.DAYS,
                            forced = true,
                            startDate = DateUtils.getStartDateForSite(it, DateUtils.getStartOfCurrentDay()),
                            endDate = DateUtils.getEndDateForSite(it)
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats with granularity ${StatsGranularity.DAYS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentWeekRevenueStats.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.WEEKS,
                            startDate = DateUtils.getFirstDayOfCurrentWeekBySite(it),
                            endDate = DateUtils.getLastDayOfCurrentWeekForSite(it)
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats with granularity ${StatsGranularity.WEEKS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentWeekRevenueStatsForced.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.WEEKS,
                            startDate = DateUtils.getFirstDayOfCurrentWeekBySite(it),
                            endDate = DateUtils.getLastDayOfCurrentWeekForSite(it),
                            forced = true
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats forced with granularity ${StatsGranularity.WEEKS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentMonthRevenueStats.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.MONTHS,
                            startDate = DateUtils.getFirstDayOfCurrentMonthBySite(it),
                            endDate = DateUtils.getLastDayOfCurrentMonthForSite(it)
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats with granularity ${StatsGranularity.MONTHS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentMonthRevenueStatsForced.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.MONTHS,
                            startDate = DateUtils.getFirstDayOfCurrentMonthBySite(it),
                            endDate = DateUtils.getLastDayOfCurrentMonthForSite(it),
                            forced = true
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats forced with granularity ${StatsGranularity.MONTHS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentYearRevenueStats.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.YEARS,
                            startDate = DateUtils.getFirstDayOfCurrentYearBySite(it),
                            endDate = DateUtils.getLastDayOfCurrentYearForSite(it)
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats with granularity ${StatsGranularity.YEARS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentYearRevenueStatsForced.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        val payload = FetchRevenueStatsPayload(
                            site = it,
                            granularity = StatsGranularity.YEARS,
                            startDate = DateUtils.getFirstDayOfCurrentYearBySite(it),
                            endDate = DateUtils.getLastDayOfCurrentYearForSite(it),
                            forced = true
                        )
                        wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                            prependToLog(
                                "Revenue stats forced with granularity ${StatsGranularity.YEARS} " +
                                    ": ${it.rowsAffected != 0}"
                            )
                            onFetchRevenueStatsLoaded(it)
                        } ?: prependToLog("Fetching revenueStats failed.")
                    }
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentDayVisitorStats.setOnClickListener {
                selectedSite?.let {
                    val payload = FetchNewVisitorStatsPayload(
                        site = it,
                        granularity = StatsGranularity.DAYS,
                        startDate = DateUtils.getStartDateForSite(it, DateUtils.getStartOfCurrentDay()),
                        endDate = DateUtils.getEndDateForSite(it)
                    )
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentWeekVisitorStatsForced.setOnClickListener {
                selectedSite?.let {
                    val payload = FetchNewVisitorStatsPayload(
                        site = it,
                        granularity = StatsGranularity.WEEKS,
                        startDate = DateUtils.getFirstDayOfCurrentWeekBySite(it),
                        endDate = DateUtils.getLastDayOfCurrentWeekForSite(it),
                        forced = true
                    )
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentMonthVisitorStats.setOnClickListener {
                selectedSite?.let {
                    val payload = FetchNewVisitorStatsPayload(
                        site = it,
                        granularity = StatsGranularity.MONTHS,
                        startDate = DateUtils.getFirstDayOfCurrentMonthBySite(it),
                        endDate = DateUtils.getLastDayOfCurrentMonthForSite(it)
                    )
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
                } ?: prependToLog("No site selected!")
            }

            fetchCurrentYearVisitorStatsForced.setOnClickListener {
                selectedSite?.let {
                    val payload = FetchNewVisitorStatsPayload(
                        site = it,
                        granularity = StatsGranularity.YEARS,
                        startDate = DateUtils.getFirstDayOfCurrentYearBySite(it),
                        endDate = DateUtils.getLastDayOfCurrentYearForSite(it),
                        forced = true
                    )
                    dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
                } ?: prependToLog("No site selected!")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    private fun onFetchRevenueStatsLoaded(event: OnWCRevenueStatsChanged) {
        selectedSite?.let { site ->
            if (event.isError) {
                prependToLog("Error fetching order status options from the api: ${event.error.message}")
                return
            }

            val wcRevenueStatsModel = wcStatsStore.getRawRevenueStats(
                site,
                event.granularity,
                event.startDate!!,
                event.endDate!!
            )
            wcRevenueStatsModel?.let {
                val revenueSum = it.parseTotal()?.totalSales
                prependToLog(
                    "Fetched stats with total " + revenueSum + " for granularity " +
                        event.granularity.toString().toLowerCase() + " from " + site.name +
                        " between " + event.startDate + " and " + event.endDate
                )
            } ?: prependToLog("No stats were stored for site " + site.name + " =(")
        } ?: prependToLog("No stats were stored for site " + selectedSite?.name + " =(")
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        val site = selectedSite
        when (event.causeOfChange) {
            WCStatsAction.FETCH_REVENUE_STATS_AVAILABILITY -> {
                prependToLog("Revenue stats available for site ${site?.name}: ${event.availability}")
            }
            WCStatsAction.FETCH_NEW_VISITOR_STATS -> Unit // Do nothing
            WCStatsAction.FETCHED_REVENUE_STATS_AVAILABILITY -> Unit // Do nothing
            null -> Unit // Do nothing
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        val site = selectedSite
        when (event.causeOfChange) {
            WCStatsAction.FETCH_NEW_VISITOR_STATS -> {
                val visitorStatsMap = wcStatsStore.getNewVisitorStats(
                    site!!,
                    event.granularity,
                    event.quantity,
                    event.date,
                    event.isCustomField
                )
                if (visitorStatsMap.isEmpty()) {
                    prependToLog("No visitor stats were stored for site " + site.name + " =(")
                } else {
                    if (event.isCustomField) {
                        prependToLog(
                            "Fetched visitor stats for " + visitorStatsMap.size + " " +
                                event.granularity.toString().toLowerCase() + " from " + site.name +
                                " with quantity " + event.quantity + " and date " + event.date
                        )
                    } else {
                        prependToLog(
                            "Fetched visitor stats for " + visitorStatsMap.size + " " +
                                event.granularity.toString().toLowerCase() + " from " + site.name
                        )
                    }
                }
            }
            WCStatsAction.FETCH_REVENUE_STATS_AVAILABILITY -> Unit // Do nothing
            WCStatsAction.FETCHED_REVENUE_STATS_AVAILABILITY -> Unit // Do nothing
            null -> Unit // Do nothing
        }
    }
}
