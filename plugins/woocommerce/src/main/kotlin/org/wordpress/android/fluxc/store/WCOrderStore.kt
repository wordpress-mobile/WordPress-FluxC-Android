package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
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
    private val coroutineEngine: CoroutineEngine,
    private val ordersDao: OrdersDao,
    private val orderNotesDao: OrderNotesDao
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
        val orderIds: List<Long>
    ) : Payload<BaseNetworkError>()

    class FetchOrdersResponsePayload(
        var site: SiteModel,
        var orders: List<OrderEntity> = emptyList(),
        var statusFilter: String? = null,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) {
            this.error = error
        }
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
        var orderIds: List<Long>,
        var fetchedOrders: List<OrderEntity> = emptyList()
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            orderIds: List<Long>
        ) : this(site = site, orderIds = orderIds) {
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
        var orders: List<OrderEntity> = emptyList()
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

    class FetchHasOrdersResponsePayload(
        var site: SiteModel,
        var statusFilter: String? = null,
        var hasOrders: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class UpdateOrderStatusPayload(
        val order: OrderEntity,
        val site: SiteModel,
        val status: String
    ) : Payload<BaseNetworkError>()

    class RemoteOrderPayload(
        val order: OrderEntity,
        val site: SiteModel
    ) : Payload<OrderError>() {
        constructor(error: OrderError, order: OrderEntity, site: SiteModel) : this(order, site) {
            this.error = error
        }
    }

    sealed class UpdateOrderResult {
        abstract val event: OnOrderChanged

        data class OptimisticUpdateResult(override val event: OnOrderChanged) : UpdateOrderResult()
        data class RemoteUpdateResult(override val event: OnOrderChanged) : UpdateOrderResult()
    }

    class FetchOrderStatusOptionsPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchOrderStatusOptionsResponsePayload(
        val site: SiteModel,
        val labels: List<WCOrderStatusModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class FetchOrderShipmentTrackingsResponsePayload(
        var site: SiteModel,
        var orderId: Long,
        var trackings: List<WCOrderShipmentTrackingModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, orderId: Long) :
            this(site, orderId) {
            this.error = error
        }
    }

    class AddOrderShipmentTrackingPayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel,
        val isCustomProvider: Boolean
    ) : Payload<BaseNetworkError>()

    class AddOrderShipmentTrackingResponsePayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel?
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            orderId: Long,
            tracking: WCOrderShipmentTrackingModel
        ) : this(site, orderId, tracking) {
            this.error = error
        }
    }

    class DeleteOrderShipmentTrackingPayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel
    ) : Payload<BaseNetworkError>()

    class DeleteOrderShipmentTrackingResponsePayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel?
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            orderId: Long,
            tracking: WCOrderShipmentTrackingModel?
        ) : this(site, orderId, tracking) {
            this.error = error
        }
    }

    class FetchOrderShipmentProvidersPayload(
        val site: SiteModel,
        val order: OrderEntity
    ) : Payload<BaseNetworkError>()

    class FetchOrderShipmentProvidersResponsePayload(
        val site: SiteModel,
        val order: OrderEntity,
        val providers: List<WCOrderShipmentProviderModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, order: OrderEntity) : this(site, order) {
            this.error = error
        }
    }

    data class OrderError(val type: OrderErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class OrderErrorType {
        INVALID_PARAM,
        INVALID_ID,
        ORDER_STATUS_NOT_FOUND,
        PLUGIN_NOT_ACTIVE,
        INVALID_RESPONSE,
        GENERIC_ERROR,
        EMPTY_BILLING_EMAIL;

        companion object {
            private val reverseMap = values().associateBy(OrderErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    sealed class HasOrdersResult {
        data class Success(val hasOrders: Boolean) : HasOrdersResult()
        data class Failure(val error: OrderError) : HasOrdersResult()
    }

    // OnChanged events
    data class OnOrderChanged(
        val statusFilter: String? = null,
        val canLoadMore: Boolean = false,
        val causeOfChange: WCOrderAction? = null,
        private val orderError: OrderError? = null
    ) : OnChanged<OrderError>() {
        init {
            super.error = orderError
        }
    }

    // TODO nbradbury this and related code can be removed
    data class OnQuickOrderResult(
        var order: OrderEntity? = null
    ) : OnChanged<OrderError>()

    /**
     * Emitted after fetching a list of Order summaries from the network.
     */
    class OnOrderSummariesFetched(
        val listDescriptor: WCOrderListDescriptor,
        val duration: Long
    ) : OnChanged<OrderError>()

    class OnOrdersFetchedByIds(
        val site: SiteModel,
        val orderIds: List<Long>
    ) : OnChanged<OrderError>()

    class OnOrdersSearched(
        var searchQuery: String = "",
        var canLoadMore: Boolean = false,
        var nextOffset: Int = 0,
        var searchResults: List<OrderEntity> = emptyList()
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
    suspend fun getOrdersForSite(site: SiteModel, vararg status: String) = if (status.isEmpty()) {
        ordersDao.getOrdersForSite(site.localId())
    } else {
        ordersDao.getOrdersForSite(site.localId(), status = status.asList())
    }

    /**
     * Observe the changes to orders for a given [SiteModel]
     *
     * @param site the current site
     * @param statuses an optional list of statuses to filter the list of orders, pass an empty list to include all
     *                 orders
     */
    fun observeOrdersForSite(site: SiteModel, statuses: List<String> = emptyList()): Flow<List<OrderEntity>> {
        return if (statuses.isEmpty()) {
        ordersDao.observeOrdersForSite(site.localId())
        } else {
            ordersDao.observeOrdersForSite(site.localId(), statuses)
        }
    }

    fun getOrdersForDescriptor(
        orderListDescriptor: WCOrderListDescriptor,
        orderIds: List<Long>
    ): Map<Long, OrderEntity> {
        val orders = ordersDao.getOrdersForSiteByRemoteIds(orderListDescriptor.site.localId(), orderIds)
        return orders.associateBy { it.orderId }
    }

    fun getOrderSummariesByRemoteOrderIds(
        site: SiteModel,
        orderIds: List<Long>
    ): Map<RemoteId, WCOrderSummaryModel> {
        val orderSummaries = OrderSqlUtils.getOrderSummariesForRemoteIds(site, orderIds)
        return orderSummaries.associateBy { RemoteId(it.orderId) }
    }

    /**
     * Given an order id and [SiteModel],
     * returns the corresponding order from the database as a [OrderEntity].
     */
    suspend fun getOrderByIdAndSite(orderId: Long, site: SiteModel): OrderEntity? {
        return ordersDao.getOrder(orderId, site.localId())
    }

    /**
     * Returns the notes belonging to supplied [OrderEntity] as a list of [OrderNoteEntity].
     */
    suspend fun getOrderNotesForOrder(site: SiteModel, orderId: Long): List<OrderNoteEntity> =
            orderNotesDao.queryNotesOfOrder(siteId = site.remoteId(), orderId = RemoteId(orderId))

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
     * Returns shipment trackings as list of [WCOrderShipmentTrackingModel] for a single [OrderEntity]
     */
    fun getShipmentTrackingsForOrder(site: SiteModel, orderId: Long): List<WCOrderShipmentTrackingModel> =
        OrderSqlUtils.getShipmentTrackingsForOrder(site, orderId)

    fun getShipmentTrackingByTrackingNumber(site: SiteModel, orderId: Long, trackingNumber: String) =
        OrderSqlUtils.getShipmentTrackingByTrackingNumber(site, orderId, trackingNumber)

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
            WCOrderAction.UPDATE_ORDER_STATUS ->
                throw IllegalStateException("Invalid action. Use suspendable updateOrderStatus(..) directly")
            WCOrderAction.SEARCH_ORDERS -> searchOrders(action.payload as SearchOrdersPayload)
            WCOrderAction.FETCH_ORDER_STATUS_OPTIONS ->
                fetchOrderStatusOptions(action.payload as FetchOrderStatusOptionsPayload)

            // remote responses
            WCOrderAction.FETCHED_ORDERS -> handleFetchOrdersCompleted(action.payload as FetchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_LIST ->
                handleFetchOrderListCompleted(action.payload as FetchOrderListResponsePayload)
            WCOrderAction.FETCHED_ORDERS_BY_IDS ->
                handleFetchOrderByIdsCompleted(action.payload as FetchOrdersByIdsResponsePayload)
            WCOrderAction.FETCHED_ORDERS_COUNT ->
                handleFetchOrdersCountCompleted(action.payload as FetchOrdersCountResponsePayload)
            WCOrderAction.SEARCHED_ORDERS -> handleSearchOrdersCompleted(action.payload as SearchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS ->
                handleFetchOrderStatusOptionsCompleted(action.payload as FetchOrderStatusOptionsResponsePayload)
        }
    }

    private fun fetchOrders(payload: FetchOrdersPayload) {
        val offset = if (payload.loadMore) {
            ordersDao.getOrderCountForSite(payload.site.localId())
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
        payload.orderIds.chunked(NUM_ORDERS_PER_FETCH).forEach { idsToFetch ->
            wcOrderRestClient.fetchOrdersByIds(payload.site, idsToFetch)
        }
    }

    private fun searchOrders(payload: SearchOrdersPayload) {
        wcOrderRestClient.searchOrders(payload.site, payload.searchQuery, payload.offset)
    }

    private fun fetchOrdersCount(payload: FetchOrdersCountPayload) {
        with(payload) { wcOrderRestClient.fetchOrderCount(site, statusFilter) }
    }

    suspend fun fetchHasOrders(site: SiteModel, status: String?): HasOrdersResult {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchHasOrders") {
            val result = wcOrderRestClient.fetchHasOrders(site, status)

            return@withDefaultContext if (result.isError) {
                HasOrdersResult.Failure(result.error)
            } else {
                HasOrdersResult.Success(result.hasOrders)
            }
        }
    }

    suspend fun fetchSingleOrder(site: SiteModel, remoteOrderId: Long): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSingleOrder") {
            val result = wcOrderRestClient.fetchSingleOrder(site, remoteOrderId)

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                ordersDao.insertOrUpdateOrder(order = result.order)
                OnOrderChanged()
            }
        }
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        site: SiteModel,
        newStatus: WCOrderStatusModel
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateOrderStatus") {
            val orderModel = ordersDao.getOrder(orderId, site.localId())

            if (orderModel != null) {
                updateOrderStatusLocally(orderId, site.localId(), newStatus.statusKey)

                val optimisticUpdateResult = OnOrderChanged(
                    causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS
                )

                emit(OptimisticUpdateResult(optimisticUpdateResult))

                val remotePayload = wcOrderRestClient.updateOrderStatus(orderModel, site, newStatus.statusKey)
                val remoteUpdateResult: OnOrderChanged = if (remotePayload.isError) {
                    revertOrderStatus(remotePayload)
                } else {
                    ordersDao.insertOrUpdateOrder(remotePayload.order)
                    OnOrderChanged()
                }.copy(causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS)

                emit(RemoteUpdateResult(remoteUpdateResult))
                // Needs to remain here until all event bus observables are removed from the client code
                emitChange(remoteUpdateResult)
            } else {
                emit(
                    OptimisticUpdateResult(
                        OnOrderChanged(
                            orderError = OrderError(
                                message = "Order with id $orderId not found"
                            )
                        )
                    )
                )
            }
        }
    }

    private suspend fun updateOrderStatusLocally(orderId: Long, localSiteId: LocalId, newStatus: String) {
        val updatedOrder = ordersDao.getOrder(orderId, localSiteId)!!
            .copy(status = newStatus)
        ordersDao.insertOrUpdateOrder(updatedOrder)
    }

    suspend fun fetchOrderNotes(
        site: SiteModel,
        orderId: Long
    ): WooResult<List<OrderNoteEntity>> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchOrderNotes") {
            val result = wcOrderRestClient.fetchOrderNotes(orderId, site)

            return@withDefaultContext result.let {
                if (!it.isError) {
                    orderNotesDao.insertNotes(*it.result!!.toTypedArray())
                }
                result.asWooResult()
            }
        }
    }

    suspend fun postOrderNote(
        site: SiteModel,
        orderId: Long,
        note: String,
        isCustomerNote: Boolean
    ): WooResult<OrderNoteEntity> {
        return coroutineEngine.withDefaultContext(T.API, this, "postOrderNote") {
            val result = wcOrderRestClient.postOrderNote(orderId, site, note, isCustomerNote)

            return@withDefaultContext if (result.isError) {
                result.asWooResult()
            } else {
                orderNotesDao.insertNotes(result.result!!)
                result.asWooResult()
            }
        }
    }

    private fun fetchOrderStatusOptions(payload: FetchOrderStatusOptionsPayload) {
        wcOrderRestClient.fetchOrderStatusOptions(payload.site)
    }

    suspend fun fetchOrderShipmentTrackings(orderId: Long, site: SiteModel): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchOrderShipmentTrackings") {
            val result = wcOrderRestClient.fetchOrderShipmentTrackings(site, orderId)
            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                // Calculate which existing records should be deleted because they no longer exist in the payload
                val existingTrackings = OrderSqlUtils.getShipmentTrackingsForOrder(
                    result.site,
                    result.orderId
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
                OnOrderChanged()
            }
        }
    }

    suspend fun addOrderShipmentTracking(payload: AddOrderShipmentTrackingPayload): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "addOrderShipmentTracking") {
            val result = with(payload) {
                wcOrderRestClient.addOrderShipmentTrackingForOrder(
                    site, orderId, tracking, isCustomProvider
                )
            }

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                result.tracking?.let { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
                OnOrderChanged()
            }
        }
    }

    suspend fun deleteOrderShipmentTracking(payload: DeleteOrderShipmentTrackingPayload): OnOrderChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "deleteOrderShipmentTracking") {
            val result = with(payload) {
                wcOrderRestClient.deleteShipmentTrackingForOrder(site, orderId, tracking)
            }

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                // Remove the record from the database and send response
                result.tracking?.let { OrderSqlUtils.deleteOrderShipmentTrackingById(it) }
                OnOrderChanged()
            }
        }
    }

    suspend fun fetchOrderShipmentProviders(
        payload: FetchOrderShipmentProvidersPayload
    ): OnOrderShipmentProvidersChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchOrderShipmentProviders") {
            val result = with(payload) {
                wcOrderRestClient.fetchOrderShipmentProviders(site, order)
            }

            return@withDefaultContext if (result.isError) {
                OnOrderShipmentProvidersChanged(0).also { it.error = result.error }
            } else {
                // Delete all providers from the db
                OrderSqlUtils.deleteOrderShipmentProvidersForSite(payload.site)

                // Add new list to the database
                val rowsAffected = result.providers.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentProvider(it) }
                OnOrderShipmentProvidersChanged(rowsAffected)
            }
        }
    }

    private fun handleFetchOrdersCompleted(payload: FetchOrdersResponsePayload) {
        val onOrderChanged: OnOrderChanged = if (payload.isError) {
            OnOrderChanged(orderError = payload.error)
        } else {
            // Clear existing uploading orders if this is a fresh fetch (loadMore = false in the original request)
            // This is the simplest way of keeping our local orders in sync with remote orders (in case of deletions,
            // or if the user manual changed some order IDs)
            if (!payload.loadedMore) {
                ordersDao.deleteOrdersForSite(payload.site.localId())
                orderNotesDao.deleteOrderNotesForSite(payload.site.remoteId())
                OrderSqlUtils.deleteOrderShipmentTrackingsForSite(payload.site)
            }

            payload.orders.forEach { ordersDao.insertOrUpdateOrder(it) }

            OnOrderChanged(payload.statusFilter, canLoadMore = payload.canLoadMore)
        }.copy(causeOfChange = FETCH_ORDERS)

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
            remoteItemIds = payload.orderSummaries.map { it.orderId },
            loadedMore = payload.loadedMore,
            canLoadMore = payload.canLoadMore,
            error = payload.error?.let { fetchError ->
                // TODO: Use the actual error type
                ListError(type = ListErrorType.GENERIC_ERROR, message = fetchError.message)
            }
        )))
    }

    private fun fetchOutdatedOrMissingOrders(site: SiteModel, fetchedSummaries: List<WCOrderSummaryModel>) {
        val fetchedSummariesIds = fetchedSummaries.map { it.orderId }
        val localOrdersForFetchedSummaries = ordersDao.getOrdersForSiteByRemoteIds(site.localId(), fetchedSummariesIds)

        val idsToFetch = outdatedOrdersIds(fetchedSummaries, localOrdersForFetchedSummaries)
            .plus(missingOrdersIds(fetchedSummariesIds, localOrdersForFetchedSummaries))

        wcOrderFetcher.fetchOrders(site = site, orderIds = idsToFetch)
    }

    private fun outdatedOrdersIds(
        fetchedSummaries: List<WCOrderSummaryModel>,
        localOrdersForSiteByRemoteIds: List<OrderEntity>
    ): List<Long> {
        val summaryModifiedDates = fetchedSummaries.associate { it.orderId to it.dateModified }

        return localOrdersForSiteByRemoteIds.filter { order ->
            order.dateModified != summaryModifiedDates[order.orderId]
        }.map(OrderEntity::orderId)
    }

    private fun missingOrdersIds(
        fetchedSummariesIds: List<Long>,
        localOrdersForSiteByRemoteIds: List<OrderEntity>
    ): List<Long> {
        return fetchedSummariesIds.minus(
            localOrdersForSiteByRemoteIds.map(OrderEntity::orderId)
        )
    }

    private fun handleFetchOrderByIdsCompleted(payload: FetchOrdersByIdsResponsePayload) {
        val onOrdersFetchedByIds = if (payload.isError) {
            OnOrdersFetchedByIds(payload.site, payload.orderIds).apply { error = payload.error }
        } else {
            OnOrdersFetchedByIds(payload.site, payload.fetchedOrders.map { it.orderId })
        }

        if (!payload.isError) {
            // Save the list of orders to the database
            payload.fetchedOrders.forEach { ordersDao.insertOrUpdateOrder(it) }

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
            OnOrderChanged(orderError = payload.error)
        } else {
            with(payload) {
                OnOrderChanged(statusFilter = statusFilter)
            }
        }.copy(causeOfChange = WCOrderAction.FETCH_ORDERS_COUNT)
        emitChange(onOrderChanged)
    }

    private suspend fun revertOrderStatus(payload: RemoteOrderPayload): OnOrderChanged {
        updateOrderStatusLocally(payload.order.orderId, payload.order.localSiteId, payload.order.status)
        return OnOrderChanged().also { it.error = payload.error }
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
}
