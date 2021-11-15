package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Billing
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Shipping
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.collections.MutableMap.MutableEntry

@Singleton
class OrderRestClient @Inject constructor(
    appContext: Context,
    private val dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
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
        val responseType = object : TypeToken<List<OrderDto>>() {}.type
        val params = mapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "status" to statusFilter,
                "_fields" to ORDER_FIELDS)
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderDto>? ->
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
     * Fetches orders from the API, but only requests `id` and `date_created_gmt` fields be returned. This is
     * used to determine what orders should be fetched (either existing orders that have since changed or new
     * orders not yet downloaded).
     *
     * Makes a GET call to `/wc/v3/orders` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of orders for the given WooCommerce [SiteModel].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDER_LIST] action with the resulting list of order summaries.
     *
     * @param listDescriptor The [WCOrderListDescriptor] that describes the type of list being fetched and
     * the optional parameters in effect.
     * @param offset Used to retrieve older orders
     */
    fun fetchOrderListSummaries(listDescriptor: WCOrderListDescriptor, offset: Long, requestStartTime: Calendar) {
        // If null, set the filter to the api default value of "any", which will not apply any order status filters.
        val statusFilter = listDescriptor.statusFilter.takeUnless { it.isNullOrBlank() }
                ?: WCOrderStore.DEFAULT_ORDER_STATUS

        val url = WOOCOMMERCE.orders.pathV3
        val responseType = object : TypeToken<List<OrderSummaryApiResponse>>() {}.type
        val networkPageSize = listDescriptor.config.networkPageSize
        val params = mutableMapOf(
                "per_page" to networkPageSize.toString(),
                "offset" to offset.toString(),
                "status" to statusFilter,
                "_fields" to "id,date_created_gmt,date_modified_gmt"
        ).putIfNotEmpty(
            "search" to listDescriptor.searchQuery,
                "before" to listDescriptor.beforeFilter,
                "after" to listDescriptor.afterFilter
        )

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, listDescriptor.site.siteId, params, responseType,
                { response: List<OrderSummaryApiResponse>? ->
                    val orderSummaries = response?.map {
                        orderResponseToOrderSummaryModel(it).apply { localSiteId = listDescriptor.site.id }
                    }.orEmpty()

                    val canLoadMore = orderSummaries.size == networkPageSize

                    val payload = FetchOrderListResponsePayload(
                            listDescriptor = listDescriptor,
                            orderSummaries = orderSummaries,
                            loadedMore = offset > 0,
                            canLoadMore = canLoadMore,
                            requestStartTime = requestStartTime
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderListAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrderListResponsePayload(
                            error = orderError,
                            listDescriptor = listDescriptor,
                            requestStartTime = requestStartTime
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderListAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    /**
     * Requests orders from the API that match the provided list of [remoteOrderIds] by making a GET call to
     * `/wc/v3/orders` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]).
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS_BY_IDS] action with the resulting list of orders.
     *
     * @param site The WooCommerce [SiteModel] the orders belong to
     * @param remoteOrderIds A list of remote order identifiers to fetch from the API
     */
    fun fetchOrdersByIds(site: SiteModel, remoteOrderIds: List<RemoteId>) {
        val url = WOOCOMMERCE.orders.pathV3
        val responseType = object : TypeToken<List<OrderDto>>() {}.type
        val params = mapOf(
                "per_page" to remoteOrderIds.size.toString(),
                "include" to remoteOrderIds.map { it.value }.joinToString(),
                "_fields" to ORDER_FIELDS)
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderDto>? ->
                    val orderModels = response?.map {
                        orderResponseToOrderModel(it).apply { localSiteId = site.id }
                    }.orEmpty()

                    val payload = FetchOrdersByIdsResponsePayload(
                            site = site,
                            remoteOrderIds = remoteOrderIds,
                            fetchedOrders = orderModels
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersByIdsAction(payload))
                },
                WPComErrorListener { networkError ->
                    val orderError = networkErrorToOrderError(networkError)
                    val payload = FetchOrdersByIdsResponsePayload(
                            error = orderError, site = site, remoteOrderIds = remoteOrderIds)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersByIdsAction(payload))
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
        val responseType = object : TypeToken<List<OrderDto>>() {}.type
        val params = mutableMapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "status" to WCOrderStore.DEFAULT_ORDER_STATUS,
                "_fields" to ORDER_FIELDS
        ).putIfNotEmpty("search" to searchQuery)

        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params, responseType,
                { response: List<OrderDto>? ->
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
     * Makes a GET request to `/wc/v3/orders/{remoteOrderId}` to fetch a single order by the remoteOrderId.
     *
     * @param [remoteOrderId] Unique server id of the order to fetch
     */
    suspend fun fetchSingleOrder(site: SiteModel, remoteOrderId: Long): RemoteOrderPayload {
        val url = WOOCOMMERCE.orders.id(remoteOrderId).pathV3
        val params = mapOf("_fields" to ORDER_FIELDS)

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                OrderDto::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val newModel = orderResponseToOrderModel(it).apply {
                        localSiteId = site.id
                    }
                    RemoteOrderPayload(newModel, site)
                } ?: RemoteOrderPayload(
                        OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                        WCOrderModel().apply { this.remoteOrderId = remoteOrderId },
                        site
                )
            }
            is JetpackError -> {
                val orderError = networkErrorToOrderError(response.error)
                RemoteOrderPayload(
                        orderError,
                        WCOrderModel().apply { this.remoteOrderId = remoteOrderId },
                        site
                )
            }
        }
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
     *
     * @param [filterByStatus] Nullable. If not null, consider only orders with a matching order status.
     */
    suspend fun fetchHasOrders(site: SiteModel, filterByStatus: String? = null): FetchHasOrdersResponsePayload {
        val statusFilter = if (filterByStatus.isNullOrBlank()) { "any" } else { filterByStatus }

        val url = WOOCOMMERCE.orders.pathV3

        val params = mapOf(
                "per_page" to "1",
                "offset" to "0",
                "status" to statusFilter)

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                Array<OrderDto>::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    FetchHasOrdersResponsePayload(
                            site,
                            filterByStatus,
                            it.count() > 0
                    )
                } ?: FetchHasOrdersResponsePayload(
                    OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                    site
                )
            }
            is JetpackError -> {
                var orderError = networkErrorToOrderError(response.error)
                FetchHasOrdersResponsePayload(
                        orderError,
                        site
                )
            }
        }
    }

    /**
     * Makes a PUT call to `/wc/v3/orders/<id>` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * updating the order.
     */
    private suspend fun updateOrder(
        orderToUpdate: WCOrderModel,
        site: SiteModel,
        updatePayload: Map<String, Any>
    ): RemoteOrderPayload {
        val url = WOOCOMMERCE.orders.id(orderToUpdate.remoteOrderId).pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncPutRequest(
            restClient = this,
            site = site,
            url = url,
            body = updatePayload.plus("_fields" to ORDER_FIELDS),
            clazz = OrderDto::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val newModel = orderResponseToOrderModel(it).apply {
                        id = orderToUpdate.id
                        localSiteId = orderToUpdate.localSiteId
                    }
                    RemoteOrderPayload(newModel, site)
                } ?: RemoteOrderPayload(
                    OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                    orderToUpdate,
                    site
                )
            }
            is JetpackError -> {
                val orderError = networkErrorToOrderError(response.error)
                RemoteOrderPayload(
                    orderError,
                    orderToUpdate,
                    site
                )
            }
        }
    }

    suspend fun updateOrderStatus(orderToUpdate: WCOrderModel, site: SiteModel, status: String) =
            updateOrder(orderToUpdate, site, mapOf("status" to status))

    suspend fun updateCustomerOrderNote(orderToUpdate: WCOrderModel, site: SiteModel, newNotes: String) =
            updateOrder(orderToUpdate, site, mapOf("customer_note" to newNotes))

    suspend fun updateBillingAddress(orderToUpdate: WCOrderModel, site: SiteModel, billing: Billing) =
            updateOrder(orderToUpdate, site, mapOf("billing" to billing))

    suspend fun updateShippingAddress(orderToUpdate: WCOrderModel, site: SiteModel, shipping: Shipping) =
            updateOrder(orderToUpdate, site, mapOf("shipping" to shipping))

    suspend fun updateBothOrderAddresses(
        orderToUpdate: WCOrderModel,
        site: SiteModel,
        shipping: Shipping,
        billing: Billing
    ) = updateOrder(
            orderToUpdate, site,
            mapOf("shipping" to shipping, "billing" to billing)
    )

    /**
     * Creates a "quick order," which is an empty order assigned the passed amount
     */
    suspend fun postQuickOrder(site: SiteModel, amount: String): RemoteOrderPayload {
        val jsonFee = JsonObject().also {
            it.addProperty("name", "Quick Order")
            it.addProperty("total", amount)
            it.addProperty("tax_status", "none")
            it.addProperty("tax_class", "")
        }
        val jsonFeeItems = JsonArray().also { it.add(jsonFee) }
        val params = mapOf(
                "fee_lines" to jsonFeeItems,
                "_fields" to ORDER_FIELDS
        )

        val url = WOOCOMMERCE.orders.pathV3
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                OrderDto::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val newModel = orderResponseToOrderModel(it).apply {
                        localSiteId = site.id
                    }
                    RemoteOrderPayload(newModel, site)
                } ?: RemoteOrderPayload(
                        OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                        WCOrderModel(),
                        site
                )
            }
            is JetpackError -> {
                val orderError = networkErrorToOrderError(response.error)
                RemoteOrderPayload(
                        orderError,
                        WCOrderModel(),
                        site
                )
            }
        }
    }

    /**
     * Makes a GET call to `/wc/v3/orders/<id>/notes` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving a list of notes for the given WooCommerce [SiteModel] and [WCOrderModel].
     */
    suspend fun fetchOrderNotes(
        localOrderId: Int,
        remoteOrderId: Long,
        site: SiteModel
    ): FetchOrderNotesResponsePayload {
        val url = WOOCOMMERCE.orders.id(remoteOrderId).notes.pathV3
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            mapOf(),
            Array<OrderNoteApiResponse>::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                val noteModels = response.data?.map {
                    orderNoteResponseToOrderNoteModel(it).apply {
                        localSiteId = site.id
                        this.localOrderId = localOrderId
                    }
                }.orEmpty()
                FetchOrderNotesResponsePayload(localOrderId, remoteOrderId, site, noteModels)
            }
            is JetpackError -> {
                val orderError = networkErrorToOrderError(response.error)
                FetchOrderNotesResponsePayload(orderError, site, localOrderId, remoteOrderId)
            }
        }
    }

    /**
     * Makes a POST call to `/wc/v3/orders/<id>/notes` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * saving the provide4d note for the given WooCommerce [SiteModel] and [WCOrderModel].
     */
    suspend fun postOrderNote(
        localOrderId: Int,
        remoteOrderId: Long,
        site: SiteModel,
        note: WCOrderNoteModel
    ): RemoteOrderNotePayload {
        val url = WOOCOMMERCE.orders.id(remoteOrderId).notes.pathV3

        val params = mutableMapOf(
                "note" to note.note,
                "customer_note" to note.isCustomerNote,
                "added_by_user" to true
        )

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                OrderNoteApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val newNote = orderNoteResponseToOrderNoteModel(it).apply {
                        localSiteId = site.id
                        this.localOrderId = localOrderId
                    }
                    return RemoteOrderNotePayload(localOrderId, remoteOrderId, site, newNote)
                } ?: RemoteOrderNotePayload(
                        OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                        localOrderId,
                        remoteOrderId,
                        site,
                        note
                )
            }
            is JetpackError -> {
                val noteError = networkErrorToOrderError(response.error)
                return RemoteOrderNotePayload(noteError, localOrderId, remoteOrderId, site, note)
            }
        }
    }

    /**
     * Makes a GET call to `/wc/v2/orders/<order_id>/shipment-trackings/` via the Jetpack tunnel
     * (see [JetpackTunnelGsonRequest]), retrieving a list of shipment tracking objects for a single [WCOrderModel].
     *
     * Note: This is not currently supported in v3, but will be in the short future.
     *
     */
    suspend fun fetchOrderShipmentTrackings(
        site: SiteModel,
        localOrderId: Int,
        remoteOrderId: Long
    ): FetchOrderShipmentTrackingsResponsePayload {
        val url = WOOCOMMERCE.orders.id(remoteOrderId).shipment_trackings.pathV2
        val params = mapOf("_fields" to TRACKING_FIELDS)

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                Array<OrderShipmentTrackingApiResponse>::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                val trackings = response.data?.map {
                    orderShipmentTrackingResponseToModel(it).apply {
                        localSiteId = site.id
                        this.localOrderId = localOrderId
                    }
                }.orEmpty()

                FetchOrderShipmentTrackingsResponsePayload(site, localOrderId, trackings)
            }
            is JetpackError -> {
                val trackingsError = networkErrorToOrderError(response.error)
                FetchOrderShipmentTrackingsResponsePayload(trackingsError, site, localOrderId)
            }
        }
    }

    /**
     * Posts a new Order Shipment Tracking record to the API for an order.
     *
     * Makes a POST call to save a Shipment Tracking record via the Jetpack tunnel (see [JetpackTunnelGsonRequest]).
     * The API calls for different fields depending on if the new record uses a custom provider or not, so this is
     * why there is an if-statement. Either way, the same standard [WCOrderShipmentTrackingModel] is returned.
     *
     * Note: This API does not currently support v3.
     */
    suspend fun addOrderShipmentTrackingForOrder(
        site: SiteModel,
        localOrderId: Int,
        remoteOrderId: Long,
        tracking: WCOrderShipmentTrackingModel,
        isCustomProvider: Boolean
    ): AddOrderShipmentTrackingResponsePayload {
        val url = WOOCOMMERCE.orders.id(remoteOrderId).shipment_trackings.pathV2
        val params = if (isCustomProvider) {
            mutableMapOf(
                    "custom_tracking_provider" to tracking.trackingProvider,
                    "custom_tracking_link" to tracking.trackingLink)
        } else {
            mutableMapOf("tracking_provider" to tracking.trackingProvider)
        }
        params.put("tracking_number", tracking.trackingNumber)
        params.put("date_shipped", tracking.dateShipped)

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                OrderShipmentTrackingApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val trackingModel = orderShipmentTrackingResponseToModel(it).apply {
                        this.localOrderId = localOrderId
                        localSiteId = site.id
                    }
                    AddOrderShipmentTrackingResponsePayload(site, localOrderId, remoteOrderId, trackingModel)
                } ?: AddOrderShipmentTrackingResponsePayload(
                        OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                        site,
                        localOrderId,
                        remoteOrderId,
                        tracking
                )
            }
            is JetpackError -> {
                val trackingsError = networkErrorToOrderError(response.error)
                AddOrderShipmentTrackingResponsePayload(trackingsError, site, localOrderId, remoteOrderId, tracking)
            }
        }
    }

    /**
     * Deletes a single shipment tracking record for an order.
     *
     * Makes a POST call requesting a DELETE method on `/wc/v2/orders/<order_id>/shipment_trackings/<tracking_id>/`
     * via the Jetpack tunnel (see [JetpackTunnelGsonRequest].
     *
     * Note this is currently not supported in v3, but will be in the future.
     */
    suspend fun deleteShipmentTrackingForOrder(
        site: SiteModel,
        localOrderId: Int,
        remoteOrderId: Long,
        tracking: WCOrderShipmentTrackingModel
    ): DeleteOrderShipmentTrackingResponsePayload {
        val url = WOOCOMMERCE.orders.id(remoteOrderId)
                .shipment_trackings.tracking(tracking.remoteTrackingId).pathV2
        val response = jetpackTunnelGsonRequestBuilder.syncDeleteRequest(
                this,
                site,
                url,
                OrderShipmentTrackingApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    val model = orderShipmentTrackingResponseToModel(it).apply {
                        localSiteId = site.id
                        this.localOrderId = localOrderId
                        id = tracking.id
                    }
                    DeleteOrderShipmentTrackingResponsePayload(
                            site,
                            localOrderId,
                            remoteOrderId,
                            model
                    )
                } ?: DeleteOrderShipmentTrackingResponsePayload(
                        OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                        site,
                        localOrderId,
                        remoteOrderId,
                        tracking
                )
            }
            is JetpackError -> {
                DeleteOrderShipmentTrackingResponsePayload(
                        networkErrorToOrderError(response.error),
                        site,
                        localOrderId,
                        remoteOrderId,
                        tracking
                )
            }
        }
    }

    /**
     * Fetches a list of shipment providers from the WooCommerce Shipment Tracking plugin.
     *
     * Makes a GET call to `/wc/v2/orders/<order_id>/shipment-trackings/providers/` via the Jetpack tunnel
     * (see [JetpackTunnelGsonRequest]), retrieving a list of shipment tracking provider objects. The `<order_id>`
     * argument is only needed because it is a requirement of the plugins API even though this data is not directly
     * related to shipment providers.
     */
    suspend fun fetchOrderShipmentProviders(
        site: SiteModel,
        order: WCOrderModel
    ): FetchOrderShipmentProvidersResponsePayload {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).shipment_trackings.providers.pathV2
        val params = emptyMap<String, String>()

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                JsonElement::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let {
                    try {
                        val providers = jsonResponseToShipmentProviderList(site, it)
                        FetchOrderShipmentProvidersResponsePayload(site, order, providers)
                    } catch (e: IllegalStateException) {
                        // we have at least once instance of the response being invalid json so we catch the exception
                        // https://github.com/wordpress-mobile/WordPress-FluxC-Android/issues/1331
                        AppLog.e(T.UTILS, "IllegalStateException parsing shipment provider list, response = $it")
                        FetchOrderShipmentProvidersResponsePayload(
                                OrderError(INVALID_RESPONSE, it.toString()),
                                site,
                                order
                        )
                    }
                } ?: FetchOrderShipmentProvidersResponsePayload(
                        OrderError(GENERIC_ERROR, "Success response with empty data"),
                        site,
                        order
                )
            }
            is JetpackError -> {
                val trackingError = networkErrorToOrderError(response.error)
                FetchOrderShipmentProvidersResponsePayload(trackingError, site, order)
            }
        }
    }

    private fun orderResponseToOrderSummaryModel(response: OrderSummaryApiResponse): WCOrderSummaryModel {
        return WCOrderSummaryModel().apply {
            remoteOrderId = response.id ?: 0
            dateCreated = convertDateToUTCString(response.dateCreatedGmt)
            dateModified = convertDateToUTCString(response.dateModifiedGmt)
        }
    }

    private fun orderResponseToOrderModel(response: OrderDto): WCOrderModel {
        return WCOrderModel().apply {
            remoteOrderId = response.id ?: 0
            number = response.number ?: remoteOrderId.toString()
            status = response.status ?: ""
            currency = response.currency ?: ""
            orderKey = response.order_key ?: ""
            dateCreated = convertDateToUTCString(response.date_created_gmt)
            dateModified = convertDateToUTCString(response.date_modified_gmt)
            total = response.total ?: ""
            totalTax = response.total_tax ?: ""
            shippingTotal = response.shipping_total ?: ""
            paymentMethod = response.payment_method ?: ""
            paymentMethodTitle = response.payment_method_title ?: ""
            datePaid = response.date_paid_gmt?.let { "${it}Z" } ?: ""
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
            shippingPhone = response.shipping?.phone.orEmpty()

            lineItems = response.line_items.toString()
            shippingLines = response.shipping_lines.toString()
            feeLines = response.fee_lines.toString()
            metaData = response.meta_data.toString()
        }
    }

    private fun orderNoteResponseToOrderNoteModel(response: OrderNoteApiResponse): WCOrderNoteModel {
        return WCOrderNoteModel().apply {
            remoteNoteId = response.id ?: 0
            dateCreated = response.date_created_gmt?.let { "${it}Z" } ?: ""
            note = response.note ?: ""
            isSystemNote = response.author == "system" || response.author == "WooCommerce"
            author = response.author ?: ""
            isCustomerNote = response.customer_note
        }
    }

    private fun networkErrorToOrderError(wpComError: WPComGsonNetworkError): OrderError {
        val orderErrorType = when (wpComError.apiError) {
            "rest_invalid_param" -> OrderErrorType.INVALID_PARAM
            "woocommerce_rest_shop_order_invalid_id" -> OrderErrorType.INVALID_ID
            "rest_no_route" -> OrderErrorType.PLUGIN_NOT_ACTIVE
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
            statusCount = response.total
        }
    }

    private fun orderShipmentTrackingResponseToModel(
        response: OrderShipmentTrackingApiResponse
    ): WCOrderShipmentTrackingModel {
        return WCOrderShipmentTrackingModel().apply {
            remoteTrackingId = response.tracking_id ?: ""
            trackingNumber = response.tracking_number ?: ""
            trackingProvider = response.tracking_provider ?: ""
            trackingLink = response.tracking_link ?: ""
            dateShipped = response.date_shipped ?: ""
        }
    }

    private fun convertDateToUTCString(date: String?): String =
            date?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "" // Store the date in UTC format

    private fun jsonResponseToShipmentProviderList(
        site: SiteModel,
        response: JsonElement
    ): List<WCOrderShipmentProviderModel> {
        val providers = mutableListOf<WCOrderShipmentProviderModel>()
        response.asJsonObject.entrySet()
                .forEach { countryEntry: MutableEntry<String, JsonElement> ->
                    countryEntry.value.asJsonObject.entrySet().map { carrierEntry ->
                        carrierEntry?.let { carrier ->
                            val provider = WCOrderShipmentProviderModel().apply {
                                localSiteId = site.id
                                this.country = countryEntry.key
                                this.carrierName = carrier.key
                                this.carrierLink = carrier.value.asString
                            }
                            providers.add(provider)
                        }
                    }
                }

        return providers
    }

    companion object {
        private val ORDER_FIELDS = arrayOf(
                "billing",
                "coupon_lines",
                "currency",
                "order_key",
                "customer_note",
                "date_created_gmt",
                "date_modified_gmt",
                "date_paid_gmt",
                "discount_total",
                "fee_lines",
                "id",
                "line_items",
                "number",
                "payment_method",
                "payment_method_title",
                "prices_include_tax",
                "refunds",
                "shipping",
                "shipping_lines",
                "shipping_total",
                "status",
                "total",
                "total_tax",
                "meta_data"
        ).joinToString(separator = ",")

        private val TRACKING_FIELDS = arrayOf(
                "date_shipped",
                "tracking_id",
                "tracking_link",
                "tracking_number",
                "tracking_provider"
        ).joinToString(separator = ",")
    }
}
