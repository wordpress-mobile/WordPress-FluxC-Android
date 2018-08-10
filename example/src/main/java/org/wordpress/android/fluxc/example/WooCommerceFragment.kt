package org.wordpress.android.fluxc.example

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.fragment_woocommerce.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class WooCommerceFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcOrderStore: WCOrderStore
    @Inject internal lateinit var wcStatsStore: WCStatsStore

    private var pendingNotesOrderModel: WCOrderModel? = null
    private var pendingFetchOrdersFilter: List<String>? = null
    private var pendingFetchCompletedOrders: Boolean = false

    override fun onAttach(context: Context?) {
        AndroidInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woocommerce, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log_sites.setOnClickListener {
            for (site in wooCommerceStore.getWooCommerceSites()) {
                prependToLog(site.name + ": " + if (site.isWpComStore) "WP.com store" else "Self-hosted store")
                AppLog.i(T.API, LogUtils.toString(site))
            }
        }

        fetch_orders.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrdersPayload(it, loadMore = false)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_orders_by_status.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter comma-separated list of statuses to filter by:") { editText ->
                    pendingFetchOrdersFilter = editText.text.toString()
                            .takeIf { it.trim().isNotEmpty() }
                            ?.split(",")
                            ?.mapNotNull { if (it.trim().isNotEmpty()) it.trim() else null }
                    // only use the status for filtering if it's not empty
                    if (pendingFetchOrdersFilter?.count() == 0) {
                        pendingFetchOrdersFilter = null
                        prependToLog("No valid filters defined, fetching all orders...")
                    } else {
                        prependToLog("Submitting request to fetch " +
                                "orders matching the following statuses $pendingFetchOrdersFilter")
                    }
                    // First fetch orders from the API to seed the database with data before attempting to pull
                    // orders by order status.
                    val payload = FetchOrdersPayload(site, loadMore = false)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
                }
            }
        }

        fetch_orders_by_status_api.setOnClickListener {
            getFirstWCSite()?.let { site ->
                prependToLog("Submitting request to fetch only completed orders from the api")
                pendingFetchCompletedOrders = true
                val payload = FetchOrdersPayload(site, loadMore = false, statusFilter = CoreOrderStatus.COMPLETED.value)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            }
        }

        fetch_order_notes.setOnClickListener {
            getFirstWCSite()?.let { site ->
                getFirstWCOrder()?.let { order ->
                    pendingNotesOrderModel = order
                    val payload = FetchOrderNotesPayload(order, site)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
                }
            }
        }

        post_order_note.setOnClickListener {
            getFirstWCSite()?.let { site ->
                getFirstWCOrder()?.let { order ->
                    pendingNotesOrderModel = order
                    showSingleLineDialog(activity, "Enter note") { editText ->
                        val newNote = WCOrderNoteModel().apply {
                            note = editText.text.toString()
                        }
                        val payload = PostOrderNotePayload(order, site, newNote)
                        dispatcher.dispatch(WCOrderActionBuilder.newPostOrderNoteAction(payload))
                    }
                }
            }
        }

        update_latest_order_status.setOnClickListener {
            getFirstWCSite()?.let { site ->
                wcOrderStore.getOrdersForSite(site).firstOrNull()?.let { order ->
                    showSingleLineDialog(activity, "Enter new order status") { editText ->
                        val status = editText.text.toString()
                        val payload = UpdateOrderStatusPayload(order, site, status)
                        dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
                    }
                } ?: showNoOrdersToast(site)
            } ?: showNoWCSitesToast()
        }

        fetch_order_stats.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_order_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_top_earners_stats.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchTopEarnersStatsPayload(it, StatsGranularity.DAYS, 10, false)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_top_earners_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchTopEarnersStatsPayload(it, StatsGranularity.DAYS, 10, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
            } ?: showNoWCSitesToast()
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
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        getFirstWCSite()?.let { site ->
            wcOrderStore.getOrdersForSite(site).let { orderList ->
                if (orderList.isEmpty()) {
                    prependToLog("No orders were stored for site " + site.name + " =(")
                    return
                }

                when (event.causeOfChange) {
                    FETCH_ORDERS -> {
                        pendingFetchOrdersFilter?.let { filter ->
                            getFirstWCSite()?.let { site ->
                                // get orders and group by order.status
                                val orders = wcOrderStore.getOrdersForSite(site, *filter.toTypedArray())
                                        .groupBy { order -> order.status }
                                // print count of orders fetched by filtered status
                                filter.forEach { status ->
                                    prependToLog("Fetched ${orders[status]?.count() ?: 0} orders for status [$status]")
                                }
                                pendingFetchOrdersFilter = null
                            }
                        } ?: if (pendingFetchCompletedOrders) {
                            pendingFetchCompletedOrders = false
                            val completedOrders = wcOrderStore.getOrdersForSite(site, "completed")
                            prependToLog("Fetched ${completedOrders.size} completed orders from ${site.name}")
                        } else {
                            prependToLog("Fetched ${event.rowsAffected} orders from: ${site.name}")
                        }
                    }
                    FETCH_ORDER_NOTES -> {
                        val notes = wcOrderStore.getOrderNotesForOrder(pendingNotesOrderModel!!)
                        prependToLog(
                            "Fetched ${notes.size} order notes for order " +
                                    "${pendingNotesOrderModel!!.remoteOrderId}. ${event.rowsAffected} " +
                                    "notes inserted into database.")
                    }
                    POST_ORDER_NOTE -> prependToLog("Posted ${event.rowsAffected} " +
                            "note to the api for order ${pendingNotesOrderModel!!.remoteOrderId}")
                    UPDATE_ORDER_STATUS ->
                        with (orderList[0]) { prependToLog("Updated order status for $number to $status") }
                    else -> prependToLog("Order store was updated from a " + event.causeOfChange)
                }
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

        getFirstWCSite()?.let { site ->
            wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS).let { statsMap ->
                if (statsMap.isEmpty()) {
                    prependToLog("No stats were stored for site " + site.name + " =(")
                    return
                }

                when (event.causeOfChange) {
                    WCStatsAction.FETCH_ORDER_STATS ->
                        prependToLog("Fetched stats for " + statsMap.size + " " +
                                event.granularity.toString().toLowerCase() + " from " + site.name)
                    else -> prependToLog("WooCommerce stats were updated from a " + event.causeOfChange)
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

        prependToLog("Fetched ${event.topEarners.size} top earner stats for ${event.granularity.toString()
                .toLowerCase()} from ${getFirstWCSite()?.name}")
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)

    private fun getFirstWCOrder() = getFirstWCSite()?.let {
        wcOrderStore.getOrdersForSite(it).getOrNull(0)
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)

    private fun showNoWCSitesToast() {
        ToastUtils.showToast(activity, "No WooCommerce sites found for this account!")
    }

    private fun showNoOrdersToast(site: SiteModel) {
        ToastUtils.showToast(activity, "No orders found for site: " + site.name)
    }
}
