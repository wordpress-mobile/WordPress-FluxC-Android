package org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.RefundsStore.RefundsPayload
import org.wordpress.android.fluxc.store.toRefundsError
import javax.inject.Singleton

@Singleton
class RefundsRestClient
constructor(
    dispatcher: Dispatcher,
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun createRefund(
        order: WCOrderModel,
        reason: String = "",
        automaticRefund: Boolean = true,
        partialRefundLineItems: List<LineItem> = emptyList(),
        forced: Boolean = true
    ): RefundsPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(order.remoteOrderId).refund.pathV3

        val params = mapOf(
            "amount" to order.total,
            "reason" to reason,
            "api_refund" to automaticRefund.toString(),
            "line_items" to partialRefundLineItems.toString()
        )
        val response = wpComGsonRequestBuilder.syncGetRequest(
                this,
                url,
                params,
                RefundResponse::class.java,
                enableCaching = false,
                forced = forced
        )
        return when (response) {
            is Success -> {
                RefundsPayload(response.data)
            }
            is Error -> {
                RefundsPayload(response.error.toRefundsError())
            }
        }
    }

    data class RefundResponse(
        @SerializedName("refunded_payment") val refundedPayment: Boolean?
    )
}
