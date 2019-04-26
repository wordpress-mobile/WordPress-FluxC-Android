package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCOrderStore @Inject constructor(dispatcher: Dispatcher, private val wcOrderRestClient: OrderRestClient) :
        Store(dispatcher) {
    companion object {
        const val NUM_ORDERS_PER_FETCH = 25
        const val DEFAULT_ORDER_STATUS = "any"
    }

    class FetchOrdersPayload(
        var site: SiteModel,
        var statusFilter: String? = null,
        var loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchOrderListPayload(
        val listDescriptor: WCOrderListDescriptor,
        val offset: Long
    ) : Payload<BaseNetworkError>()

    class FetchOrdersByIdsPayload(
        val site: SiteModel,
        val remoteIds: List<RemoteId>
    ) : Payload<BaseNetworkError>()

    class FetchOrdersResponsePayload(
        var site: SiteModel,
        var orders: List<WCOrderModel> = emptyList(),
        var statusFilter: String? = null,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) { this.error = error }
    }

    data class WCOrderModelId(val id: Long)

    class FetchOrderListResponsePayload(
        val listDescriptor: WCOrderListDescriptor,
        var orderIds: List<WCOrderModelId> = emptyList(),
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, listDescriptor: WCOrderListDescriptor) : this(listDescriptor) {
            this.error = error
        }
    }

    class FetchOrdersByIdsResponsePayload(
        val site: SiteModel,
        var orders: List<WCOrderModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site = site) {
            this.error = error
        }
    }

    class SearchOrdersPayload(
        var site: SiteModel,
        var searchQuery: String,
        var offset: Int
    ) : Payload<BaseNetworkError>()

    class SearchOrdersResponsePayload(
        var site: SiteModel,
        var searchQuery: String,
        var canLoadMore: Boolean = false,
        var offset: Int = 0,
        var orders: List<WCOrderModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, query: String) : this(site, query) {
            this.error = error
        }
    }

    class FetchOrdersCountPayload(
        var site: SiteModel,
        var statusFilter: String
    ) : Payload<BaseNetworkError>()

    class FetchOrdersCountResponsePayload(
        var site: SiteModel,
        var statusFilter: String,
        var count: Int = 0
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, statusFilter: String) : this(site, statusFilter) {
            this.error = error
        }
    }

    class FetchSingleOrderPayload(
        var site: SiteModel,
        var remoteOrderId: Long
    ) : Payload<BaseNetworkError>()

    class FetchHasOrdersPayload(
        var site: SiteModel,
        var statusFilter: String? = null
    ) : Payload<BaseNetworkError>()

    class FetchHasOrdersResponsePayload(
        var site: SiteModel,
        var statusFilter: String? = null,
        var hasOrders: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) { this.error = error }
    }

    class UpdateOrderStatusPayload(
        val order: WCOrderModel,
        val site: SiteModel,
        val status: String
    ) : Payload<BaseNetworkError>()

    class RemoteOrderPayload(
        val order: WCOrderModel,
        val site: SiteModel
    ) : Payload<OrderError>() {
        constructor(error: OrderError, order: WCOrderModel, site: SiteModel) : this(order, site) { this.error = error }
    }

    class FetchOrderNotesPayload(
        var order: WCOrderModel,
        var site: SiteModel
    ) : Payload<BaseNetworkError>()

    class FetchOrderNotesResponsePayload(
        var order: WCOrderModel,
        var site: SiteModel,
        var notes: List<WCOrderNoteModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, order: WCOrderModel) : this(order, site) { this.error = error }
    }

    class PostOrderNotePayload(
        val order: WCOrderModel,
        val site: SiteModel,
        val note: WCOrderNoteModel
    ) : Payload<BaseNetworkError>()

    class RemoteOrderNotePayload(
        val order: WCOrderModel,
        val site: SiteModel,
        val note: WCOrderNoteModel
    ) : Payload<OrderError>() {
        constructor(error: OrderError, order: WCOrderModel, site: SiteModel, note: WCOrderNoteModel) :
                this(order, site, note) { this.error = error }
    }

    class FetchOrderStatusOptionsPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchOrderStatusOptionsResponsePayload(
        val site: SiteModel,
        val labels: List<WCOrderStatusModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) { this.error = error }
    }

    class FetchOrderShipmentTrackingsPayload(
        val site: SiteModel,
        val order: WCOrderModel
    ) : Payload<BaseNetworkError>()

    class FetchOrderShipmentTrackingsResponsePayload(
        var site: SiteModel,
        var order: WCOrderModel,
        var trackings: List<WCOrderShipmentTrackingModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, order: WCOrderModel) : this(site, order) { this.error = error }
    }

    class FetchOrderShipmentProvidersPayload(
        val site: SiteModel,
        val order: WCOrderModel
    ) : Payload<BaseNetworkError>()

    class FetchOrderShipmentProvidersResponsePayload(
        val site: SiteModel,
        val order: WCOrderModel,
        val providers: List<WCOrderShipmentProviderModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, order: WCOrderModel) : this(site, order) { this.error = error }
    }

    class OrderError(val type: OrderErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class OrderErrorType {
        INVALID_PARAM,
        INVALID_ID,
        ORDER_STATUS_NOT_FOUND,
        PLUGIN_NOT_ACTIVE,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = OrderErrorType.values().associateBy(OrderErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    // OnChanged events
    class OnOrderChanged(
        var rowsAffected: Int,
        var statusFilter: String? = null,
        var canLoadMore: Boolean = false
    ) : OnChanged<OrderError>() {
        var causeOfChange: WCOrderAction? = null
    }

    class OnOrdersFetchedByIds(
        val site: SiteModel,
        val orderIds: List<RemoteId>
    ) : OnChanged<OrderError>()

    class OnOrdersSearched(
        var searchQuery: String = "",
        var canLoadMore: Boolean = false,
        var nextOffset: Int = 0,
        var searchResults: List<WCOrderModel> = emptyList()
    ) : OnChanged<OrderError>()

    class OnOrderStatusOptionsChanged(
        var rowsAffected: Int
    ) : OnChanged<OrderError>()

    class OnOrderShipmentProvidersChanged(
        var rowsAffected: Int
    ) : OnChanged<OrderError>()

    override fun onRegister() = AppLog.d(T.API, "WCOrderStore onRegister")

    /**
     * Given a [SiteModel] and optional statuses, returns all orders for that site matching any of those statuses.
     */
    fun getOrdersForSite(site: SiteModel, vararg status: String): List<WCOrderModel> =
            OrderSqlUtils.getOrdersForSite(site, status = status.asList())

    fun getOrdersForDescriptor(
        orderListDescriptor: WCOrderListDescriptor,
        remoteOrderIds: List<RemoteId>
    ): Map<RemoteId, WCOrderModel> {
        val orders = OrderSqlUtils.getOrdersForSiteByRemoteIds(orderListDescriptor.site, remoteOrderIds)
        return orders.associateBy { RemoteId(it.remoteOrderId) }
    }

    /**
     * Given an [OrderIdentifier], returns the corresponding order from the database as a [WCOrderModel].
     */
    fun getOrderByIdentifier(orderIdentifier: OrderIdentifier): WCOrderModel? {
        return OrderSqlUtils.getOrderForIdSet((orderIdentifier.toIdSet()))
    }

    /**
     * Returns the notes belonging to supplied [WCOrderModel] as a list of [WCOrderNoteModel].
     */
    fun getOrderNotesForOrder(order: WCOrderModel): List<WCOrderNoteModel> =
            OrderSqlUtils.getOrderNotesForOrder(order.id)

    /**
     * Returns the order status options available for the provided site [SiteModel] as a list of [WCOrderStatusModel].
     */
    fun getOrderStatusOptionsForSite(site: SiteModel): List<WCOrderStatusModel> =
            OrderSqlUtils.getOrderStatusOptionsForSite(site)

    /**
     * Returns the order status as a [WCOrderStatusModel] that matches the provided order status key.
     */
    fun getOrderStatusForSiteAndKey(site: SiteModel, key: String): WCOrderStatusModel? =
            OrderSqlUtils.getOrderStatusOptionForSiteByKey(site, key)

    /**
     * Returns shipment trackings as list of [WCOrderShipmentTrackingModel] for a single [WCOrderModel]
     */
    fun getShipmentTrackingsForOrder(order: WCOrderModel): List<WCOrderShipmentTrackingModel> =
            OrderSqlUtils.getShipmentTrackingsForOrder(order)

    /**
     * Returns the shipment providers as a list of [WCOrderShipmentProviderModel]
     */
    fun getShipmentProvidersForSite(site: SiteModel): List<WCOrderShipmentProviderModel> =
            OrderSqlUtils.getOrderShipmentProvidersForSite(site)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCOrderAction ?: return
        when (actionType) {
            // remote actions
            WCOrderAction.FETCH_ORDERS -> fetchOrders(action.payload as FetchOrdersPayload)
            WCOrderAction.FETCH_ORDER_LIST -> fetchOrderList(action.payload as FetchOrderListPayload)
            WCOrderAction.FETCH_ORDERS_BY_IDS -> fetchOrdersByIds(action.payload as FetchOrdersByIdsPayload)
            WCOrderAction.FETCH_ORDERS_COUNT -> fetchOrdersCount(action.payload as FetchOrdersCountPayload)
            WCOrderAction.FETCH_SINGLE_ORDER -> fetchSingleOrder(action.payload as FetchSingleOrderPayload)
            WCOrderAction.UPDATE_ORDER_STATUS -> updateOrderStatus(action.payload as UpdateOrderStatusPayload)
            WCOrderAction.FETCH_ORDER_NOTES -> fetchOrderNotes(action.payload as FetchOrderNotesPayload)
            WCOrderAction.POST_ORDER_NOTE -> postOrderNote(action.payload as PostOrderNotePayload)
            WCOrderAction.FETCH_HAS_ORDERS -> fetchHasOrders(action.payload as FetchHasOrdersPayload)
            WCOrderAction.SEARCH_ORDERS -> searchOrders(action.payload as SearchOrdersPayload)
            WCOrderAction.FETCH_ORDER_STATUS_OPTIONS ->
                fetchOrderStatusOptions(action.payload as FetchOrderStatusOptionsPayload)
            WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS ->
                fetchOrderShipmentTrackings(action.payload as FetchOrderShipmentTrackingsPayload)
            WCOrderAction.FETCH_ORDER_SHIPMENT_PROVIDERS ->
                fetchOrderShipmentProviders(action.payload as FetchOrderShipmentProvidersPayload)

            // remote responses
            WCOrderAction.FETCHED_ORDERS -> handleFetchOrdersCompleted(action.payload as FetchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_LIST ->
                handleFetchOrderListCompleted(action.payload as FetchOrderListResponsePayload)
            WCOrderAction.FETCHED_ORDERS_BY_IDS ->
                handleFetchOrderByIdsCompleted(action.payload as FetchOrdersByIdsResponsePayload)
            WCOrderAction.FETCHED_ORDERS_COUNT ->
                handleFetchOrdersCountCompleted(action.payload as FetchOrdersCountResponsePayload)
            WCOrderAction.FETCHED_SINGLE_ORDER -> handleFetchSingleOrderCompleted(action.payload as RemoteOrderPayload)
            WCOrderAction.UPDATED_ORDER_STATUS -> handleUpdateOrderStatusCompleted(action.payload as RemoteOrderPayload)
            WCOrderAction.FETCHED_ORDER_NOTES ->
                handleFetchOrderNotesCompleted(action.payload as FetchOrderNotesResponsePayload)
            WCOrderAction.POSTED_ORDER_NOTE -> handlePostOrderNoteCompleted(action.payload as RemoteOrderNotePayload)
            WCOrderAction.FETCHED_HAS_ORDERS -> handleFetchHasOrdersCompleted(
                    action.payload as FetchHasOrdersResponsePayload)
            WCOrderAction.SEARCHED_ORDERS -> handleSearchOrdersCompleted(action.payload as SearchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS ->
                handleFetchOrderStatusOptionsCompleted(action.payload as FetchOrderStatusOptionsResponsePayload)
            WCOrderAction.FETCHED_ORDER_SHIPMENT_TRACKINGS ->
                handleFetchOrderShipmentTrackingsCompleted(action.payload as FetchOrderShipmentTrackingsResponsePayload)
            WCOrderAction.FETCHED_ORDER_SHIPMENT_PROVIDERS ->
                handleFetchOrderShipmentProvidersCompleted(
                        action.payload as FetchOrderShipmentProvidersResponsePayload)
        }
    }

    private fun fetchOrders(payload: FetchOrdersPayload) {
        val offset = if (payload.loadMore) {
            OrderSqlUtils.getOrdersForSite(payload.site).size
        } else {
            0
        }
        wcOrderRestClient.fetchOrders(payload.site, offset, payload.statusFilter)
    }

    private fun fetchOrderList(payload: FetchOrderListPayload) {
        wcOrderRestClient.fetchOrderList(payload.listDescriptor, payload.offset)
    }

    private fun fetchOrdersByIds(payload: FetchOrdersByIdsPayload) {
        wcOrderRestClient.fetchOrdersByIds(payload.site, payload.remoteIds)
    }

    private fun searchOrders(payload: SearchOrdersPayload) {
        wcOrderRestClient.searchOrders(payload.site, payload.searchQuery, payload.offset)
    }

    private fun fetchOrdersCount(payload: FetchOrdersCountPayload) {
        with(payload) { wcOrderRestClient.fetchOrderCount(site, statusFilter) }
    }

    private fun fetchHasOrders(payload: FetchHasOrdersPayload) {
        with(payload) { wcOrderRestClient.fetchHasOrders(site, statusFilter) }
    }

    private fun fetchSingleOrder(payload: FetchSingleOrderPayload) {
        with(payload) { wcOrderRestClient.fetchSingleOrder(site, remoteOrderId) }
    }

    private fun updateOrderStatus(payload: UpdateOrderStatusPayload) {
        with(payload) { wcOrderRestClient.updateOrderStatus(order, site, status) }
    }

    private fun fetchOrderNotes(payload: FetchOrderNotesPayload) {
        wcOrderRestClient.fetchOrderNotes(payload.order, payload.site)
    }

    private fun postOrderNote(payload: PostOrderNotePayload) {
        wcOrderRestClient.postOrderNote(payload.order, payload.site, payload.note)
    }

    private fun fetchOrderStatusOptions(payload: FetchOrderStatusOptionsPayload) {
        wcOrderRestClient.fetchOrderStatusOptions(payload.site)
    }

    private fun fetchOrderShipmentTrackings(payload: FetchOrderShipmentTrackingsPayload) {
        wcOrderRestClient.fetchOrderShipmentTrackings(payload.site, payload.order)
    }

    private fun fetchOrderShipmentProviders(payload: FetchOrderShipmentProvidersPayload) {
        wcOrderRestClient.fetchOrderShipmentProviders(payload.site, payload.order)
    }

    private fun handleFetchOrdersCompleted(payload: FetchOrdersResponsePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            // Clear existing uploading orders if this is a fresh fetch (loadMore = false in the original request)
            // This is the simplest way of keeping our local orders in sync with remote orders (in case of deletions,
            // or if the user manual changed some order IDs)
            if (!payload.loadedMore) {
                OrderSqlUtils.deleteOrdersForSite(payload.site)
                OrderSqlUtils.deleteOrderNotesForSite(payload.site)
                OrderSqlUtils.deleteOrderShipmentTrackingsForSite(payload.site)
            }

            val rowsAffected = payload.orders.sumBy { OrderSqlUtils.insertOrUpdateOrder(it) }

            onOrderChanged = OnOrderChanged(rowsAffected, payload.statusFilter, canLoadMore = payload.canLoadMore)
        }

        onOrderChanged.causeOfChange = WCOrderAction.FETCH_ORDERS

        emitChange(onOrderChanged)
    }

    private fun handleFetchOrderListCompleted(payload: FetchOrderListResponsePayload) {
        // TODO: Ideally we would have a separate process that prunes the following
        // tables of defunct records:
        // - WCOrderSummaryModel
        // - WCOrderModel
        // - WCOrderNoteModel
        // - WCOrderShipmentTrackingModel
        mDispatcher.dispatch(ListActionBuilder.newFetchedListItemsAction(FetchedListItemsPayload(
                listDescriptor = payload.listDescriptor,
                remoteItemIds = payload.orderIds.map { it.id },
                loadedMore = payload.loadedMore,
                canLoadMore = payload.canLoadMore,
                error = payload.error?.let { fetchError ->
                    // TODO: Use the actual error type
                    ListError(type = ListErrorType.GENERIC_ERROR, message = fetchError.message)
                }
        )))
    }

    private fun handleFetchOrderByIdsCompleted(payload: FetchOrdersByIdsResponsePayload) {
        val onOrdersFetchedByIds = OnOrdersFetchedByIds(payload.site, payload.orders.map { RemoteId(it.remoteOrderId) })
        if (payload.isError) {
            onOrdersFetchedByIds.error = payload.error
        } else {
            payload.orders.forEach { OrderSqlUtils.insertOrUpdateOrder(it) }
        }
        emitChange(onOrdersFetchedByIds)
        val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(
                localSiteId = payload.site.id
        )
        mDispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
    }

    private fun handleSearchOrdersCompleted(payload: SearchOrdersResponsePayload) {
        val onOrdersSearched = if (payload.isError) {
            OnOrdersSearched(payload.searchQuery)
        } else {
            OnOrdersSearched(payload.searchQuery, payload.canLoadMore, payload.offset, payload.orders)
        }
        emitChange(onOrdersSearched)
    }

    /**
     * This is a response to a request to retrieve only the count of orders matching a filter. These
     * results are not stored in the database.
     */
    private fun handleFetchOrdersCountCompleted(payload: FetchOrdersCountResponsePayload) {
        val onOrderChanged = if (payload.isError) {
            OnOrderChanged(0).also { it.error = payload.error }
        } else {
            with(payload) { OnOrderChanged(count, statusFilter) }
        }.also { it.causeOfChange = FETCH_ORDERS_COUNT }
        emitChange(onOrderChanged)
    }

    private fun handleFetchSingleOrderCompleted(payload: RemoteOrderPayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = OrderSqlUtils.insertOrUpdateOrder(payload.order)
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = WCOrderAction.FETCH_SINGLE_ORDER
        emitChange(onOrderChanged)
    }

    /**
     * This is a response to a request to determine whether any orders matching a filter exist
     */
    private fun handleFetchHasOrdersCompleted(payload: FetchHasOrdersResponsePayload) {
        val onOrderChanged = if (payload.isError) {
            OnOrderChanged(0).also { it.error = payload.error }
        } else {
            with(payload) {
                // set 'rowsAffected' to non-zero if there are orders, otherwise set to zero
                val rowsAffected = if (payload.hasOrders) 1 else 0
                OnOrderChanged(rowsAffected, statusFilter)
            }
        }.also { it.causeOfChange = FETCH_HAS_ORDERS }
        emitChange(onOrderChanged)
    }

    private fun handleUpdateOrderStatusCompleted(payload: RemoteOrderPayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = OrderSqlUtils.insertOrUpdateOrder(payload.order)
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS

        emitChange(onOrderChanged)
    }

    private fun handleFetchOrderNotesCompleted(payload: FetchOrderNotesResponsePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = OrderSqlUtils.insertOrIgnoreOrderNotes(payload.notes)
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = FETCH_ORDER_NOTES
        emitChange(onOrderChanged)
    }

    private fun handlePostOrderNoteCompleted(payload: RemoteOrderNotePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = OrderSqlUtils.insertOrIgnoreOrderNote(payload.note)
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = POST_ORDER_NOTE
        emitChange(onOrderChanged)
    }

    private fun handleFetchOrderStatusOptionsCompleted(payload: FetchOrderStatusOptionsResponsePayload) {
        val onOrderStatusLabelsChanged: OnOrderStatusOptionsChanged

        if (payload.isError) {
            onOrderStatusLabelsChanged = OnOrderStatusOptionsChanged(0).also { it.error = payload.error }
        } else {
            val existingOptions = OrderSqlUtils.getOrderStatusOptionsForSite(payload.site)
            val deleteOptions = mutableListOf<WCOrderStatusModel>()
            val addOrUpdateOptions = mutableListOf<WCOrderStatusModel>()
            existingOptions.iterator().forEach { existingOption ->
                var exists = false
                payload.labels.iterator().forEach noi@{ newOption ->
                    if (newOption.statusKey == existingOption.statusKey) {
                        exists = true
                        return@noi
                    }
                }
                if (!exists) deleteOptions.add(existingOption)
            }
            payload.labels.iterator().forEach { newOption ->
                var exists = false
                existingOptions.iterator().forEach eoi@{ existingOption ->
                    if (newOption.statusKey == existingOption.statusKey) {
                        exists = true
                        if (newOption.label != existingOption.label) {
                            addOrUpdateOptions.add(newOption)
                        }
                        return@eoi
                    }
                }
                if (!exists) addOrUpdateOptions.add(newOption)
            }

            var rowsAffected = addOrUpdateOptions.sumBy { OrderSqlUtils.insertOrUpdateOrderStatusOption(it) }
            rowsAffected += deleteOptions.sumBy { OrderSqlUtils.deleteOrderStatusOption(it) }
            onOrderStatusLabelsChanged = OnOrderStatusOptionsChanged(rowsAffected)
        }

        emitChange(onOrderStatusLabelsChanged)
    }

    private fun handleFetchOrderShipmentTrackingsCompleted(payload: FetchOrderShipmentTrackingsResponsePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            // Calculate which existing records should be deleted because they no longer exist in the payload
            val existingTrackings = OrderSqlUtils.getShipmentTrackingsForOrder(payload.order)
            val deleteTrackings = mutableListOf<WCOrderShipmentTrackingModel>()
            existingTrackings.iterator().forEach { existing ->
                var exists = false
                payload.trackings.iterator().forEach nti@{ newTracking ->
                    if (newTracking.remoteTrackingId == existing.remoteTrackingId) {
                        exists = true
                        return@nti
                    }
                }
                if (!exists) deleteTrackings.add(existing)
            }
            var rowsAffected = deleteTrackings.sumBy { OrderSqlUtils.deleteOrderShipmentTrackingById(it) }

            // Save new shipment trackings to the database
            rowsAffected += payload.trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = FETCH_ORDER_SHIPMENT_TRACKINGS
        emitChange(onOrderChanged)
    }

    private fun handleFetchOrderShipmentProvidersCompleted(
        payload: FetchOrderShipmentProvidersResponsePayload
    ) {
        val onProviderChanged: OnOrderShipmentProvidersChanged

        if (payload.isError) {
            onProviderChanged = OnOrderShipmentProvidersChanged(0).also { it.error = payload.error }
        } else {
            // Delete all providers from the db
            OrderSqlUtils.deleteOrderShipmentProvidersForSite(payload.site)

            // Add new list to the database
            val rowsAffected = payload.providers.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentProvider(it) }
            onProviderChanged = OnOrderShipmentProvidersChanged(rowsAffected)
        }

        emitChange(onProviderChanged)
    }
}
