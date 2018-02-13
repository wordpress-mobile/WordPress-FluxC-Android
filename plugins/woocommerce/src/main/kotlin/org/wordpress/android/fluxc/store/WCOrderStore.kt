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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCOrderStore @Inject constructor(dispatcher: Dispatcher, private val wcOrderRestClient: OrderRestClient)
    : Store(dispatcher) {
    class FetchOrdersPayload(
            var site: SiteModel,
            var loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    // TODO: Use custom OrderError parameter
    class FetchOrdersResponsePayload(
            var site: SiteModel,
            var orders: List<WCOrderModel> = emptyList(),
            var loadedMore: Boolean = false,
            var canLoadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    override fun onRegister() {
        AppLog.d(T.API, "WCOrderStore onRegister")
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCOrderAction ?: return
        when (actionType) {
            WCOrderAction.FETCH_ORDERS -> {} // TODO
            WCOrderAction.FETCHED_ORDERS -> {} // TODO
        }
        // TODO
    }
}
