package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import javax.inject.Singleton

@Singleton
class OrderRestClient(
    appContext: Context,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
)
    : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to `/wc/v2/orders` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of orders for the given WooCommerce [SiteModel].
     *
     * The number of orders fetched is defined in [WCOrderStore.NUM_ORDERS_PER_FETCH], and retrieving older
     * orders is done by passing an [offset].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS] action with the resulting list of orders.
     */
    fun fetchOrders(site: SiteModel, offset: Int) {
        val url = WOOCOMMERCE.orders.pathV2
        val responseType = object : TypeToken<List<OrderApiResponse>>() {}.type
        val params = mapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString())
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderApiResponse>? ->
                    val orderModels = response?.map {
                        orderResponseToOrderModel(it).apply { localSiteId = site.id }
                    }.orEmpty()

                    val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                    val payload = FetchOrdersResponsePayload(site, orderModels, offset > 0, canLoadMore)
                    mDispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                },
                BaseErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError as WPComGsonNetworkError)
                    val payload = FetchOrdersResponsePayload(orderError, site)
                    mDispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                })
        add(request)
    }

    /**
     * Makes a PUT call to `/wc/v2/orders/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * updating the status for the given [order] to [status].
     *
     * Dispatches a [WCOrderAction.UPDATED_ORDER_STATUS] with the updated [WCOrderModel].
     *
     * Possible non-generic errors:
     * [OrderErrorType.INVALID_PARAM] if the [status] is not a valid order status on the server
     * [OrderErrorType.INVALID_ID] if an order by this id was not found on the server
     */
    fun updateOrderStatus(order: WCOrderModel, site: SiteModel, status: String) {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).pathV2
        val params = mapOf("status" to status)
        val request = JetpackTunnelGsonRequest.buildPutRequest(url, site.siteId, params, OrderApiResponse::class.java,
                { response: OrderApiResponse? ->
                    response?.let {
                        val newModel = orderResponseToOrderModel(it).apply {
                            id = order.id
                            localSiteId = site.id
                        }
                        val payload = RemoteOrderPayload(newModel, site)
                        mDispatcher.dispatch(WCOrderActionBuilder.newUpdatedOrderStatusAction(payload))
                    }
                },
                BaseErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError as WPComGsonNetworkError)
                    val payload = RemoteOrderPayload(orderError, order, site)
                    mDispatcher.dispatch(WCOrderActionBuilder.newUpdatedOrderStatusAction(payload))
                })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v2/orders/<id>/notes` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of notes for the given WooCommerce [SiteModel] and [WCOrderModel].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDER_NOTES] action with the resulting list of order notes.
     */
    fun fetchOrderNotes(order: WCOrderModel, site: SiteModel) {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).notes.pathV2
        val responseType = object : TypeToken<List<OrderNotesApiResponse>>() {}.type
        val params = emptyMap<String, String>()
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderNotesApiResponse>? ->
                    val noteModels = response?.map {
                        orderNotesResponseToOrderNotesModel(it).apply {
                            localSiteId = site.id
                            localOrderId = order.id
                        }
                    }.orEmpty()
                    val payload = FetchOrderNotesResponsePayload(order, site, noteModels)
                    mDispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderNotesAction(payload))
                },
                BaseErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError as WPComGsonNetworkError)
                    val payload = FetchOrderNotesResponsePayload(orderError, site, order)
                    mDispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderNotesAction(payload))
                })
        add(request)
    }

    private fun orderResponseToOrderModel(response: OrderApiResponse): WCOrderModel {
        return WCOrderModel().apply {
            remoteOrderId = response.id ?: 0
            number = response.number ?: remoteOrderId.toString()
            status = response.status ?: ""
            currency = response.currency ?: ""
            dateCreated = "${response.date_created_gmt}Z" // Store the date in UTC format
            total = response.total ?: ""
            totalTax = response.total_tax ?: ""
            shippingTotal = response.shipping_total ?: ""
            paymentMethod = response.payment_method ?: ""
            paymentMethodTitle = response.payment_method_title ?: ""
            pricesIncludeTax = response.prices_include_tax

            customerNote = response.customer_note ?: ""

            discountTotal = response.discount_total ?: ""
            response.coupon_lines?.let { couponLines ->
                // Extract the discount codes from the coupon_lines list and store them as a comma-delimited String
                discountCodes = couponLines
                        .filter { !it.code.isNullOrEmpty() }
                        .joinToString { it.code!! }
            }

            response.refunds?.let { refunds ->
                // Extract the individual refund totals from the refunds list and store their sum as a Double
                refundTotal = refunds.sumByDouble { it.total?.toDoubleOrNull() ?: 0.0 }
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

    private fun orderNotesResponseToOrderNotesModel(response: OrderNotesApiResponse): WCOrderNoteModel {
        return WCOrderNoteModel().apply {
            remoteNoteId = response.id ?: 0
            dateCreated = "${response.date_created_gmt}Z"
            note = response.note ?: ""
            customerNote = response.customer_note
        }
    }

    private fun networkErrorToOrderError(wpComError: WPComGsonNetworkError): OrderError {
        val orderErrorType = when (wpComError.apiError) {
            "rest_invalid_param" -> OrderErrorType.INVALID_PARAM
            "woocommerce_rest_shop_order_invalid_id" -> OrderErrorType.INVALID_ID
            else -> OrderErrorType.fromString(wpComError.apiError)
        }
        return OrderError(orderErrorType, wpComError.message)
    }
}
