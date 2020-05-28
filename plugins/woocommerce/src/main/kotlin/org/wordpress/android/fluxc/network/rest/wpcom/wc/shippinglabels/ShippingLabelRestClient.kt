package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Singleton

@Singleton
class ShippingLabelRestClient
constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchShippingLabelsForOrder(
        orderId: Long,
        site: SiteModel
    ): WooPayload<ShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                ShippingLabelApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun refundShippingLabelForOrder(
        site: SiteModel,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WooPayload<ShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.order(orderId).shippingLabelId(remoteShippingLabelId).refund.pathV1

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                emptyMap(),
                ShippingLabelApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun printShippingLabel(
        site: SiteModel,
        paperSize: String,
        remoteShippingLabelId: Long
    ): WooPayload<PrintShippingLabelApiResponse> {
        val url = WOOCOMMERCE.connect.label.print.pathV1
        val params = mapOf(
                "paper_size" to paperSize,
                "label_id_csv" to remoteShippingLabelId.toString(),
                "caption_csv" to "",
                "json" to "true"
        )

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                PrintShippingLabelApiResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class PrintShippingLabelApiResponse(
        val mimeType: String,
        val b64Content: String,
        val success: Boolean
    )
}
