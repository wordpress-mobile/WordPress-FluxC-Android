package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Singleton

@Singleton
class CustomerRestClient(
    appContext: Context,
    private val requestBuilder: JetpackTunnelGsonRequestBuilder,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to `/wc/v3/customers/[remoteCustomerId]` to fetch a single customer
     *
     * @param [remoteCustomerId] Unique server id of the customer to fetch
     */
    suspend fun fetchSingleCustomer(site: SiteModel, remoteCustomerId: Long): WooPayload<CustomerApiResponse> {
        val url = WOOCOMMERCE.customers.id(remoteCustomerId).pathV3

        val response = requestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                CustomerApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }
}
