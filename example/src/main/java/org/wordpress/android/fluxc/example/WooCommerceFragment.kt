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
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_SINGLE_ORDER
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_ORDER_STATS
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_ORDER_STATS_FORCED
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_VISITOR_STATS
import org.wordpress.android.fluxc.example.CustomStatsDialog.WCOrderStatsAction.FETCH_CUSTOM_VISITOR_STATS_FORCED
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchSingleOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersSearched
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.OnApiVersionFetched
import org.wordpress.android.fluxc.store.WooCommerceStore.OnWCSiteSettingsChanged
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class WooCommerceFragment : Fragment(), CustomStatsDialog.Listener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var wcOrderStore: WCOrderStore
    @Inject internal lateinit var wcProductStore: WCProductStore
    @Inject internal lateinit var wcStatsStore: WCStatsStore

    private var pendingNotesOrderModel: WCOrderModel? = null
    private var pendingFetchOrdersFilter: List<String>? = null
    private var pendingFetchCompletedOrders: Boolean = false
    private var pendingFetchSingleOrderRemoteId: Long? = null
    private var pendingFetchSingleProductRemoteId: Long? = null
    private var pendingShipmentTrackingOrder: WCOrderModel? = null

    private var visitorStatsStartDate: String? = null
    private var visitorStatsEndDate: String? = null
    private var visitorStatsGranularity: String? = null

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

        fetch_settings.setOnClickListener {
            getFirstWCSite()?.let {
                dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteSettingsAction(it))
            } ?: showNoWCSitesToast()
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
                        "Enter a single order status to filter by:"
                ) { editText ->

                    // only use the status for filtering if it's not empty
                    val statusFilter = editText.text.toString().trim().takeIf { it.isNotEmpty() }
                    statusFilter?.let { filter ->
                        prependToLog("Submitting request to fetch a count of $filter orders")
                        val payload = FetchOrdersCountPayload(site, filter)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersCountAction(payload))
                    } ?: run {
                        prependToLog("No valid filters defined! Required for this request")
                    }
                }
            }
        }

        search_orders.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter a search query:"
                ) { editText ->

                    val searchQuery = editText.text.toString().trim().takeIf { it.isNotEmpty() }
                    searchQuery?.let {
                        prependToLog("Submitting request to search orders matching $it")
                        val payload = SearchOrdersPayload(site, searchQuery, 0)
                        dispatcher.dispatch(WCOrderActionBuilder.newSearchOrdersAction(payload))
                    } ?: prependToLog("No search query entered")
                }
            } ?: showNoWCSitesToast()
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
                val payload = FetchOrdersPayload(site, loadMore = false, statusFilter = "completed")
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            }
        }

        fetch_order_status_options.setOnClickListener {
            getFirstWCSite()?.let { site ->
                dispatcher.dispatch(WCOrderActionBuilder
                        .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(site)))
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

        fetch_single_product.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(activity, "Enter the remoteProductId of product to fetch:") { editText ->
                    pendingFetchSingleProductRemoteId = editText.text.toString().toLongOrNull()
                    pendingFetchSingleProductRemoteId?.let { id ->
                        prependToLog("Submitting request to fetch product by remoteProductID $id")
                        val payload = FetchSingleProductPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
                    } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                }
            } ?: showNoWCSitesToast()
        }

        fetch_product_variations.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteProductId of product to fetch variations:"
                ) { editText ->
                    val remoteProductId = editText.text.toString().toLongOrNull()
                    remoteProductId?.let { id ->
                        prependToLog("Submitting request to fetch product variations by remoteProductID $id")
                        val payload = FetchProductVariationsPayload(site, id)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
                    } ?: prependToLog("No valid remoteProductId defined...doing nothing")
                }
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
                val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchOrderStatsAction(payload))
            } ?: showNoWCSitesToast()
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
            } ?: showNoWCSitesToast()
        }

        fetch_top_earners_stats_forced.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchTopEarnersStatsPayload(it, StatsGranularity.DAYS, 10, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchTopEarnersStatsAction(payload))
            } ?: showNoWCSitesToast()
        }

        fetch_shipment_trackings.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteOrderId to fetch shipment trackings:"
                ) { editText ->
                    editText.text.toString().toLongOrNull()?.let { remoteOrderId ->
                        wcOrderStore.getOrderByIdentifier(OrderIdentifier(site.id, remoteOrderId))?.let { order ->
                            prependToLog("Submitting request to fetch shipment trackings for " +
                                    "remoteOrderId: ${order.remoteOrderId}")
                            pendingShipmentTrackingOrder = order
                            val payload = FetchOrderShipmentTrackingsPayload(site, order)
                            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(payload))
                        } ?: prependToLog("No order found in the db for remoteOrderId: $remoteOrderId, " +
                                "please fetch orders first.")
                    } ?: prependToLog("No valid remoteOrderId submitted for search")
                }
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
    fun onWCSiteSettingsChanged(event: OnWCSiteSettingsChanged) {
        if (event.isError) {
            prependToLog("Error in onWCSiteSettingsChanged: ${event.error.type} - ${event.error.message}")
            return
        }

        with(event) {
            prependToLog("Updated site settings for ${site.name}:\n" +
                    wooCommerceStore.getSiteSettings(site).toString()
            )
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
                            prependToLog("printing the first 5 remoteOrderId's from result:")
                            val orders = wcOrderStore.getOrdersForSite(site)
                            orders.take(5).forEach { prependToLog("- remoteOrderId [${it.remoteOrderId}]") }
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
                    FETCH_ORDER_SHIPMENT_TRACKINGS -> {
                        pendingShipmentTrackingOrder?.let {
                            val trackings = wcOrderStore.getShipmentTrackingsForOrder(it)
                            prependToLog("[${trackings.size}] shipment trackings retrieved for " +
                                    "remoteOrderId: ${it.id}, and [${event.rowsAffected}] rows changed in the db")
                            pendingShipmentTrackingOrder = null
                        }
                    }
                    else -> prependToLog("Order store was updated from a " + event.causeOfChange)
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrdersSearched(event: OnOrdersSearched) {
        if (event.isError) {
            prependToLog("Error searching orders - error: " + event.error.type)
        } else {
            prependToLog("Found ${event.searchResults.size} orders matching ${event.searchQuery}")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        getFirstWCSite()?.let { site ->
            when (event.causeOfChange) {
                FETCH_SINGLE_PRODUCT -> {
                    pendingFetchSingleProductRemoteId?.let { remoteId ->
                        pendingFetchSingleProductRemoteId = null
                        val product = wcProductStore.getProductByRemoteId(site, remoteId)
                        product?.let {
                            prependToLog("Single product fetched successfully! ${it.name}")
                        } ?: prependToLog("WARNING: Fetched product not found in the local database!")
                    }
                }
                FETCH_PRODUCT_VARIATIONS -> {
                    prependToLog("Fetched ${event.rowsAffected} product variations")
                }
                else -> prependToLog("Product store was updated from a " + event.causeOfChange)
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        if (event.isError) {
            prependToLog("Error fetching order status options from the api: ${event.error.message}")
            return
        }

        val orderStatusOptions = getFirstWCSite()?.let {
            wcOrderStore.getOrderStatusOptionsForSite(it)
        }?.map { it.label }
        prependToLog("Fetched order status options from the api: $orderStatusOptions " +
                "- updated ${event.rowsAffected} in the db")
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)

    private fun getCustomStatsForSite() = getFirstWCSite()?.let {
        wcStatsStore.getCustomStatsForSite(it)
    }

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
        } ?: showNoWCSitesToast()
    }
}
