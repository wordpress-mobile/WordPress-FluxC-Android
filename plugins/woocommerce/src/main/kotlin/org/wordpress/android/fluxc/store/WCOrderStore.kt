package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
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
            var loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchOrdersResponsePayload(
            var site: SiteModel,
            var orders: List<WCOrderModel> = emptyList(),
            var loadedMore: Boolean = false,
            var canLoadMore: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) { this.error = error }
    }

    class OrderError(val type: OrderErrorType? = null, val message: String = "") : OnChangedError {
        constructor(type: String, message: String): this(OrderErrorType.fromString(type), message)
    }

    enum class OrderErrorType {
        GENERIC_ERROR;

        companion object {
            private val reverseMap = OrderErrorType.values().associateBy(OrderErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    override fun onRegister() {
        AppLog.d(T.API, "WCOrderStore onRegister")
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCOrderAction ?: return
        when (actionType) {
            WCOrderAction.FETCH_ORDERS -> fetchOrders(action.payload as FetchOrdersPayload)
            WCOrderAction.FETCHED_ORDERS -> handledFetchOrdersCompleted(action.payload as FetchOrdersResponsePayload)
        }
    }

    private fun fetchOrders(payload: FetchOrdersPayload) {
        // TODO: Handle loadMore
        wcOrderRestClient.fetchOrders(payload.site, 0)
    }

    private fun handledFetchOrdersCompleted(payload: FetchOrdersResponsePayload) {
        // TODO
    }
}
