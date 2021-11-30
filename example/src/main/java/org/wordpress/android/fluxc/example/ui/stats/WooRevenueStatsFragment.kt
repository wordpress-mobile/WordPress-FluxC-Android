package org.wordpress.android.fluxc.example.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_revenue_stats.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.R
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
import javax.inject.Inject

class WooRevenueStatsFragment : StoreSelectingFragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcStatsStore: WCStatsStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_revenue_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_revenue_stats_availability.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsAvailabilityPayload(it)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAvailabilityAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_day_revenue_stats.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS)
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_day_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, forced = true)
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats with granularity ${StatsGranularity.DAYS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_week_revenue_stats.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(it, StatsGranularity.WEEKS)
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats with granularity ${StatsGranularity.WEEKS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_week_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(site = it,
                            granularity = StatsGranularity.WEEKS,
                            forced = true
                    )
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats forced with granularity ${StatsGranularity.WEEKS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_month_revenue_stats.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(it, StatsGranularity.MONTHS)
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats with granularity ${StatsGranularity.MONTHS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_month_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(site = it,
                            granularity = StatsGranularity.MONTHS,
                            forced = true
                    )
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats forced with granularity ${StatsGranularity.MONTHS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_year_revenue_stats.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(it, StatsGranularity.YEARS)
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats with granularity ${StatsGranularity.YEARS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_year_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                coroutineScope.launch {
                    val payload = FetchRevenueStatsPayload(site = it,
                            granularity = StatsGranularity.YEARS,
                            forced = true
                    )
                    wcStatsStore.fetchRevenueStats(payload).takeUnless { it.isError }?.let {
                        prependToLog("Revenue stats forced with granularity ${StatsGranularity.YEARS} " +
                                ": ${it.rowsAffected != 0}")
                        onFetchRevenueStatsLoaded(it)
                    } ?: prependToLog("Fetching revenueStats failed.")
                }
            } ?: prependToLog("No site selected!")
        }

        fetch_current_day_visitor_stats.setOnClickListener {
            selectedSite?.let {
                val payload = FetchNewVisitorStatsPayload(site = it, granularity = StatsGranularity.DAYS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_week_visitor_stats_forced.setOnClickListener {
            selectedSite?.let {
                val payload = FetchNewVisitorStatsPayload(it, StatsGranularity.WEEKS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_month_visitor_stats.setOnClickListener {
            selectedSite?.let {
                val payload = FetchNewVisitorStatsPayload(site = it, granularity = StatsGranularity.MONTHS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_year_visitor_stats_forced.setOnClickListener {
            selectedSite?.let {
                val payload = FetchNewVisitorStatsPayload(it, StatsGranularity.YEARS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(payload))
            } ?: prependToLog("No site selected!")
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
                    event.endDate!!)
            wcRevenueStatsModel?.let {
                val revenueSum = it.parseTotal()?.totalSales
                prependToLog("Fetched stats with total " + revenueSum + " for granularity " +
                        event.granularity.toString().toLowerCase() + " from " + site.name +
                        " between " + event.startDate + " and " + event.endDate)
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
        }
    }
}
