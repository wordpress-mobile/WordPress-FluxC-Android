package org.wordpress.android.fluxc.example.ui.stats

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_revenue_stats.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.utils.toggleSiteDependentButtons
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooRevenueStatsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcStatsStore: WCStatsStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var selectedSite: SiteModel? = null
    private var selectedPos: Int = -1

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_revenue_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stats_select_site.setOnClickListener {
            showSiteSelectorDialog(selectedPos, object : StoreSelectorDialog.Listener {
                override fun onSiteSelected(site: SiteModel, pos: Int) {
                    selectedSite = site
                    selectedPos = pos
                    buttonContainer.toggleSiteDependentButtons(true)
                    stats_select_site.text = site.name ?: site.displayName
                }
            })
        }

        fetch_revenue_stats_availability.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsAvailabilityPayload(it)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAvailabilityAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_day_revenue_stats.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_day_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_week_revenue_stats.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(it, StatsGranularity.WEEKS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_week_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(site = it, granularity = StatsGranularity.WEEKS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_month_revenue_stats.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(it, StatsGranularity.MONTHS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_month_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(site = it, granularity = StatsGranularity.MONTHS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_year_revenue_stats.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(it, StatsGranularity.YEARS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
            } ?: prependToLog("No site selected!")
        }

        fetch_current_year_revenue_stats_forced.setOnClickListener {
            selectedSite?.let {
                val payload = FetchRevenueStatsPayload(site = it, granularity = StatsGranularity.YEARS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(payload))
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        val site = selectedSite
        when (event.causeOfChange) {
            WCStatsAction.FETCH_REVENUE_STATS -> {
                if (event.isError) {
                    prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
                    return
                }

                val wcRevenueStatsModel = wcStatsStore.getRawRevenueStats(
                        site!!,
                        event.granularity,
                        event.startDate!!,
                        event.endDate!!)
                wcRevenueStatsModel?.let {
                    val revenueSum = it.parseTotal()?.totalSales
                    prependToLog("Fetched stats with total " + revenueSum + " for granularity " +
                            event.granularity.toString().toLowerCase() + " from " + site.name +
                            " between " + event.startDate + " and " + event.endDate)
                } ?: prependToLog("No stats were stored for site " + site.name + " =(")
            }

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

    private fun showSiteSelectorDialog(selectedPos: Int, listener: StoreSelectorDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = StoreSelectorDialog.newInstance(listener, selectedPos)
            dialog.show(fm, "StoreSelectorDialog")
        }
    }
}
