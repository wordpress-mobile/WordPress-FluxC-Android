@file:Suppress("DEPRECATION_ERROR")
package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wcapi.BaseWCAPIRestClient
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDtoMapper.Companion.toDto
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WCAPIOrderRestClient @Inject constructor(
        dispatcher: Dispatcher,
        @Named("regular") requestQueue: RequestQueue,
        private val wcAPIGsonRequestBuilder: WCAPIGsonRequestBuilder,
        private val orderDtoMapper: OrderDtoMapper,
        userAgent: UserAgent
) : BaseWCAPIRestClient(dispatcher, requestQueue, userAgent) {

    companion object {
        const val AUTH_KEY = "INSERT_KEY_HERE"
    }

    suspend fun createOrder(
            site: SiteModel,
            request: UpdateOrderRequest
    ): WooPayload<OrderEntity> {
        val url = site.url + "/wp-json" + WOOCOMMERCE.orders.pathV3
        val params = request.toNetworkRequest()

        val response = wcAPIGsonRequestBuilder.syncPostRequest(
                this,
                url,
                params,
                OrderDto::class.java,
                AUTH_KEY
        )

        return when (response) {
            is Success -> {
                response.data?.let { orderDto ->
                    WooPayload(orderDtoMapper.toDatabaseEntity(orderDto, site.localId()).first)
                } ?: WooPayload(
                        error = WooError(
                                type = WooErrorType.GENERIC_ERROR,
                                original = GenericErrorType.UNKNOWN,
                                message = "Success response with empty data"
                        )
                )
            }
            is Error -> {
                WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, response.error.message))
            }
        }
    }

    suspend fun deleteOrder(
            site: SiteModel,
            orderId: Long,
            trash: Boolean
    ) : WooPayload<Unit> {
        val url = site.url + "/wp-json" + WOOCOMMERCE.orders.id(orderId).pathV3
        val response = wcAPIGsonRequestBuilder.syncDeleteRequest(
                this,
                url,
                mapOf("force" to trash.toString()),
                Unit::class.java,
                AUTH_KEY
        )

        return when (response) {
            is Success -> WooPayload(Unit)
            is Error -> {
                WooPayload(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, response.error.message))
            }
        }
    }


    private fun UpdateOrderRequest.toNetworkRequest(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            status?.let { put("status", it.statusKey) }
            lineItems?.let { put("line_items", it) }
            shippingAddress?.toDto()?.let { put("shipping", it) }
            billingAddress?.toDto()?.let { put("billing", it) }
            feeLines?.let { put("fee_lines", it) }
            shippingLines?.let { put("shipping_lines", it) }
            customerNote?.let { put("customer_note", it) }
        }
    }

}
