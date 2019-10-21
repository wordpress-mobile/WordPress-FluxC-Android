package org.wordpress.android.fluxc.example.ui.orders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_woo_orders.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_SINGLE_ORDER
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.WCAddOrderShipmentTrackingDialog
import org.wordpress.android.fluxc.example.WCOrderListActivity
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchSingleOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersSearched
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class WooOrdersFragment : Fragment(), WCAddOrderShipmentTrackingDialog.Listener {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var wcOrderStore: WCOrderStore
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore

    private var pendingNotesOrderModel: WCOrderModel? = null
    private var pendingFetchOrdersFilter: List<String>? = null
    private var pendingFetchCompletedOrders: Boolean = false
    private var pendingFetchSingleOrderRemoteId: Long? = null
    private var pendingShipmentTrackingOrder: WCOrderModel? = null
    private var pendingDeleteShipmentTracking: WCOrderShipmentTrackingModel? = null
    private var pendingAddShipmentTracking: WCOrderShipmentTrackingModel? = null
    private var pendingOpenAddShipmentTracking: Boolean = false
    private var pendingAddShipmentTrackingRemoteOrderID: Long? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_orders.setOnClickListener {
            getFirstWCSite()?.let {
                val payload = FetchOrdersPayload(it, loadMore = false)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            }
        }

        fetch_order_list.setOnClickListener {
            getFirstWCSite()?.let {
                val intent = Intent(activity, WCOrderListActivity::class.java)
                startActivity(intent)
            }
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
            }
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
            }
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
            }
        }

        add_shipment_tracking.setOnClickListener {
            getFirstWCSite()?.let { site ->
                getFirstWCOrder()?.let { order ->
                    val providers = wcOrderStore.getShipmentProvidersForSite(site)
                    if (providers.isNullOrEmpty()) {
                        // Fetch providers for order
                        pendingOpenAddShipmentTracking = true
                        val payload = FetchOrderShipmentProvidersPayload(site, order)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
                    } else {
                        val providerNames = mutableListOf<String>()
                        providers.forEach { providerNames.add(it.carrierName) }
                        showAddTrackingDialog(site, order, providerNames)
                    }
                }
            }
        }

        delete_shipment_tracking.setOnClickListener {
            getFirstWCSite()?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteOrderId to delete the first shipment tracking for:"
                ) { editText ->
                    editText.text.toString().toLongOrNull()?.let { remoteOrderId ->
                        wcOrderStore.getOrderByIdentifier(OrderIdentifier(site.id, remoteOrderId))?.let { order ->
                            prependToLog("Submitting request to fetch shipment trackings for " +
                                    "remoteOrderId: ${order.remoteOrderId}")

                            wcOrderStore.getShipmentTrackingsForOrder(order).firstOrNull()?.let { tracking ->
                                pendingDeleteShipmentTracking = tracking
                                val payload = DeleteOrderShipmentTrackingPayload(site, order, tracking)
                                dispatcher.dispatch(WCOrderActionBuilder.newDeleteOrderShipmentTrackingAction(payload))
                            } ?: prependToLog("No shipment trackings in the db for remoteOrderId: $remoteOrderId, " +
                                    "please fetch records first for this order")
                        } ?: prependToLog("No order found in the db for remoteOrderId: $remoteOrderId, " +
                                "please fetch orders first.")
                    }
                }
            }
        }

        fetch_shipment_providers.setOnClickListener {
            getFirstWCSite()?.let { site ->
                // Just use the first order, the shipment trackings api oddly requires an order_id for fetching
                // a list of providers, even though the providers are not order specific.
                getFirstWCOrder()?.let { order ->
                    prependToLog("Fetching a list of providers from the API")

                    val payload = FetchOrderShipmentProvidersPayload(site, order)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
                } ?: prependToLog("No orders found in db to use as seed. Fetch orders first.")
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
    fun onOrderShipmentProviderChanged(event: OnOrderShipmentProvidersChanged) {
        if (event.isError) {
            prependToLog("Error fetching shipment providers - error: " + event.error.type)
        } else {
            getFirstWCSite()?.let { site ->
                if (pendingOpenAddShipmentTracking) {
                    pendingOpenAddShipmentTracking = false
                    getFirstWCOrder()?.let { order ->
                        val providers = mutableListOf<String>()
                        wcOrderStore.getShipmentProvidersForSite(site).forEach { providers.add(it.carrierName) }
                        showAddTrackingDialog(site, order, providers)
                    }
                } else {
                    wcOrderStore.getShipmentProvidersForSite(site).forEach { provider ->
                        prependToLog(" - ${provider.carrierName}")
                    }
                    prependToLog("[${event.rowsAffected}] shipment providers fetched successfully!")
                }
            }
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
                            trackings.forEach { tracking ->
                                prependToLog("- shipped:${tracking.dateShipped}: ${tracking.trackingNumber}")
                            }
                            prependToLog("[${trackings.size}] shipment trackings retrieved for " +
                                    "remoteOrderId: ${it.remoteOrderId}, and [${event.rowsAffected}] rows changed in " +
                                    "the db:")
                            pendingShipmentTrackingOrder = null
                        }
                    }
                    ADD_ORDER_SHIPMENT_TRACKING -> {
                        pendingAddShipmentTracking?.let {
                            getFirstWCOrder()?.let { order ->
                                val trackingCount = wcOrderStore.getShipmentTrackingsForOrder(order).size
                                prependToLog("Shipment tracking added successfully to " +
                                        "remoteOrderId [$pendingAddShipmentTrackingRemoteOrderID]! " +
                                        "[$trackingCount] tracking records now exist for this order in the db.")
                            }
                            pendingAddShipmentTracking = null
                            pendingAddShipmentTrackingRemoteOrderID = null
                        }
                    }
                    DELETE_ORDER_SHIPMENT_TRACKING -> {
                        pendingDeleteShipmentTracking?.let {
                            prependToLog("Shipment tracking deleted successfully! [${event.rowsAffected}] db rows " +
                                    "affected.")
                            pendingDeleteShipmentTracking = null
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
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        if (event.isError) {
            prependToLog("Error fetching order status options from the api: ${event.error.message}")
            return
        }

        val orderStatusOptions = getFirstWCSite()?.let {
            wcOrderStore.getOrderStatusOptionsForSite(it)
        }?.map { it.label to it.statusCount }?.toMap()
        prependToLog("Fetched order status options from the api: $orderStatusOptions " +
                "- updated ${event.rowsAffected} in the db")
    }

    private fun getFirstWCOrder() = getFirstWCSite()?.let {
        wcOrderStore.getOrdersForSite(it).getOrNull(0)
    }

    private fun showNoOrdersToast(site: SiteModel) {
        ToastUtils.showToast(activity, "No orders found for site: " + site.name)
    }

    private fun getFirstWCSite() = wooCommerceStore.getWooCommerceSites().getOrNull(0)

    private fun showAddTrackingDialog(site: SiteModel, order: WCOrderModel, providers: List<String>) {
        fragmentManager?.let { fm ->
            val dialog = WCAddOrderShipmentTrackingDialog.newInstance(
                    this,
                    site,
                    order,
                    providers)
            dialog.show(fm, "WCAddOrderShipmentTrackingDialog")
        }
    }

    override fun onTrackingSubmitted(
        site: SiteModel,
        order: WCOrderModel,
        tracking: WCOrderShipmentTrackingModel,
        isCustomProvider: Boolean
    ) {
        pendingAddShipmentTracking = tracking
        pendingAddShipmentTrackingRemoteOrderID = order.remoteOrderId
        val payload = AddOrderShipmentTrackingPayload(site, order, tracking, isCustomProvider)
        dispatcher.dispatch(WCOrderActionBuilder.newAddOrderShipmentTrackingAction(payload))
    }
}
