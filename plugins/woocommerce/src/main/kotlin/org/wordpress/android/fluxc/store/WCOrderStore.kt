package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCOrderAction
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
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCOrderStore @Inject constructor(
    dispatcher: Dispatcher,
    private val wcOrderRestClient: OrderRestClient,
    private val wcOrderFetcher: WCOrderFetcher,
    private val coroutineEngine: CoroutineEngine
) : Store(dispatcher) {
    companion object {
        const val NUM_ORDERS_PER_FETCH = 15
        const val DEFAULT_ORDER_STATUS = "any"
    }

    class FetchOrdersPayload(
        var site: SiteModel,
        var statusFilter: String? = null,
        var loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchOrderListPayload(
        val listDescriptor: WCOrderListDescriptor,
        val offset: Long,
        val requestStartTime: Calendar = Calendar.getInstance()
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

    class FetchOrderListResponsePayload(
        val listDescriptor: WCOrderListDescriptor,
        var orderSummaries: List<WCOrderSummaryModel> = emptyList(),
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        val requestStartTime: Calendar
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            listDescriptor: WCOrderListDescriptor,
            requestStartTime: Calendar
        ) : this(listDescriptor, requestStartTime = requestStartTime) {
            this.error = error
        }
    }

    class FetchOrdersByIdsResponsePayload(
        val site: SiteModel,
        var remoteOrderIds: List<RemoteId>,
        var fetchedOrders: List<WCOrderModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            remoteOrderIds: List<RemoteId>
        ) : this(site = site, remoteOrderIds = remoteOrderIds) {
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
        var localOrderId: Int,
        var remoteOrderId: Long,
        var site: SiteModel
    ) : Payload<BaseNetworkError>()

    class FetchOrderNotesResponsePayload(
        var localOrderId: Int,
        var remoteOrderId: Long,
        var site: SiteModel,
        var notes: List<WCOrderNoteModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, localOrderId: Int, remoteOrderId: Long) : this(
                localOrderId, remoteOrderId, site
        ) { this.error = error }
    }

    class PostOrderNotePayload(
        var localOrderId: Int,
        var remoteOrderId: Long,
        val site: SiteModel,
        val note: WCOrderNoteModel
    ) : Payload<BaseNetworkError>()

    class RemoteOrderNotePayload(
        var localOrderId: Int,
        var remoteOrderId: Long,
        val site: SiteModel,
        val note: WCOrderNoteModel
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            localOrderId: Int,
            remoteOrderId: Long,
            site: SiteModel,
            note: WCOrderNoteModel
        ) : this(localOrderId, remoteOrderId, site, note) { this.error = error }
    }

    class FetchOrderStatusOptionsPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchOrderStatusOptionsResponsePayload(
        val site: SiteModel,
        val labels: List<WCOrderStatusModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) { this.error = error }
    }

    class FetchOrderShipmentTrackingsPayload(
        var localOrderId: Int,
        var remoteOrderId: Long,
        val site: SiteModel
    ) : Payload<BaseNetworkError>()

    class FetchOrderShipmentTrackingsResponsePayload(
        var site: SiteModel,
        var localOrderId: Int,
        var trackings: List<WCOrderShipmentTrackingModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, localOrderId: Int) :
                this(site, localOrderId) { this.error = error }
    }

    class AddOrderShipmentTrackingPayload(
        val site: SiteModel,
        var localOrderId: Int,
        var remoteOrderId: Long,
        val tracking: WCOrderShipmentTrackingModel,
        val isCustomProvider: Boolean
    ) : Payload<BaseNetworkError>()

    class AddOrderShipmentTrackingResponsePayload(
        val site: SiteModel,
        var localOrderId: Int,
        var remoteOrderId: Long,
        val tracking: WCOrderShipmentTrackingModel?
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            localOrderId: Int,
            remoteOrderId: Long,
            tracking: WCOrderShipmentTrackingModel
        ) : this(site, localOrderId, remoteOrderId, tracking) { this.error = error }
    }

    class DeleteOrderShipmentTrackingPayload(
        val site: SiteModel,
        var localOrderId: Int,
        var remoteOrderId: Long,
        val tracking: WCOrderShipmentTrackingModel
    ) : Payload<BaseNetworkError>()

    class DeleteOrderShipmentTrackingResponsePayload(
        val site: SiteModel,
        var localOrderId: Int,
        var remoteOrderId: Long,
        val tracking: WCOrderShipmentTrackingModel?
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            localOrderId: Int,
            remoteOrderId: Long,
            tracking: WCOrderShipmentTrackingModel?
        ) : this(site, localOrderId, remoteOrderId, tracking) { this.error = error }
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
        INVALID_RESPONSE,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(OrderErrorType::name)
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

    /**
     * Emitted after fetching a list of Order summaries from the network.
     */
    class OnOrderSummariesFetched(
        val listDescriptor: WCOrderListDescriptor,
        val duration: Long
    ) : OnChanged<OrderError>()

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

    fun getOrderSummariesByRemoteOrderIds(
        site: SiteModel,
        remoteOrderIds: List<RemoteId>
    ): Map<RemoteId, WCOrderSummaryModel> {
        val orderSummaries = OrderSqlUtils.getOrderSummariesForRemoteIds(site, remoteOrderIds)
        return orderSummaries.associateBy { RemoteId(it.remoteOrderId) }
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
    fun getOrderNotesForOrder(orderId: Int): List<WCOrderNoteModel> =
            OrderSqlUtils.getOrderNotesForOrder(orderId)

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
    fun getShipmentTrackingsForOrder(site: SiteModel, localOrderId: Int): List<WCOrderShipmentTrackingModel> =
            OrderSqlUtils.getShipmentTrackingsForOrder(site, localOrderId)

    fun getShipmentTrackingByTrackingNumber(site: SiteModel, localOrderId: Int, trackingNumber: String) =
            OrderSqlUtils.getShipmentTrackingByTrackingNumber(site, localOrderId, trackingNumber)

    /**
     * Returns the shipment providers as a list of [WCOrderShipmentProviderModel]
     */
    fun getShipmentProvidersForSite(site: SiteModel): List<WCOrderShipmentProviderModel> =
            OrderSqlUtils.getOrderShipmentProvidersForSite(site)

    /**
     * @return Returns true if orders for the provided site exist in the DB, else false.
     */
    fun hasCachedOrdersForSite(site: SiteModel) = OrderSqlUtils.getOrderCountForSite(site) > 0

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCOrderAction ?: return
        when (actionType) {
            // remote actions
            WCOrderAction.FETCH_ORDERS -> fetchOrders(action.payload as FetchOrdersPayload)
            WCOrderAction.FETCH_ORDER_LIST -> fetchOrderList(action.payload as FetchOrderListPayload)
            WCOrderAction.FETCH_ORDERS_BY_IDS -> fetchOrdersByIds(action.payload as FetchOrdersByIdsPayload)
            WCOrderAction.FETCH_ORDERS_COUNT -> fetchOrdersCount(action.payload as FetchOrdersCountPayload)
            WCOrderAction.UPDATE_ORDER_STATUS -> updateOrderStatus(action.payload as UpdateOrderStatusPayload)
            WCOrderAction.POST_ORDER_NOTE -> postOrderNote(action.payload as PostOrderNotePayload)
            WCOrderAction.FETCH_HAS_ORDERS -> fetchHasOrders(action.payload as FetchHasOrdersPayload)
            WCOrderAction.SEARCH_ORDERS -> searchOrders(action.payload as SearchOrdersPayload)
            WCOrderAction.FETCH_ORDER_STATUS_OPTIONS ->
                fetchOrderStatusOptions(action.payload as FetchOrderStatusOptionsPayload)
            WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING ->
                addOrderShipmentTracking(action.payload as AddOrderShipmentTrackingPayload)
            WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING ->
                deleteOrderShipmentTracking(action.payload as DeleteOrderShipmentTrackingPayload)
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
            WCOrderAction.UPDATED_ORDER_STATUS -> handleUpdateOrderStatusCompleted(action.payload as RemoteOrderPayload)
            WCOrderAction.POSTED_ORDER_NOTE -> handlePostOrderNoteCompleted(action.payload as RemoteOrderNotePayload)
            WCOrderAction.FETCHED_HAS_ORDERS -> handleFetchHasOrdersCompleted(
                    action.payload as FetchHasOrdersResponsePayload)
            WCOrderAction.SEARCHED_ORDERS -> handleSearchOrdersCompleted(action.payload as SearchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS ->
                handleFetchOrderStatusOptionsCompleted(action.payload as FetchOrderStatusOptionsResponsePayload)
            WCOrderAction.ADDED_ORDER_SHIPMENT_TRACKING ->
                handleAddOrderShipmentTrackingCompleted(action.payload as AddOrderShipmentTrackingResponsePayload)
            WCOrderAction.DELETED_ORDER_SHIPMENT_TRACKING ->
                handleDeleteOrderShipmentTrackingCompleted(action.payload as DeleteOrderShipmentTrackingResponsePayload)
            WCOrderAction.FETCHED_ORDER_SHIPMENT_PROVIDERS ->
                handleFetchOrderShipmentProvidersCompleted(
                        action.payload as FetchOrderShipmentProvidersResponsePayload)
        }
    }

    private fun fetchOrders(payload: FetchOrdersPayload) {
        val offset = if (payload.loadMore) {
            OrderSqlUtils.getOrderCountForSite(payload.site)
        } else {
            0
        }
        wcOrderRestClient.fetchOrders(payload.site, offset, payload.statusFilter)
    }

    private fun fetchOrderList(payload: FetchOrderListPayload) {
        wcOrderRestClient.fetchOrderListSummaries(
                listDescriptor = payload.listDescriptor,
                offset = payload.offset,
                requestStartTime = payload.requestStartTime
        )
    }

    private fun fetchOrdersByIds(payload: FetchOrdersByIdsPayload) {
        payload.remoteIds.chunked(NUM_ORDERS_PER_FETCH).forEach { idsToFetch ->
            wcOrderRestClient.fetchOrdersByIds(payload.site, idsToFetch)
        }
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

    suspend fun fetchSingleOrder(site: SiteModel, remoteOrderId: Long): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSingleOrder") {
            val result = wcOrderRestClient.fetchSingleOrder(site, remoteOrderId)

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(0).also { it.error = result.error }
            } else {
                val rowsAffected = OrderSqlUtils.insertOrUpdateOrder(result.order)
                OnOrderChanged(rowsAffected)
            }
        }
    }

    private fun updateOrderStatus(payload: UpdateOrderStatusPayload) {
        with(payload) {
            val rowsAffected = updateOrderStatusLocally(payload.order.id, payload.status)

            emitChange(OnOrderChanged(rowsAffected).apply { causeOfChange = WCOrderAction.UPDATED_ORDER_STATUS })

            wcOrderRestClient.updateOrderStatus(payload.order, site, status)
        }
    }

    private fun updateOrderStatusLocally(localOrderId: Int, newStatus: String): Int {
        val updatedOrder = OrderSqlUtils.getOrderByLocalId(localOrderId).apply {
            status = newStatus
        }
        return OrderSqlUtils.insertOrUpdateOrder(updatedOrder)
    }

    suspend fun fetchOrderNotes(localOrderId: Int, remoteOrderId: Long, site: SiteModel): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchOrderNotes") {
            val result = wcOrderRestClient.fetchOrderNotes(localOrderId, remoteOrderId, site)

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(0).also { it.error = result.error }
            } else {
                val rowsAffected = OrderSqlUtils.insertOrIgnoreOrderNotes(result.notes)
                OnOrderChanged(rowsAffected)
            }
        }
    }

    private fun postOrderNote(payload: PostOrderNotePayload) {
        with(payload) { wcOrderRestClient.postOrderNote(localOrderId, remoteOrderId, site, note) }
    }

    private fun fetchOrderStatusOptions(payload: FetchOrderStatusOptionsPayload) {
        wcOrderRestClient.fetchOrderStatusOptions(payload.site)
    }

    suspend fun fetchOrderShipmentTrackings(localOrderId: Int, remoteOrderId: Long, site: SiteModel): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchOrderShipmentTrackings") {
            val result = wcOrderRestClient.fetchOrderShipmentTrackings(site, localOrderId, remoteOrderId)
            return@withDefaultContext if (result.isError) {
                OnOrderChanged(0).also { it.error = result.error }
            } else {
                // Calculate which existing records should be deleted because they no longer exist in the payload
                val existingTrackings = OrderSqlUtils.getShipmentTrackingsForOrder(
                        result.site,
                        result.localOrderId
                )
                val deleteTrackings = mutableListOf<WCOrderShipmentTrackingModel>()
                existingTrackings.iterator().forEach { existing ->
                    var exists = false
                    result.trackings.iterator().forEach nti@{ newTracking ->
                        if (newTracking.remoteTrackingId == existing.remoteTrackingId) {
                            exists = true
                            return@nti
                        }
                    }
                    if (!exists) deleteTrackings.add(existing)
                }
                var rowsAffected = deleteTrackings.sumBy { OrderSqlUtils.deleteOrderShipmentTrackingById(it) }

                // Save new shipment trackings to the database
                rowsAffected += result.trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
                OnOrderChanged(rowsAffected)
            }
        }
    }

    private fun addOrderShipmentTracking(payload: AddOrderShipmentTrackingPayload) {
        with(payload) {
            wcOrderRestClient.addOrderShipmentTrackingForOrder(
                    site, localOrderId, remoteOrderId, tracking, isCustomProvider
            )
        }
    }

    private fun deleteOrderShipmentTracking(payload: DeleteOrderShipmentTrackingPayload) {
        with(payload) { wcOrderRestClient.deleteShipmentTrackingForOrder(site, localOrderId, remoteOrderId, tracking) }
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
        // - WCOrderModel
        // - WCOrderNoteModel
        // - WCOrderShipmentTrackingModel
        if (!payload.isError) {
            // Save order summaries to the db
            OrderSqlUtils.insertOrUpdateOrderSummaries(payload.orderSummaries)

            // Fetch outdated or missing orders
            fetchOutdatedOrMissingOrders(payload.listDescriptor.site, payload.orderSummaries)
        }

        val duration = Calendar.getInstance().timeInMillis - payload.requestStartTime.timeInMillis
        emitChange(OnOrderSummariesFetched(listDescriptor = payload.listDescriptor, duration = duration))

        mDispatcher.dispatch(ListActionBuilder.newFetchedListItemsAction(FetchedListItemsPayload(
                listDescriptor = payload.listDescriptor,
                remoteItemIds = payload.orderSummaries.map { it.remoteOrderId },
                loadedMore = payload.loadedMore,
                canLoadMore = payload.canLoadMore,
                error = payload.error?.let { fetchError ->
                    // TODO: Use the actual error type
                    ListError(type = ListErrorType.GENERIC_ERROR, message = fetchError.message)
                }
        )))
    }

    private fun fetchOutdatedOrMissingOrders(site: SiteModel, fetchedSummaries: List<WCOrderSummaryModel>) {
        val fetchedSummariesIds = fetchedSummaries.map { RemoteId(it.remoteOrderId) }
        val localOrdersForFetchedSummaries = OrderSqlUtils.getOrdersForSiteByRemoteIds(site, fetchedSummariesIds)

        val idsToFetch = outdatedOrdersIds(fetchedSummaries, localOrdersForFetchedSummaries)
                .plus(missingOrdersIds(fetchedSummariesIds, localOrdersForFetchedSummaries))

        wcOrderFetcher.fetchOrders(site = site, remoteItemIds = idsToFetch)
    }

    private fun outdatedOrdersIds(
        fetchedSummaries: List<WCOrderSummaryModel>,
        localOrdersForSiteByRemoteIds: List<WCOrderModel>
    ): List<RemoteId> {
        val summaryModifiedDates = fetchedSummaries.associate { it.remoteOrderId to it.dateModified }

        return localOrdersForSiteByRemoteIds.filter { order ->
            order.dateModified != summaryModifiedDates[order.remoteOrderId]
        }.map(WCOrderModel::remoteOrderId).map(::RemoteId)
    }

    private fun missingOrdersIds(
        fetchedSummariesIds: List<RemoteId>,
        localOrdersForSiteByRemoteIds: List<WCOrderModel>
    ): List<RemoteId> {
        return fetchedSummariesIds.minus(
                localOrdersForSiteByRemoteIds.map(WCOrderModel::remoteOrderId).map(::RemoteId)
        )
    }

    private fun handleFetchOrderByIdsCompleted(payload: FetchOrdersByIdsResponsePayload) {
        val onOrdersFetchedByIds = if (payload.isError) {
            OnOrdersFetchedByIds(payload.site, payload.remoteOrderIds).apply { error = payload.error }
        } else {
            OnOrdersFetchedByIds(payload.site, payload.fetchedOrders.map { RemoteId(it.remoteOrderId) })
        }

        if (!payload.isError) {
            // Save the list of orders to the database
            payload.fetchedOrders.forEach { OrderSqlUtils.insertOrUpdateOrder(it) }

            // Notify listeners that the list of orders has changed (only call this if there is no error)
            val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(localSiteId = payload.site.id)
            mDispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
        }

        emitChange(onOrdersFetchedByIds)
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
        }.also { it.causeOfChange = WCOrderAction.FETCH_ORDERS_COUNT }
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
        }.also { it.causeOfChange = WCOrderAction.FETCH_HAS_ORDERS }
        emitChange(onOrderChanged)
    }

    private fun handleUpdateOrderStatusCompleted(payload: RemoteOrderPayload) {
        val onOrderChanged: OnOrderChanged = if (payload.isError) {
            revertOrderStatus(payload)
        } else {
            val rowsAffected = OrderSqlUtils.insertOrUpdateOrder(payload.order)
            OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS

        emitChange(onOrderChanged)
    }

    private fun revertOrderStatus(payload: RemoteOrderPayload): OnOrderChanged {
        val rowsAffected = updateOrderStatusLocally(payload.order.id, payload.order.status)
        return OnOrderChanged(rowsAffected).also { it.error = payload.error }
    }

    private fun handlePostOrderNoteCompleted(payload: RemoteOrderNotePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = OrderSqlUtils.insertOrIgnoreOrderNote(payload.note)
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = WCOrderAction.POST_ORDER_NOTE
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
                        if (newOption.label != existingOption.label ||
                                newOption.statusCount != existingOption.statusCount) {
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

    private fun handleAddOrderShipmentTrackingCompleted(payload: AddOrderShipmentTrackingResponsePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = payload.tracking?.let { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) } ?: 0
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING
        emitChange(onOrderChanged)
    }

    private fun handleDeleteOrderShipmentTrackingCompleted(payload: DeleteOrderShipmentTrackingResponsePayload) {
        val onOrderChanged: OnOrderChanged

        if (payload.isError) {
            onOrderChanged = OnOrderChanged(0).also { it.error = payload.error }
        } else {
            // Remove the record from the database and send response
            val rowsAffected = payload.tracking?.let { OrderSqlUtils.deleteOrderShipmentTrackingById(it) } ?: 0
            onOrderChanged = OnOrderChanged(rowsAffected)
        }

        onOrderChanged.causeOfChange = WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING
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
