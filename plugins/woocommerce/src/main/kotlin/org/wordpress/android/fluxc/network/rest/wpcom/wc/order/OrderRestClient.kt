package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import javax.inject.Singleton

@Singleton
class OrderRestClient(appContext: Context, dispatcher: Dispatcher, requestQueue: RequestQueue,
                      accessToken: AccessToken, userAgent: UserAgent)
    : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    fun fetchOrders(site: SiteModel, offset: Int) {
        val url = WOOCOMMERCE.orders.pathV2
        val responseType = object : TypeToken<List<OrderApiResponse>>() {}.type
        val params = mapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString())
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderApiResponse>? ->
                    run {
                        val orderModels = response?.map {
                            orderResponseToOrderModel(it).apply { localSiteId = site.id }
                        }.orEmpty()

                        val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                        val payload = FetchOrdersResponsePayload(site, orderModels, offset > 0, canLoadMore)
                        mDispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                    }
                },
                BaseErrorListener { error ->
                    run {
                        // TODO
                    }
                })
        add(request)
    }
    private fun orderResponseToOrderModel(response: OrderApiResponse): WCOrderModel {
        return WCOrderModel().apply {
            remoteOrderId = response.number ?: 0
            status = response.status ?: ""
            currency = response.currency ?: ""
            dateCreated = "${response.date_created_gmt}Z" // Store the date in UTC format
            total = response.total ?: 0F
            billingFirstName = response.billing?.first_name ?: ""
            billingLastName = response.billing?.last_name ?: ""
        }
    }
}
