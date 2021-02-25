package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCCustomerActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCCustomerModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error.networkErrorToCustomerError
import org.wordpress.android.fluxc.store.WCCustomerStore.RemoteCustomerPayload
import javax.inject.Singleton

@Singleton
class CustomerRestClient(
    appContext: Context,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to `/wc/v3/customers/[remoteCustomerId]` to fetch a single customer
     *
     * Dispatches a WCCustomerAction.FETCH_SINGLE_CUSTOMER action with the result
     *
     * @param [remoteCustomerId] Unique server id of the customer to fetch
     */
    fun fetchSingleCustomer(site: SiteModel, remoteCustomerId: Long) {
        val url = WOOCOMMERCE.customers.id(remoteCustomerId).pathV3
        val responseType = object : TypeToken<CustomerApiResponse>() {}.type
        val params = emptyMap<String, String>()
        val request = JetpackTunnelGsonRequest.buildGetRequest(url,
                site.siteId,
                params,
                responseType,
                { response: CustomerApiResponse? ->
                    response?.let {
                        val newModel = it.asCustomerModel().apply { localSiteId = site.id }
                        val payload = RemoteCustomerPayload(newModel, site)
                        dispatcher.dispatch(WCCustomerActionBuilder.newFetchedSingleCustomerAction(payload))
                    }
                },
                { networkError ->
                    val customerError = networkErrorToCustomerError(networkError)
                    val payload = RemoteCustomerPayload(
                            customerError,
                            WCCustomerModel().apply { this.remoteCustomerId = remoteCustomerId },
                            site
                    )
                    dispatcher.dispatch(WCCustomerActionBuilder.newFetchedSingleCustomerAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }
}
