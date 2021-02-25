package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCCustomerAction
import org.wordpress.android.fluxc.action.WCCustomerAction.FETCHED_SINGLE_CUSTOMER
import org.wordpress.android.fluxc.action.WCCustomerAction.FETCH_SINGLE_CUSTOMER
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error.CustomerError
import org.wordpress.android.fluxc.persistence.CustomerSqlUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerStore @Inject constructor(
    dispatcher: Dispatcher,
    private val restClient: CustomerRestClient
) : Store(dispatcher) {
    // region request payloads
    class FetchSingleCustomerPayload(
        var site: SiteModel,
        var remoteCustomerId: Long
    ) : Payload<BaseNetworkError>()
    // endregion

    // region response payloads
    class RemoteCustomerPayload(
        val customer: WCCustomerModel,
        val site: SiteModel
    ) : Payload<CustomerError>() {
        constructor(
            error: CustomerError,
            customer: WCCustomerModel,
            site: SiteModel
        ) : this(customer, site) {
            this.error = error
        }
    }
    // endregion

    // region OnChanged events
    class OnCustomerChanged(
        var rowsAffected: Int,
        /**
         * Only set for fetching or deleting a single customer
         */
        var remoteCustomerId: Long = 0L,
        var canLoadMore: Boolean = false
    ) : OnChanged<CustomerError>() {
        var causeOfChange: WCCustomerAction? = null
    }
    // endregion

    // region action handling
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCCustomerAction ?: return

        when (actionType) {
            // region request action handling
            FETCH_SINGLE_CUSTOMER -> fetchSingleCustomer(action.payload as FetchSingleCustomerPayload)
            // endregion

            // region response handling
            FETCHED_SINGLE_CUSTOMER -> handleFetchSingleConsumerCompleted(action.payload as RemoteCustomerPayload)
            // endregion
        }
    }
    // endregion

    // region request handling
    private fun fetchSingleCustomer(payload: FetchSingleCustomerPayload) {
        with(payload) { restClient.fetchSingleCustomer(site, remoteCustomerId) }
    }
    // endregion

    // region response handling
    private fun handleFetchSingleConsumerCompleted(payload: RemoteCustomerPayload) {
        val onCustomerChanged: OnCustomerChanged

        if (payload.isError) {
            onCustomerChanged = OnCustomerChanged(0).also {
                it.error = payload.error
                it.remoteCustomerId = payload.customer.remoteCustomerId
            }
        } else {
            val rowsAffected = CustomerSqlUtils.insertOrUpdateCustomer(payload.customer)
            onCustomerChanged = OnCustomerChanged(rowsAffected).also {
                it.remoteCustomerId = payload.customer.remoteCustomerId
            }
        }

        onCustomerChanged.causeOfChange = FETCH_SINGLE_CUSTOMER
        emitChange(onCustomerChanged)
    }
    // endregion

    override fun onRegister() = AppLog.d(T.API, "WCCustomerStore onRegister")
}
