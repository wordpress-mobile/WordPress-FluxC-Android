package org.wordpress.android.fluxc.example.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.example.WCAddOrderShipmentTrackingDialog
import org.wordpress.android.fluxc.example.WCOrderListActivity
import org.wordpress.android.fluxc.example.databinding.FragmentWooOrdersBinding
import org.wordpress.android.fluxc.example.prependToLog
import org.wordpress.android.fluxc.example.replaceFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.example.ui.metadata.CustomFieldsFragment
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.AddressType
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.AddressType.BILLING
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment.AddressType.SHIPPING
import org.wordpress.android.fluxc.example.utils.showSingleLineDialog
import org.wordpress.android.fluxc.example.utils.showTwoButtonsDialog
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.OrderAttributionInfo
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.metadata.MetaDataParentItemType
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult.Failure
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult.Success
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersSearched
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.putIfNotNull
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject
import kotlin.coroutines.resume

private const val NUMBER_OF_FIRST_ORDERS_TO_PRINT = 5

@Suppress("LargeClass")
class WooOrdersFragment : StoreSelectingFragment(), WCAddOrderShipmentTrackingDialog.Listener {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var wcOrderStore: WCOrderStore
    @Inject lateinit var wooCommerceStore: WooCommerceStore
    @Inject lateinit var orderUpdateStore: OrderUpdateStore

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var pendingFetchOrdersFilter: List<String>? = null
    private var pendingFetchCompletedOrders: Boolean = false
    private var pendingOpenAddShipmentTracking: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWooOrdersBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentWooOrdersBinding.bind(view)) {
            fetchOrders.setOnClickListener {
                selectedSite?.let {
                    val payload = FetchOrdersPayload(it, loadMore = false)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
                }
            }

            fetchOrderList.setOnClickListener {
                selectedSite?.let {
                    val intent = Intent(activity, WCOrderListActivity::class.java)
                    startActivity(intent)
                }
            }

            fetchOrdersCount.setOnClickListener {
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

            searchOrders.setOnClickListener {
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

            fetchSingleOrder.setOnClickListener {
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

            fetchHasOrders.setOnClickListener {
                selectedSite?.let {
                    coroutineScope.launch {
                        when (val result = wcOrderStore.fetchHasOrders(it, null)) {
                            is Failure -> {
                                prependToLog("Fetching hasOrders failed.")
                            }
                            is Success -> {
                                prependToLog("Has orders: ${result.hasOrders}")
                            }
                        }
                    }
                }
            }

            fetchOrdersByStatus.setOnClickListener {
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

            fetchOrdersByStatusApi.setOnClickListener {
                selectedSite?.let { site ->
                    prependToLog("Submitting request to fetch only completed orders from the api")
                    pendingFetchCompletedOrders = true
                    val payload = FetchOrdersPayload(site, loadMore = false, statusFilter = "completed")
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
                }
            }

            fetchOrderStatusOptions.setOnClickListener {
                selectedSite?.let { site ->
                    dispatcher.dispatch(
                        WCOrderActionBuilder
                            .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(site))
                    )
                }
            }

            fetchOrderNotes.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        getFirstWCOrder()?.let { order ->
                            val notesCountBeforeRequest = wcOrderStore.getOrderNotesForOrder(site, order.orderId).size
                            coroutineScope.launch {
                                wcOrderStore.fetchOrderNotes(site, order.orderId)
                                    .takeUnless { it.isError }
                                    ?.let {
                                        val notesCountAfterRequest = wcOrderStore.getOrderNotesForOrder(
                                            site = site,
                                            orderId = order.orderId
                                        ).size
                                        prependToLog(
                                            "Fetched order(${order.orderId}) notes. " +
                                                "${notesCountAfterRequest - notesCountBeforeRequest} " +
                                                "notes inserted into database."
                                        )
                                    } ?: prependToLog("Fetching notes failed.")
                            }
                        }
                    }
                }
            }

            postOrderNote.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        getFirstWCOrder()?.let { order ->
                            showSingleLineDialog(activity, "Enter note") { editText ->
                                val newNote = editText.text.toString()
                                coroutineScope.launch {
                                    val onOrderChanged = wcOrderStore.postOrderNote(
                                        site = site,
                                        orderId = order.orderId,
                                        note = newNote,
                                        isCustomerNote = false
                                    )
                                    if (!onOrderChanged.isError) {
                                        prependToLog(
                                            "Posted note to the api for order ${order.orderId}"
                                        )
                                    } else {
                                        prependToLog(
                                            "Posting note FAILED for order ${order.orderId}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            updateLatestOrderStatus.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        wcOrderStore.getOrdersForSite(site).firstOrNull()?.let { order ->
                            showSingleLineDialog(activity, "Enter new order status") { editText ->
                                val status = editText.text.toString()
                                coroutineScope.launch {
                                    wcOrderStore.updateOrderStatus(order.orderId, site, WCOrderStatusModel(status))
                                        .collect {
                                            if (it.event.isError) {
                                                prependToLog(
                                                    "FAILED: Update order status for ${order.orderId} " +
                                                        "to $status - ${it::class.simpleName}"
                                                )
                                            } else {
                                                prependToLog(
                                                    "Updated order status for ${order.orderId} " +
                                                        "to $status - ${it::class.simpleName}"
                                                )
                                            }
                                        }
                                }
                            }
                        } ?: showNoOrdersToast(site)
                    }
                }
            }

            updateLatestOrderNotes.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        wcOrderStore.getOrdersForSite(site).firstOrNull()?.let { order ->
                            showSingleLineDialog(activity, "Enter new customer note") { editText ->
                                val status = editText.text.toString()
                                coroutineScope.launch {
                                    orderUpdateStore.updateCustomerOrderNote(
                                        order.orderId,
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
            }

            fetchShipmentTrackings.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(
                        activity,
                        "Enter the remoteOrderId to fetch shipment trackings:"
                    ) { editText ->
                        editText.text.toString().toLongOrNull()?.let { remoteOrderId ->
                            coroutineScope.launch {
                                val orderId = wcOrderStore.getOrderByIdAndSite(remoteOrderId, site)!!.orderId
                                val trackingsCountBeforeRequest =
                                    OrderSqlUtils.getShipmentTrackingsForOrder(site, orderId).size
                                wcOrderStore.getOrderByIdAndSite(remoteOrderId, site)?.let { order ->
                                    prependToLog(
                                        "Submitting request to fetch shipment trackings for " +
                                            "remoteOrderId: ${order.orderId}"
                                    )
                                    coroutineScope.launch {
                                        val result = wcOrderStore
                                            .fetchOrderShipmentTrackings(
                                                order.orderId, site
                                            )

                                        result.takeUnless { it.isError }?.let {
                                            prependToLog("ShipmentTrackings fetched successfuly")
                                        } ?: prependToLog("Fetching ShipmentTrackings failed")

                                        val trackings = wcOrderStore.getShipmentTrackingsForOrder(site, orderId)
                                        trackings.forEach { tracking ->
                                            prependToLog(
                                                "- shipped:${tracking.dateShipped}:" +
                                                    tracking.trackingNumber
                                            )
                                        }
                                        val trackingsCountAfterRequest =
                                            OrderSqlUtils.getShipmentTrackingsForOrder(site, orderId).size

                                        prependToLog(
                                            "[${trackings.size}] shipment trackings retrieved for " +
                                                "remoteOrderId: ${order.orderId}, and " +
                                                "[${trackingsCountAfterRequest - trackingsCountBeforeRequest}] rows " +
                                                "changed in the db:"
                                        )
                                    }
                                } ?: prependToLog(
                                    "No order found in the db for remoteOrderId: $remoteOrderId, " +
                                        "please fetch orders first."
                                )
                            }
                        }
                    }
                }
            }

            addShipmentTracking.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        getFirstWCOrder()?.let { order ->
                            val providers = wcOrderStore.getShipmentProvidersForSite(site)
                            if (providers.isNullOrEmpty()) {
                                // Fetch providers for order
                                pendingOpenAddShipmentTracking = true
                                fetchOrderShipmentProviders(site, order)
                            } else {
                                val providerNames = mutableListOf<String>()
                                providers.forEach { providerNames.add(it.carrierName) }
                                showAddTrackingDialog(site, order, providerNames)
                            }
                        }
                    }
                }
            }

            deleteShipmentTracking.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(
                        activity,
                        "Enter the remoteOrderId to delete the first shipment tracking for:"
                    ) { editText ->
                        editText.text.toString().toLongOrNull()?.let { remoteOrderId ->
                            coroutineScope.launch {
                                wcOrderStore.getOrderByIdAndSite(remoteOrderId, site)?.let { order ->
                                    prependToLog(
                                        "Submitting request to fetch shipment trackings for " +
                                            "remoteOrderId: ${order.orderId}"
                                    )

                                    wcOrderStore.getShipmentTrackingsForOrder(site, order.orderId).firstOrNull()
                                        ?.let { tracking ->
                                            coroutineScope.launch {
                                                val onOrderChanged = wcOrderStore.deleteOrderShipmentTracking(
                                                    DeleteOrderShipmentTrackingPayload(
                                                        site, order.orderId, tracking
                                                    )
                                                )
                                                onOrderChanged.takeUnless { it.isError }?.let {
                                                    prependToLog(
                                                        "Shipment tracking deleted successfully! "
                                                    )
                                                } ?: prependToLog("Shipment tracking deletion FAILED!")
                                            }
                                        } ?: prependToLog(
                                        "No shipment trackings in the db for remoteOrderId: $remoteOrderId, " +
                                            "please fetch records first for this order"
                                    )
                                } ?: prependToLog(
                                    "No order found in the db for remoteOrderId: $remoteOrderId, " +
                                        "please fetch orders first."
                                )
                            }
                        }
                    }
                }
            }

            fetchShipmentProviders.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        // Just use the first order, the shipment trackings api oddly requires an order_id for fetching
                        // a list of providers, even though the providers are not order specific.
                        getFirstWCOrder()?.let { order ->
                            prependToLog("Fetching a list of providers from the API")
                            fetchOrderShipmentProviders(site, order)
                        } ?: prependToLog("No orders found in db to use as seed. Fetch orders first.")
                    }
                }
            }

            updateLatestOrderBillingAddress.setOnClickListener {
                selectedSite?.let { site ->
                    coroutineScope.launch {
                        wcOrderStore.getOrdersForSite(site).firstOrNull()?.let { order ->
                            replaceFragment(AddressEditDialogFragment.newInstanceForEditing(order))
                        } ?: showNoOrdersToast(site)
                    }
                }
            }

            createSimplePayment.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(
                        activity,
                        "Enter the amount:"
                    ) { editText ->
                        coroutineScope.launch {
                            try {
                                val amount = editText.text.toString()
                                val result = orderUpdateStore.createSimplePayment(site, amount, true)
                                if (result.isError) {
                                    prependToLog("Creating simple payment failed.")
                                } else {
                                    prependToLog("Created simple payment with remote ID ${result.model?.orderId}.")
                                }
                            } catch (e: NumberFormatException) {
                                prependToLog("Invalid amount.")
                            }
                        }
                    }
                }
            }

            updateSimplePayment.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(
                        activity,
                        "Enter the remote order id (order must already be fetched):"
                    ) { remoteIdEditText ->
                        showSingleLineDialog(
                            activity,
                            "Enter the amount:"
                        ) { amountEditText ->
                            showSingleLineDialog(
                                activity,
                                "Enter the customer note:"
                            ) { customerNoteEditText ->
                                val remoteId = remoteIdEditText.text.toString().toLong()
                                val amount = amountEditText.text.toString()
                                val customerNote = customerNoteEditText.text.toString()
                                coroutineScope.launch {
                                    // pre-5.9 versions of WooCommerce fail w/o billing email so we pass one here
                                    orderUpdateStore.updateSimplePayment(
                                        site,
                                        remoteId,
                                        amount,
                                        customerNote = customerNote,
                                        billingEmail = "example@example.com",
                                        isTaxable = true
                                    ).collect { result ->
                                        when (result) {
                                            is OptimisticUpdateResult -> {
                                                if (result.event.isError) {
                                                    prependToLog("Optimistic simple payment update failed.")
                                                } else {
                                                    prependToLog("Optimistic simple payment update succeeded.")
                                                }
                                            }
                                            is RemoteUpdateResult -> {
                                                if (result.event.isError) {
                                                    prependToLog("Remote simple payment update failed.")
                                                } else {
                                                    prependToLog("Remote simple payment update succeeded.")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            createOrder.setOnClickListener {
                selectedSite?.let { site ->
                    lifecycleScope.launch {
                        val products = showSingleLineDialog(
                            activity = requireActivity(),
                            message = "Please type a comma separated list of product IDs",
                            isNumeric = false
                        )?.split(",")?.map { it.toLongOrNull() }

                        if (products == null || products.any { it == null }) {
                            prependToLog("Error while parsing the entered product IDs")
                            return@launch
                        }

                        val customerNote = showSingleLineDialog(
                            activity = requireActivity(),
                            message = "Please enter a customer note?",
                            isNumeric = false
                        )

                        val shippingAddress = showAddressDialog(addressType = SHIPPING) as OrderAddress.Shipping
                        val billingAddress = showAddressDialog(addressType = BILLING) as OrderAddress.Billing

                        val status = WCOrderStatusModel(CoreOrderStatus.PROCESSING.value)

                        val result = orderUpdateStore.createOrder(
                            site,
                            UpdateOrderRequest(
                                status = status,
                                lineItems = products.map {
                                    buildMap {
                                        putIfNotNull("product_id" to it)
                                        put("quantity", 1f)
                                    }
                                },
                                shippingAddress = shippingAddress,
                                billingAddress = billingAddress,
                                customerNote = customerNote
                            ),
                            attributionSourceType = "fluxc-example-app"
                        )
                        if (result.isError) {
                            prependToLog("Order creation failed, error ${result.error.type} ${result.error.message}")
                        } else {
                            prependToLog("Created order with id ${result.model!!.orderId}")
                        }
                    }
                }
            }

            deleteOrder.setOnClickListener {
                selectedSite?.let { site ->
                    lifecycleScope.launch {
                        val orderId = showSingleLineDialog(
                            activity = requireActivity(),
                            message = "Please enter the order id",
                            isNumeric = true
                        )?.toLongOrNull()
                        if (orderId == null) {
                            prependToLog("Please enter a valid order id")
                            return@launch
                        }

                        val shouldTrash = showTwoButtonsDialog(
                            activity = requireActivity(),
                            message = "Do you want to move the order to trash?"
                        )

                        val result = orderUpdateStore.deleteOrder(site, orderId, shouldTrash)

                        when {
                            result.isError -> {
                                prependToLog(
                                    "Deleting order failed, " +
                                    "error ${result.error.type} ${result.error.message}"
                                )
                            }
                            shouldTrash -> {
                                prependToLog("Order $orderId has been moved to trash")
                            }
                            else -> {
                                prependToLog("Order $orderId has been deleted succesfully")
                            }
                        }
                    }
                }
            }

            fetchOrderAttribution.setOnClickListener {
                selectedSite?.let { site ->
                    showSingleLineDialog(activity, "Enter the remoteOrderId of order to fetch:") { editText ->
                        val enteredRemoteId = editText.text.toString().toLongOrNull()
                        coroutineScope.launch {
                            enteredRemoteId?.let { id ->
                                prependToLog("Submitting request to fetch order by remoteOrderID: $enteredRemoteId")
                                wcOrderStore.fetchSingleOrder(site, id).takeUnless { it.isError }?.let {
                                    val attributionInfo = OrderAttributionInfo(
                                        metadata = wcOrderStore.getOrderMetadata(enteredRemoteId, site)
                                    )
                                    prependToLog("Order Attribution Information:\n$attributionInfo}")
                                } ?: prependToLog("Fetching Order Failed")
                            } ?: prependToLog("No valid remoteOrderId defined...doing nothing")
                        }
                    }
                }
            }

            customFields.setOnClickListener {
                selectedSite?.let { site ->
                    lifecycleScope.launch {
                        val orderId = showSingleLineDialog(
                            activity = requireActivity(),
                            message = "Please enter the order id",
                            isNumeric = true
                        )?.toLongOrNull() ?: return@launch

                        replaceFragment(
                            CustomFieldsFragment.newInstance(
                                siteId = site.localId(),
                                parentItemId = orderId,
                                parentItemType = MetaDataParentItemType.ORDER
                            )
                        )
                    }
                }
            }
        }
    }

    private fun fetchOrderShipmentProviders(
        site: SiteModel,
        order: OrderEntity
    ) {
        coroutineScope.launch {
            val payload = FetchOrderShipmentProvidersPayload(site, order)
            val response = wcOrderStore.fetchOrderShipmentProviders(payload)
            if (response.isError) {
                prependToLog("Error fetching shipment providers - error: " + response.error.type)
            } else {
                selectedSite?.let { site ->
                    if (pendingOpenAddShipmentTracking) {
                        pendingOpenAddShipmentTracking = false
                        getFirstWCOrder()?.let { order ->
                            val providers = mutableListOf<String>()
                            wcOrderStore.getShipmentProvidersForSite(site)
                                    .forEach { providers.add(it.carrierName) }
                            showAddTrackingDialog(site, order, providers)
                        }
                    } else {
                        wcOrderStore.getShipmentProvidersForSite(site).forEach { provider ->
                            prependToLog(" - ${provider.carrierName}")
                        }
                        prependToLog("[${response.rowsAffected}] shipment providers fetched successfully!")
                    }
                }
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

    @Suppress("unused", "ComplexMethod", "SpreadOperator")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            prependToLog("Error from " + event.causeOfChange + " - error: " + event.error.type)
            return
        }

        selectedSite?.let { site ->
            coroutineScope.launch {
                wcOrderStore.getOrdersForSite(site).let { orderList ->
                    if (orderList.isEmpty()) {
                        prependToLog("No orders were stored for site " + site.name + " =(")
                        return@launch
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
                                        prependToLog(
                                            "Fetched ${orders[status]?.count() ?: 0} orders for status [$status]"
                                        )
                                    }
                                    pendingFetchOrdersFilter = null
                                }
                            } ?: if (pendingFetchCompletedOrders) {
                                pendingFetchCompletedOrders = false
                                val completedOrders = wcOrderStore.getOrdersForSite(site, "completed")
                                prependToLog("Fetched ${completedOrders.size} completed orders from ${site.name}")
                            } else {
                                prependToLog(
                                    "printing the first $NUMBER_OF_FIRST_ORDERS_TO_PRINT remoteOrderId's from result:"
                                )
                                val orders = wcOrderStore.getOrdersForSite(site)
                                orders.take(NUMBER_OF_FIRST_ORDERS_TO_PRINT).forEach {
                                    prependToLog("- remoteOrderId [${it.orderId}]")
                                }
                            }
                        }
                        FETCH_ORDERS_COUNT -> {
                            val append = if (event.canLoadMore) "+" else ""

                            val statusFilter = event.statusFilter
                            if (statusFilter != null) {
                                val ordersCount = wcOrderStore.getOrdersForSite(site, statusFilter).count()
                                prependToLog("Count of ${event.statusFilter} orders: $ordersCount$append")
                            } else {
                                val ordersCount = wcOrderStore.getOrdersForSite(site).count()
                                prependToLog("Count of all orders: $ordersCount$append")
                            }
                        }
                        else -> prependToLog("Order store was updated from a " + event.causeOfChange)
                    }
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
        prependToLog(
            "Fetched order status options from the api: $orderStatusOptions " +
                "- updated ${event.rowsAffected} in the db"
        )
    }

    private suspend fun getFirstWCOrder() = selectedSite?.let {
        wcOrderStore.getOrdersForSite(it).getOrNull(0)
    }

    private fun showNoOrdersToast(site: SiteModel) {
        ToastUtils.showToast(activity, "No orders found for site: " + site.name)
    }

    private fun showAddTrackingDialog(site: SiteModel, order: OrderEntity, providers: List<String>) {
        fragmentManager?.let { fm ->
            val dialog = WCAddOrderShipmentTrackingDialog.newInstance(
                    this,
                    site,
                    order,
                    providers
            )
            dialog.show(fm, "WCAddOrderShipmentTrackingDialog")
        }
    }

    override fun onTrackingSubmitted(
        site: SiteModel,
        order: OrderEntity,
        tracking: WCOrderShipmentTrackingModel,
        isCustomProvider: Boolean
    ) {
        coroutineScope.launch {
            val onOrderChanged = wcOrderStore.addOrderShipmentTracking(
                    AddOrderShipmentTrackingPayload(
                            site,
                            order.orderId,
                            tracking,
                            isCustomProvider
                    )
            )
            if (!onOrderChanged.isError) {
                getFirstWCOrder()?.let { order ->
                    val trackingCount = wcOrderStore.getShipmentTrackingsForOrder(site, order.orderId).size
                    prependToLog(
                            "Shipment tracking added successfully to remoteOrderId [${order.orderId}]! " +
                                    "[$trackingCount] tracking records now exist for this order in the db."
                    )
                }
            } else {
                prependToLog("Adding shipment tracking for remoteOrderId [${order.orderId}] FAILED!")
            }
        }
    }

    private suspend fun showAddressDialog(addressType: AddressType): OrderAddress? {
        return suspendCancellableCoroutine { continuation ->
            replaceFragment(AddressEditDialogFragment.newInstanceForCreation(addressType) {
                continuation.resume(it)
            })
        }
    }
}
