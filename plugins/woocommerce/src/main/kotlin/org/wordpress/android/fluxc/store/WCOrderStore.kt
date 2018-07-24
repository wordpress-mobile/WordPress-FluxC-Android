package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCOrderStore @Inject constructor(dispatcher: Dispatcher, private val wcOrderRestClient: OrderRestClient)
    : Store(dispatcher) {
    companion object {
        const val NUM_ORDERS_PER_FETCH = 50
    }

    class FetchOrdersPayload(
        var site: SiteModel,
        var loadMore: Boolean = false,
        var status: String = "any"
    ) : Payload<BaseNetworkError>()

    class FetchOrdersResponsePayload(
        var site: SiteModel,
        var orders: List<WCOrderModel> = emptyList(),
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
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
        constructor(error: OrderError, order: WCOrderModel, site: SiteModel, note: WCOrderNoteModel)
                : this(order, site, note) { this.error = error }
    }

    class OrderError(val type: OrderErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class OrderErrorType {
        INVALID_PARAM,
        INVALID_ID,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = OrderErrorType.values().associateBy(OrderErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    // OnChanged events
    class OnOrderChanged(var rowsAffected: Int, var canLoadMore: Boolean = false) : OnChanged<OrderError>() {
        var causeOfChange: WCOrderAction? = null
    }

    override fun onRegister() = AppLog.d(T.API, "WCOrderStore onRegister")

    /**
     * Given a [SiteModel] and optional statuses, returns all orders for that site matching any of those statuses.
     *
     * The default WooCommerce statuses are defined in [OrderStatus]. Custom order statuses are also supported.
     */
    fun getOrdersForSite(site: SiteModel, vararg status: String): List<WCOrderModel> =
            OrderSqlUtils.getOrdersForSite(site, status = status.asList())

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

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCOrderAction ?: return
        when (actionType) {
            WCOrderAction.FETCH_ORDERS -> fetchOrders(action.payload as FetchOrdersPayload)
            WCOrderAction.UPDATE_ORDER_STATUS -> updateOrderStatus(action.payload as UpdateOrderStatusPayload)
            WCOrderAction.FETCH_ORDER_NOTES -> fetchOrderNotes(action.payload as FetchOrderNotesPayload)
            WCOrderAction.POST_ORDER_NOTE -> postOrderNote(action.payload as PostOrderNotePayload)
            WCOrderAction.FETCHED_ORDERS -> handleFetchOrdersCompleted(action.payload as FetchOrdersResponsePayload)
            WCOrderAction.UPDATED_ORDER_STATUS -> handleUpdateOrderStatusCompleted(action.payload as RemoteOrderPayload)
            WCOrderAction.FETCHED_ORDER_NOTES ->
                handleFetchOrderNotesCompleted(action.payload as FetchOrderNotesResponsePayload)
            WCOrderAction.POSTED_ORDER_NOTE -> handlePostOrderNoteCompleted(action.payload as RemoteOrderNotePayload)
        }
    }

    private fun fetchOrders(payload: FetchOrdersPayload) {
        val offset = if (payload.loadMore) {
            OrderSqlUtils.getOrdersForSite(payload.site).size
        } else {
            0
        }
        wcOrderRestClient.fetchOrders(payload.site, offset, payload.status)
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
            }

            val rowsAffected = payload.orders.sumBy { OrderSqlUtils.insertOrUpdateOrder(it) }

            onOrderChanged = OnOrderChanged(rowsAffected, payload.canLoadMore)
        }

        onOrderChanged.causeOfChange = WCOrderAction.FETCH_ORDERS

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
}
