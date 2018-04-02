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
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import javax.inject.Singleton

@Singleton
class OrderRestClient(appContext: Context, dispatcher: Dispatcher, requestQueue: RequestQueue,
                      accessToken: AccessToken, userAgent: UserAgent)
    : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to `/wc/v2/orders` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of orders for the given WooCommerce [SiteModel].
     *
     * The number of orders fetched is defined in [WCOrderStore.NUM_ORDERS_PER_FETCH], and retrieving older
     * orders is done by passing an [offset].
     */
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
                        val orderError = OrderError((error as WPComGsonNetworkError).apiError, error.message)
                        val payload = FetchOrdersResponsePayload(orderError, site)
                        mDispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
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
            total = response.total ?: ""
            totalTax = response.total_tax ?: ""
            shippingTotal = response.shipping_total ?: ""
            paymentMethod = response.payment_method ?: ""
            paymentMethodTitle = response.payment_method_title ?: ""
            pricesIncludeTax = response.prices_include_tax

            discountTotal = response.discount_total ?: ""
            response.coupon_lines?.let { couponLines ->
                // Extract the discount codes from the coupon_lines list and store them as a comma-delimited String
                discountCodes = couponLines
                        .filter { !it.code.isNullOrEmpty() }
                        .joinToString { it.code!! }
            }

            billingFirstName = response.billing?.first_name ?: ""
            billingLastName = response.billing?.last_name ?: ""
            billingCompany = response.billing?.company ?: ""
            billingAddress1 = response.billing?.address_1 ?: ""
            billingAddress2 = response.billing?.address_2 ?: ""
            billingCity = response.billing?.city ?: ""
            billingState = response.billing?.state ?: ""
            billingPostcode = response.billing?.postcode ?: ""
            billingCountry = response.billing?.country ?: ""
            billingEmail = response.billing?.email ?: ""
            billingPhone = response.billing?.phone ?: ""

            shippingFirstName = response.shipping?.first_name ?: ""
            shippingLastName = response.shipping?.last_name ?: ""
            shippingCompany = response.shipping?.company ?: ""
            shippingAddress1 = response.shipping?.address_1 ?: ""
            shippingAddress2 = response.shipping?.address_2 ?: ""
            shippingCity = response.shipping?.city ?: ""
            shippingState = response.shipping?.state ?: ""
            shippingPostcode = response.shipping?.postcode ?: ""
            shippingCountry = response.shipping?.country ?: ""

            lineItems = response.line_items.toString()
        }
    }
}
