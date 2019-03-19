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
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload
import javax.inject.Singleton

@Singleton
class OrderRestClient(
    appContext: Context,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    private val ORDER_FIELDS = "id,number,status,currency,date_created_gmt,total,total_tax,shipping_total," +
            "payment_method,payment_method_title,prices_include_tax,customer_note,discount_total," +
            "coupon_lines,refunds,billing,shipping,line_items"
    /**
     * Makes a GET call to `/wc/v3/orders` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of orders for the given WooCommerce [SiteModel].
     *
     * The number of orders fetched is defined in [WCOrderStore.NUM_ORDERS_PER_FETCH], and retrieving older
     * orders is done by passing an [offset].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS] action with the resulting list of orders.
     *
     * @param [filterByStatus] Nullable. If not null, fetch only orders with a matching order status.
     */
    fun fetchOrders(site: SiteModel, offset: Int, filterByStatus: String? = null) {
        // If null, set the filter to the api default value of "any", which will not apply any order status filters.
        val statusFilter = filterByStatus.takeUnless { it.isNullOrBlank() } ?: WCOrderStore.DEFAULT_ORDER_STATUS

        val url = WOOCOMMERCE.orders.pathV3
        val responseType = object : TypeToken<List<OrderApiResponse>>() {}.type
        val params = mapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "status" to statusFilter,
                "_fields" to ORDER_FIELDS)
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderApiResponse>? ->
                    val orderModels = response?.map {
                        orderResponseToOrderModel(it).apply { localSiteId = site.id }
                    }.orEmpty()

                    val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH

                    val payload = FetchOrdersResponsePayload(
                            site, orderModels, filterByStatus, offset > 0, canLoadMore)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrdersResponsePayload(orderError, site)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/reports/orders/totals` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of available order status options for the given WooCommerce [SiteModel].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS] action with the resulting list of order status labels.
     */
    fun fetchOrderStatusOptions(site: SiteModel) {
        val url = WOOCOMMERCE.reports.orders.totals.pathV3
        val params = emptyMap<String, String>()
        val responseType = object : TypeToken<List<OrderStatusApiResponse>>() {}.type
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderStatusApiResponse>? ->
                    val orderStatusOptions = response?.map {
                        orderStatusResponseToOrderStatusModel(it, site)
                    }.orEmpty()
                    val payload = FetchOrderStatusOptionsResponsePayload(site, orderStatusOptions)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderStatusOptionsAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrderStatusOptionsResponsePayload(orderError, site)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderStatusOptionsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/orders` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of orders for the given WooCommerce [SiteModel] matching [searchQuery]
     *
     * The number of orders fetched is defined in [WCOrderStore.NUM_ORDERS_PER_FETCH]
     *
     * Dispatches a [WCOrderAction.SEARCHED_ORDERS] action with the resulting list of orders.
     *
     * @param [searchQuery] the keyword or phrase to match orders with
     */
    fun searchOrders(site: SiteModel, searchQuery: String, offset: Int) {
        val url = WOOCOMMERCE.orders.pathV3
        val responseType = object : TypeToken<List<OrderApiResponse>>() {}.type
        val params = mapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "status" to WCOrderStore.DEFAULT_ORDER_STATUS,
                "search" to searchQuery,
                "_fields" to ORDER_FIELDS)
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderApiResponse>? ->
                    val orderModels = response?.map {
                        orderResponseToOrderModel(it).apply { localSiteId = site.id }
                    }.orEmpty()

                    val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                    val nextOffset = offset + orderModels.size
                    val payload = SearchOrdersResponsePayload(site, searchQuery, canLoadMore, nextOffset, orderModels)
                    dispatcher.dispatch(WCOrderActionBuilder.newSearchedOrdersAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = SearchOrdersResponsePayload(orderError, site, searchQuery)
                    dispatcher.dispatch(WCOrderActionBuilder.newSearchedOrdersAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET request to `/wc/v3/orders/{remoteOrderId}` to fetch a single order by the remoteOrderId
     *
     * Dispatches a [WCOrderAction.FETCHED_SINGLE_ORDER] action with the result
     *
     * @param [remoteOrderId] Unique server id of the order to fetch
     */
    fun fetchSingleOrder(site: SiteModel, remoteOrderId: Long) {
        val url = WOOCOMMERCE.orders.id(remoteOrderId).pathV3
        val responseType = object : TypeToken<OrderApiResponse>() {}.type
        val params = mapOf("_fields" to ORDER_FIELDS)
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: OrderApiResponse? ->
                    response?.let {
                        val newModel = orderResponseToOrderModel(it).apply {
                            localSiteId = site.id
                        }
                        val payload = RemoteOrderPayload(newModel, site)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchedSingleOrderAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = RemoteOrderPayload(
                            orderError,
                            WCOrderModel().apply { this.remoteOrderId = remoteOrderId },
                            site
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedSingleOrderAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/reports/orders/totals` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving count of orders for the given WooCommerce [SiteModel], broken down by order status.
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS_COUNT] action with the resulting count.
     *
     * @param [filterByStatus] The order status to return a count for
     */
    fun fetchOrderCount(site: SiteModel, filterByStatus: String) {
        val url = WOOCOMMERCE.reports.orders.totals.pathV3
        val params = mapOf("status" to filterByStatus)
        val responseType = object : TypeToken<List<OrderCountApiResponse>>() {}.type
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderCountApiResponse>? ->
                    val total = response?.find { it.slug == filterByStatus }?.total

                    total?.let {
                        val payload = FetchOrdersCountResponsePayload(site, filterByStatus, it)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersCountAction(payload))
                    } ?: run {
                        val orderError = OrderError(OrderErrorType.ORDER_STATUS_NOT_FOUND)
                        val payload = FetchOrdersCountResponsePayload(orderError, site, filterByStatus)
                        dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersCountAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrdersCountResponsePayload(orderError, site, filterByStatus)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersCountAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a GET request to `/wc/v3/orders` for a single order of a specific type (or any type) in order to
     * determine if there are any orders in the store.
     *
     * Dispatches a [WCOrderAction.FETCHED_HAS_ORDERS] action with the result
     *
     * @param [filterByStatus] Nullable. If not null, consider only orders with a matching order status.
     */
    fun fetchHasOrders(site: SiteModel, filterByStatus: String? = null) {
        val statusFilter = if (filterByStatus.isNullOrBlank()) { "any" } else { filterByStatus!! }

        val url = WOOCOMMERCE.orders.pathV3
        val responseType = object : TypeToken<List<OrderApiResponse>>() {}.type
        val params = mapOf(
                "per_page" to "1",
                "offset" to "0",
                "status" to statusFilter)
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderApiResponse>? ->
                    val orderModels = response?.map {
                        orderResponseToOrderModel(it).apply { localSiteId = site.id }
                    }.orEmpty()
                    val hasOrders = orderModels.isNotEmpty()
                    val payload = FetchHasOrdersResponsePayload(
                            site, filterByStatus, hasOrders)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedHasOrdersAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchHasOrdersResponsePayload(orderError, site)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedHasOrdersAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a PUT call to `/wc/v3/orders/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * updating the status for the given [order] to [status].
     *
     * Dispatches a [WCOrderAction.UPDATED_ORDER_STATUS] with the updated [WCOrderModel].
     *
     * Possible non-generic errors:
     * [OrderErrorType.INVALID_PARAM] if the [status] is not a valid order status on the server
     * [OrderErrorType.INVALID_ID] if an order by this id was not found on the server
     */
    fun updateOrderStatus(order: WCOrderModel, site: SiteModel, status: String) {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).pathV3
        val params = mapOf("status" to status)
        val request = JetpackTunnelGsonRequest.buildPutRequest(url, site.siteId, params, OrderApiResponse::class.java,
                { response: OrderApiResponse? ->
                    response?.let {
                        val newModel = orderResponseToOrderModel(it).apply {
                            id = order.id
                            localSiteId = site.id
                        }
                        val payload = RemoteOrderPayload(newModel, site)
                        dispatcher.dispatch(WCOrderActionBuilder.newUpdatedOrderStatusAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = RemoteOrderPayload(orderError, order, site)
                    dispatcher.dispatch(WCOrderActionBuilder.newUpdatedOrderStatusAction(payload))
                })
        add(request)
    }

    /**
     * Makes a GET call to `/wc/v3/orders/<id>/notes` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of notes for the given WooCommerce [SiteModel] and [WCOrderModel].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDER_NOTES] action with the resulting list of order notes.
     */
    fun fetchOrderNotes(order: WCOrderModel, site: SiteModel) {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).notes.pathV3
        val responseType = object : TypeToken<List<OrderNoteApiResponse>>() {}.type
        val params = emptyMap<String, String>()
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderNoteApiResponse>? ->
                    val noteModels = response?.map {
                        orderNoteResponseToOrderNoteModel(it).apply {
                            localSiteId = site.id
                            localOrderId = order.id
                        }
                    }.orEmpty()
                    val payload = FetchOrderNotesResponsePayload(order, site, noteModels)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderNotesAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrderNotesResponsePayload(orderError, site, order)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderNotesAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Makes a POST call to `/wc/v3/orders/<id>/notes` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * saving the provide4d note for the given WooCommerce [SiteModel] and [WCOrderModel].
     *
     * Dispatches a [WCOrderAction.POSTED_ORDER_NOTE] action with the resulting saved version of the order note.
     */
    fun postOrderNote(order: WCOrderModel, site: SiteModel, note: WCOrderNoteModel) {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).notes.pathV3

        val params = mutableMapOf(
                "note" to note.note,
                "customer_note" to note.isCustomerNote,
                "added_by_user" to true
        )
        val request = JetpackTunnelGsonRequest.buildPostRequest(
                url, site.siteId, params, OrderNoteApiResponse::class.java,
                { response: OrderNoteApiResponse? ->
                    response?.let {
                        val newNote = orderNoteResponseToOrderNoteModel(it).apply {
                            localSiteId = site.id
                            localOrderId = order.id
                        }
                        val payload = RemoteOrderNotePayload(order, site, newNote)
                        dispatcher.dispatch(WCOrderActionBuilder.newPostedOrderNoteAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val noteError = networkErrorToOrderError(networkError)
                    val payload = RemoteOrderNotePayload(noteError, order, site, note)
                    dispatcher.dispatch(WCOrderActionBuilder.newPostedOrderNoteAction(payload))
                })
        add(request)
    }

    private fun orderResponseToOrderModel(response: OrderApiResponse): WCOrderModel {
        return WCOrderModel().apply {
            remoteOrderId = response.id ?: 0
            number = response.number ?: remoteOrderId.toString()
            status = response.status ?: ""
            currency = response.currency ?: ""
            dateCreated = response.date_created_gmt?.let { "${it}Z" } ?: "" // Store the date in UTC format
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

    private fun orderNoteResponseToOrderNoteModel(response: OrderNoteApiResponse): WCOrderNoteModel {
        return WCOrderNoteModel().apply {
            remoteNoteId = response.id ?: 0
            dateCreated = response.date_created_gmt?.let { "${it}Z" } ?: ""
            note = response.note ?: ""
            isSystemNote = response.author == "system"
            isCustomerNote = response.customer_note
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

    private fun orderStatusResponseToOrderStatusModel(
        response: OrderStatusApiResponse,
        site: SiteModel
    ): WCOrderStatusModel {
        return WCOrderStatusModel().apply {
            localSiteId = site.id
            statusKey = response.slug ?: ""
            label = response.name ?: ""
        }
    }
}
