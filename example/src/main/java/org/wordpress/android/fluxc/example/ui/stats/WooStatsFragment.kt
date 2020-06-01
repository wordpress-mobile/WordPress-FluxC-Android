package org.wordpress.android.fluxc.example.ui.stats

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_stats.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.CustomStatsDialog
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_ORDER_STATS
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_ORDER_STATS_FORCED
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_VISITOR_STATS
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_VISITOR_STATS_FORCED
import org.wordpress.android.fluxc.example.R
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooStatsFragment : Fragment(), CustomStatsDialog.Listener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcStatsStore: WCStatsStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var visitorStatsStartDate: String? = null
    private var visitorStatsEndDate: String? = null
    private var visitorStatsGranularity: String? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woo_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_order_stats.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
            }
        }

        fetch_order_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
            }
        }

        fetch_order_stats_custom.setOnClickListener {
            fragmentManager?.let { fm ->
                val wcOrderStatsModel = getCustomStatsForSite()
                val dialog = CustomStatsDialog.newInstance(
                        this,
                        wcOrderStatsModel?.startDate,
                        wcOrderStatsModel?.endDate,
                        wcOrderStatsModel?.unit,
                        FETCH_CUSTOM_ORDER_STATS
                )
                dialog.show(fm, "CustomStatsFragment")
            }
        }

        fetch_order_stats_custom_forced.setOnClickListener {
            fragmentManager?.let { fm ->
                val wcOrderStatsModel = getCustomStatsForSite()
                val dialog = CustomStatsDialog.newInstance(
                        this,
                        wcOrderStatsModel?.startDate,
                        wcOrderStatsModel?.endDate,
                        wcOrderStatsModel?.unit,
                        FETCH_CUSTOM_ORDER_STATS_FORCED
                )
                dialog.show(fm, "CustomStatsFragment")
            }
        }

        fetch_visitor_stats.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchVisitorStatsPayload(it, StatsGranularity.DAYS, false)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchVisitorStatsAction(payload))
            }
        }

        fetch_visitor_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchVisitorStatsPayload(it, StatsGranularity.DAYS, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchVisitorStatsAction(payload))
            }
        }

        fetch_visitor_stats_custom.setOnClickListener {
            fragmentManager?.let { fm ->
                val dialog = CustomStatsDialog.newInstance(
                        this,
                        visitorStatsStartDate,
                        visitorStatsEndDate,
                        visitorStatsGranularity,
                        FETCH_CUSTOM_VISITOR_STATS
                )
                dialog.show(fm, "CustomStatsFragment")
            }
        }

        fetch_visitor_stats_custom_forced.setOnClickListener {
            fragmentManager?.let { fm ->
                val dialog = CustomStatsDialog.newInstance(
                        this,
                        visitorStatsStartDate,
                        visitorStatsEndDate,
                        visitorStatsGranularity,
                        FETCH_CUSTOM_VISITOR_STATS_FORCED
                )
                dialog.show(fm, "CustomStatsFragment")
            }
        }

        fetch_top_earners_stats.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchTopEarnersStatsPayload(it, StatsGranularity.DAYS, 10, false)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
            }
        }

        fetch_top_earners_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchTopEarnersStatsPayload(it, StatsGranularity.DAYS, 10, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        val site = getFirstWCSite()
        when (event.causeOfChange) {
            WCStatsAction.FETCH_ORDER_STATS -> {
                val statsMap = wcStatsStore.getRevenueStats(
                        site!!,
                        event.granularity,
                        event.quantity,
                        event.date,
                        event.isCustomField)
                if (statsMap.isEmpty()) {
                    prependToLog("No stats were stored for site " + site.name + " =(")
                } else {
                    if (event.isCustomField) {
                        prependToLog("Fetched stats for " + statsMap.size + " " +
                                event.granularity.toString().toLowerCase() + " from " + site.name +
                                " with quantity " + event.quantity + " and date " + event.date)
                    } else {
                        prependToLog("Fetched stats for " + statsMap.size + " " +
                                event.granularity.toString().toLowerCase() + " from " + site.name)
                    }
                }
            }
            WCStatsAction.FETCH_VISITOR_STATS -> {
                val visitorStatsMap = wcStatsStore.getVisitorStats(
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCTopEarnersChanged(event: OnWCTopEarnersChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        prependToLog(
                "Fetched ${event.topEarners.size} top earner stats for ${event.granularity.toString()
                        .toLowerCase()} from ${getFirstWCSite()?.name}"
        )
    }

    override fun onSubmitted(
        startDate: String,
        endDate: String,
        granularity: StatsGranularity,
        wcOrderStatsAction: WCOrderStatsAction?
    ) {
        getFirstWCSite()?.let {
            val action = when (wcOrderStatsAction) {
                FETCH_CUSTOM_ORDER_STATS -> WCStatsActionBuilder.newFetchOrderStatsAction(
                        FetchOrderStatsPayload(it, granularity, startDate, endDate)
                )
                FETCH_CUSTOM_ORDER_STATS_FORCED -> WCStatsActionBuilder.newFetchOrderStatsAction(
                        FetchOrderStatsPayload(it, granularity, startDate, endDate, forced = true)
                )
                else -> {
                    val forced = wcOrderStatsAction == FETCH_CUSTOM_VISITOR_STATS_FORCED
                    visitorStatsStartDate = startDate
                    visitorStatsEndDate = endDate
                    visitorStatsGranularity = OrderStatsApiUnit.fromStatsGranularity(granularity).name
                    WCStatsActionBuilder.newFetchVisitorStatsAction(
                            FetchVisitorStatsPayload(it, granularity, forced, startDate, endDate)
                    )
                }
            }
            dispatcher.dispatch(action)
        }
    }

    private fun getCustomStatsForSite() = getFirstWCSite()?.let {
        wcStatsStore.getCustomStatsForSite(it)
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)
}
