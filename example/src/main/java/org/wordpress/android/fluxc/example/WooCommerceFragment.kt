package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woocommerce.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_SINGLE_ORDER
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchSingleOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.OnApiVersionFetched
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
    private var pendingFetchSingleOrderRemoteId: Long? = null
    private var pendingFetchOrdersKeyword: String ? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_woocommerce, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        log_sites.setOnClickListener {
            for (site in wooCommerceStore.getWooCommerceSites()) {
                prependToLog(site.name + ": " + if (site.isWpComStore) "WP.com store" else "Self-hosted store")
                AppLog.i(T.API, LogUtils.toString(site))
            }
        }

        log_woo_api_versions.setOnClickListener {
            for (site in wooCommerceStore.getWooCommerceSites()) {
                dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteApiVersionAction(site))
            }
        }

        fetch_orders.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrdersPayload(it, loadMore = false)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_orders_count.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter a single order status to filter by or leave blank for no filter:"
                ) { editText ->

                    // only use the status for filtering if it's not empty
                    val statusFilter = editText.text.toString().trim().takeIf { it.isNotEmpty() }
                    statusFilter?.let {
                        prependToLog("Submitting request to fetch a count of $it orders")
                    } ?: prependToLog("No valid filters defined, fetching count of all orders")

                    val payload = FetchOrdersCountPayload(site, statusFilter)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersCountAction(payload))
                }
            }
        }

        fetch_single_order.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the remoteOrderId of order to fetch:") { editText ->
                    pendingFetchSingleOrderRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleOrderRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch order by remoteOrderID" +
                                ": $pendingFetchSingleOrderRemoteId")
                        val payload = FetchSingleOrderPayload(site, id)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchSingleOrderAction(payload))
                    } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                }
            }
        }

        fetch_has_orders.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchHasOrdersPayload(it)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchHasOrdersAction(payload))
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
                        prependToLog(
                                "Submitting request to fetch " +
                                        "orders matching the following statuses $pendingFetchOrdersFilter"
                        )
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

        fetch_orders_by_keyword.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter a keyword to filter by:") { editText ->
                    pendingFetchOrdersKeyword = editText.text.toString()
                    prependToLog("Submitting request to fetch orders matching keyword $pendingFetchOrdersKeyword")
                    val payload = FetchOrdersPayload(
                            site,
                            statusFilter = null,
                            keywordFilter = pendingFetchOrdersKeyword,
                            loadMore = false
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
                }
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

        fetch_visitor_stats.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchVisitorStatsPayload(it, StatsGranularity.MONTHS, false)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchVisitorStatsAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_visitor_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchVisitorStatsPayload(it, StatsGranularity.MONTHS, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchVisitorStatsAction(payload))
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
    fun onApiVersionFetched(event: OnApiVersionFetched) {
        if (event.isError) {
            prependToLog("Error in onApiVersionFetched: ${event.error.type} - ${event.error.message}")
            return
        }

        with(event) {
            val formattedVersion = apiVersion.substringAfterLast("/")
            prependToLog("Max Woo version for ${site.name}: $formattedVersion")
        }
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
                // We check if the rowsAffected value is zero because not all events will causes data to be
                // saved to the orders table (such as the FETCH-ORDERS-COUNT...so the orderList would always
                // be empty even if there were orders available.
                if (orderList.isEmpty() && event.rowsAffected == 0) {
                    prependToLog("No orders were stored for site " + site.name + " =(")
                    return
                }

                when (event.causeOfChange) {
                    FETCH_ORDERS -> {
                        when {
                            pendingFetchOrdersKeyword != null -> {
                                prependToLog("Fetched ${event.rowsAffected} orders from: ${site.name} " +
                                        "matching keyword $pendingFetchOrdersKeyword")
                                pendingFetchOrdersKeyword = null
                            }
                            pendingFetchOrdersFilter != null -> {
                                // get orders and group by order.status
                                val orders = wcOrderStore.getOrdersForSite(
                                        site,
                                        *pendingFetchOrdersFilter!!.toTypedArray()
                                ).groupBy { order -> order.status }
                                // print count of orders fetched by filtered status
                                pendingFetchOrdersFilter!!.forEach { status ->
                                    prependToLog("Fetched ${orders[status]?.count() ?: 0} orders for status [$status]")
                                }
                                pendingFetchOrdersFilter = null
                            }
                            pendingFetchCompletedOrders -> {
                                pendingFetchCompletedOrders = false
                                val completedOrders = wcOrderStore.getOrdersForSite(site, "completed")
                                prependToLog("Fetched ${completedOrders.size} completed orders from ${site.name}")
                            }
                            else -> {
                                prependToLog("Fetched ${event.rowsAffected} orders from: ${site.name}")
                                prependToLog("printing the first 5 remoteOrderId's from result:")
                                val orders = wcOrderStore.getOrdersForSite(site)
                                orders.take(5).forEach { prependToLog("- remoteOrderId [${it.remoteOrderId}]") }
                            }
                        }
                    }
                    FETCH_ORDERS_COUNT -> {
                        val append = if (event.canLoadMore) "+" else ""
                        event.statusFilter?.let {
                            prependToLog("Count of $it orders: ${event.rowsAffected}$append")
                        } ?: prependToLog("Count of all orders: ${event.rowsAffected}$append")
                    }
                    FETCH_SINGLE_ORDER -> {
                        pendingFetchSingleOrderRemoteId?.let { remoteId ->
                            pendingFetchSingleOrderRemoteId = null
                            wcOrderStore.getOrderByIdentifier(OrderIdentifier(
                                    WCOrderModel().apply {
                                        remoteOrderId = remoteId
                                        localSiteId = site.id
                                    })
                            )?.let {
                                prependToLog("Single order fetched successfully!")
                            } ?: prependToLog("WARNING: Fetched order not found in the local database!")
                        }
                    }
                    FETCH_HAS_ORDERS -> {
                        val hasOrders = event.rowsAffected > 0
                        prependToLog("Store has orders: $hasOrders")
                    }
                    FETCH_ORDER_NOTES -> {
                        val notes = wcOrderStore.getOrderNotesForOrder(pendingNotesOrderModel!!)
                        prependToLog(
                                "Fetched ${notes.size} order notes for order " +
                                        "${pendingNotesOrderModel!!.remoteOrderId}. ${event.rowsAffected} " +
                                        "notes inserted into database."
                        )
                    }
                    POST_ORDER_NOTE -> prependToLog(
                            "Posted ${event.rowsAffected} " +
                                    "note to the api for order ${pendingNotesOrderModel!!.remoteOrderId}"
                    )
                    UPDATE_ORDER_STATUS ->
                        with(orderList[0]) { prependToLog("Updated order status for $number to $status") }
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

        val site = getFirstWCSite()
        when (event.causeOfChange) {
            WCStatsAction.FETCH_ORDER_STATS -> {
                val statsMap = wcStatsStore.getRevenueStats(site!!, event.granularity)
                if (statsMap.isEmpty()) {
                    prependToLog("No stats were stored for site " + site.name + " =(")
                } else {
                    prependToLog("Fetched stats for " + statsMap.size + " " +
                                    event.granularity.toString().toLowerCase() + " from " + site.name)
                }
            }
            WCStatsAction.FETCH_VISITOR_STATS ->
                prependToLog("Fetched visitor stats from ${site!!.name}")
            else ->
                prependToLog("WooCommerce stats were updated from a " + event.causeOfChange)
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
