package org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.store.RefundsStore.RefundsPayload
import org.wordpress.android.fluxc.store.toRefundsError
import javax.inject.Singleton

@Singleton
class RefundsRestClient
constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun createRefund(
        site: SiteModel,
        orderId: Long,
        amount: String,
        reason: String = "",
        automaticRefund: Boolean = false,
        partialRefundLineItems: List<LineItem> = emptyList()
    ): RefundsPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.pathV3

        val params = mapOf(
            "amount" to amount,
            "reason" to reason,
            "api_refund" to automaticRefund.toString(),
            "line_items" to partialRefundLineItems.toString()
        )
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                RefundResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                RefundsPayload(response.data)
            }
            is JetpackError -> {
                RefundsPayload(response.error.toRefundsError())
            }
        }
    }

    suspend fun fetchRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): RefundsPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.refund(refundId).pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                RefundResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                RefundsPayload(response.data)
            }
            is JetpackError -> {
                RefundsPayload(response.error.toRefundsError())
            }
        }
    }

    suspend fun fetchAllRefunds(
        site: SiteModel,
        orderId: Long
    ): RefundsPayload<Array<RefundResponse>> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                Array<RefundResponse>::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                RefundsPayload(response.data)
            }
            is JetpackError -> {
                RefundsPayload(response.error.toRefundsError())
            }
        }
    }

    data class RefundResponse(
        @SerializedName("id") val refundId: Long,
        @SerializedName("date_created") val dateCreated: String?,
        @SerializedName("amount") val amount: String?,
        @SerializedName("reason") val reason: String?,
        @SerializedName("refunded_payment") val refundedPayment: Boolean?,
        @SerializedName("line_items") val items: List<LineItem>
    )
}
