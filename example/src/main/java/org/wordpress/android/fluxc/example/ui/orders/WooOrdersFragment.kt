package org.wordpress.android.fluxc.example.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_woo_orders.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.example.R.layout
import org.wordpress.android.fluxc.example.WCAddOrderShipmentTrackingDialog
import org.wordpress.android.fluxc.example.WCOrderListActivity
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersSearched
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class WooOrdersFragment : StoreSelectingFragment(), WCAddOrderShipmentTrackingDialog.Listener {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var wcOrderStore: WCOrderStore
    @Inject lateinit var wooCommerceStore: WooCommerceStore
    @Inject lateinit var orderUpdateStore: OrderUpdateStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var pendingFetchOrdersFilter: List<String>? = null
    private var pendingFetchCompletedOrders: Boolean = false
    private var pendingOpenAddShipmentTracking: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(layout.fragment_woo_orders, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetch_orders.setOnClickListener {
            selectedSite?.let {
                val payload = FetchOrdersPayload(it, loadMore = false)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            }
        }

        fetch_order_list.setOnClickListener {
            selectedSite?.let {
                val intent = Intent(activity, WCOrderListActivity::class.java)
                startActivity(intent)
            }
        }

        fetch_orders_count.setOnClickListener {
            selectedSite?.let { site ->
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
            selectedSite?.let { site ->
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
            selectedSite?.let { site ->
                showSingleLineDialog(activity, "Enter the remoteOrderId of order to fetch:") { editText ->
                    val enteredRemoteId = editText.text.toString().toLongOrNull()
                    coroutineScope.launch {
                        enteredRemoteId?.let { id ->
                            prependToLog("Submitting request to fetch order by remoteOrderID: $enteredRemoteId")
                            wcOrderStore.fetchSingleOrder(site, id).takeUnless { it.isError }?.let {
                                prependToLog("Single order fetched successfully!")
                            } ?: prependToLog("WARNING: Fetched order not found in the local database!")
                        } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                    }
                }
            }
        }

        fetch_has_orders.setOnClickListener {
            selectedSite?.let {
                val payload = FetchHasOrdersPayload(it)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchHasOrdersAction(payload))
            }
        }

        fetch_orders_by_status.setOnClickListener {
            selectedSite?.let { site ->
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
            selectedSite?.let { site ->
                prependToLog("Submitting request to fetch only completed orders from the api")
                pendingFetchCompletedOrders = true
                val payload = FetchOrdersPayload(site, loadMore = false, statusFilter = "completed")
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            }
        }

        fetch_order_status_options.setOnClickListener {
            selectedSite?.let { site ->
                dispatcher.dispatch(WCOrderActionBuilder
                        .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(site)))
            }
        }

        fetch_order_notes.setOnClickListener {
            selectedSite?.let { site ->
                getFirstWCOrder()?.let { order ->
                    coroutineScope.launch {
                        wcOrderStore.fetchOrderNotes(order.id, order.remoteOrderId, site).takeUnless { it.isError }
                            ?.let {
                                prependToLog(
                                    "Fetched order(${order.remoteOrderId}) notes. " +
                                        "${it.rowsAffected} notes inserted into database."
                                )
                            } ?: prependToLog("Fetching notes failed.")
                    }
                }
            }
        }

        post_order_note.setOnClickListener {
            selectedSite?.let { site ->
                getFirstWCOrder()?.let { order ->
                    showSingleLineDialog(activity, "Enter note") { editText ->
                        val newNote = WCOrderNoteModel().apply {
                            note = editText.text.toString()
                        }
                        coroutineScope.launch {
                            val payload = PostOrderNotePayload(order.id, order.remoteOrderId, site, newNote)
                            val onOrderChanged = wcOrderStore.postOrderNote(payload)
                            if (!onOrderChanged.isError) {
                                prependToLog(
                                        "Posted note to the api for order ${order.remoteOrderId}"
                                )
                            } else {
                                prependToLog(
                                        "Posting note FAILED for order ${order.remoteOrderId}"
                                )
                            }
                        }
                    }
                }
            }
        }

        update_latest_order_status.setOnClickListener {
            selectedSite?.let { site ->
                wcOrderStore.getOrdersForSite(site).firstOrNull()?.let { order ->
                    showSingleLineDialog(activity, "Enter new order status") { editText ->
                        val status = editText.text.toString()
                        coroutineScope.launch {
                            wcOrderStore.updateOrderStatus(LocalId(order.id), site, status)
                                    .collect {
                                        if (it.event.isError) {
                                            prependToLog("FAILED: Update order status for ${order.remoteOrderId} " +
                                                    "to $status - ${it::class.simpleName}")
                                        } else {
                                            prependToLog("Updated order status for ${order.remoteOrderId} " +
                                                    "to $status - ${it::class.simpleName}")
                                        }
                                    }
                        }
                    }
                } ?: showNoOrdersToast(site)
            }
        }

        update_latest_order_notes.setOnClickListener {
            selectedSite?.let { site ->
                wcOrderStore.getOrdersForSite(site).firstOrNull()?.let { order ->
                    showSingleLineDialog(activity, "Enter new customer note") { editText ->
                        val status = editText.text.toString()
                        coroutineScope.launch {
                            orderUpdateStore.updateCustomerOrderNote(
                                    LocalId(order.id),
                                    site,
                                    status
                            ).collect {
                                prependToLog(it::class.simpleName.toString())
                            }
                        }
                    }
                } ?: showNoOrdersToast(site)
            }
        }

        fetch_shipment_trackings.setOnClickListener {
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteOrderId to fetch shipment trackings:"
                ) { editText ->
                    editText.text.toString().toLongOrNull()?.let { remoteOrderId ->
                        wcOrderStore.getOrderByIdentifier(OrderIdentifier(site.id, remoteOrderId))?.let { order ->
                            prependToLog("Submitting request to fetch shipment trackings for " +
                                    "remoteOrderId: ${order.remoteOrderId}"
                            )
                            coroutineScope.launch {
                                val result = wcOrderStore
                                        .fetchOrderShipmentTrackings(order.id, order.remoteOrderId, site)

                                result.takeUnless { it.isError }?.let {
                                    prependToLog("ShipmentTrackings fetched successfuly")
                                } ?: prependToLog("Fetching ShipmentTrackings failed")

                                val trackings = wcOrderStore.getShipmentTrackingsForOrder(site, it.id)
                                trackings.forEach { tracking ->
                                    prependToLog("- shipped:${tracking.dateShipped}: ${tracking.trackingNumber}")
                                }
                                prependToLog("[${trackings.size}] shipment trackings retrieved for " +
                                        "remoteOrderId: ${order.remoteOrderId}, and [${result.rowsAffected}] rows " +
                                        "changed in the db:")
                            }
                        } ?: prependToLog(
                                "No order found in the db for remoteOrderId: $remoteOrderId, " +
                                        "please fetch orders first."
                        )
                    } ?: prependToLog("No valid remoteOrderId submitted for search")
                }
            }
        }

        add_shipment_tracking.setOnClickListener {
            selectedSite?.let { site ->
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
            selectedSite?.let { site ->
                showSingleLineDialog(
                        activity,
                        "Enter the remoteOrderId to delete the first shipment tracking for:"
                ) { editText ->
                    editText.text.toString().toLongOrNull()?.let { remoteOrderId ->
                        wcOrderStore.getOrderByIdentifier(OrderIdentifier(site.id, remoteOrderId))?.let { order ->
                            prependToLog("Submitting request to fetch shipment trackings for " +
                                    "remoteOrderId: ${order.remoteOrderId}")

                            wcOrderStore.getShipmentTrackingsForOrder(site, order.id).firstOrNull()?.let { tracking ->
                                coroutineScope.launch {
                                    val onOrderChanged = wcOrderStore.deleteOrderShipmentTracking(
                                            DeleteOrderShipmentTrackingPayload(
                                                    site, order.id, order.remoteOrderId, tracking
                                            )
                                    )
                                    onOrderChanged.takeUnless { it.isError }?.let {
                                        prependToLog(
                                                "Shipment tracking deleted successfully! " +
                                                        "[${onOrderChanged.rowsAffected}] db rows affected."
                                        )
                                    } ?: prependToLog("Shipment tracking deletion FAILED!")
                                }
                            } ?: prependToLog("No shipment trackings in the db for remoteOrderId: $remoteOrderId, " +
                                    "please fetch records first for this order")
                        } ?: prependToLog("No order found in the db for remoteOrderId: $remoteOrderId, " +
                                "please fetch orders first.")
                    }
                }
            }
        }

        fetch_shipment_providers.setOnClickListener {
            selectedSite?.let { site ->
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
            selectedSite?.let { site ->
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

        selectedSite?.let { site ->
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
                            selectedSite?.let { site ->
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
                    FETCH_HAS_ORDERS -> {
                        val hasOrders = event.rowsAffected > 0
                        prependToLog("Store has orders: $hasOrders")
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

        val orderStatusOptions = selectedSite?.let {
            wcOrderStore.getOrderStatusOptionsForSite(it)
        }?.map { it.label to it.statusCount }?.toMap()
        prependToLog("Fetched order status options from the api: $orderStatusOptions " +
                "- updated ${event.rowsAffected} in the db")
    }

    private fun getFirstWCOrder() = selectedSite?.let {
        wcOrderStore.getOrdersForSite(it).getOrNull(0)
    }

    private fun showNoOrdersToast(site: SiteModel) {
        ToastUtils.showToast(activity, "No orders found for site: " + site.name)
    }

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
        coroutineScope.launch {
            val onOrderChanged = wcOrderStore.addOrderShipmentTracking(
                    AddOrderShipmentTrackingPayload(
                            site,
                            order.id,
                            order.remoteOrderId,
                            tracking,
                            isCustomProvider
                    )
            )
            if (!onOrderChanged.isError) {
                getFirstWCOrder()?.let { order ->
                    val trackingCount = wcOrderStore.getShipmentTrackingsForOrder(site, order.id).size
                    prependToLog(
                            "Shipment tracking added successfully to remoteOrderId [${order.remoteOrderId}]! " +
                                    "[$trackingCount] tracking records now exist for this order in the db."
                    )
                }
            } else {
                prependToLog("Adding shipment tracking for remoteOrderId [${order.remoteOrderId}] FAILED!")
            }
        }
    }
}
