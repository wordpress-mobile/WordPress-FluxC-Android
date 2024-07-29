package org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundFeeLine
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundShippingLine
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject

class RefundRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    @Suppress("LongParameterList")
    suspend fun createRefund(
        site: SiteModel,
        orderId: Long,
        amount: String,
        reason: String,
        automaticRefund: Boolean,
        items: List<WCRefundModel.WCRefundItem>? = null,
        restockItems: Boolean? = null
    ): WooPayload<RefundResponse> {
        val body = mutableMapOf<String, Any>(
            "reason" to reason,
            "amount" to amount,
            "api_refund" to automaticRefund.toString()
        )
        items?.let { lineItems ->
            body["line_items"] = lineItems.associateBy { it.itemId }
            restockItems?.let { restock -> body["restock_items"] = restock }
        }
        return createRefund(site, orderId, body)
    }

    private suspend fun createRefund(
        site: SiteModel,
        orderId: Long,
        body: Map<String, Any>
    ): WooPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.pathV3
        val response = wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                body = body,
                clazz = RefundResponse::class.java
        )
        return response.toWooPayload()
    }

    suspend fun fetchRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): WooPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.refund(refundId).pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = RefundResponse::class.java
        )
        return response.toWooPayload()
    }

    suspend fun fetchAllRefunds(
        site: SiteModel,
        orderId: Long,
        page: Int,
        pageSize: Int
    ): WooPayload<Array<RefundResponse>> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<RefundResponse>::class.java,
            params = mapOf(
                "page" to page.toString(),
                "per_page" to pageSize.toString()
            )
        )
        return response.toWooPayload()
    }

    data class RefundResponse(
        @SerializedName("id") val refundId: Long,
        @SerializedName("date_created") val dateCreated: String?,
        @SerializedName("amount") val amount: String?,
        @SerializedName("reason") val reason: String?,
        @SerializedName("refunded_payment") val refundedPayment: Boolean?,
        @SerializedName("line_items") val items: List<LineItem>?,
        @SerializedName("shipping_lines") val shippingLineItems: List<WCRefundShippingLine>?,
        @SerializedName("fee_lines") val feeLineItems: List<WCRefundFeeLine>?
    )
}
