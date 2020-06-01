package org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
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
class GatewayRestClient
constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchGateway(
        site: SiteModel,
        gatewayId: String
    ): WooPayload<GatewayResponse> {
        val url = WOOCOMMERCE.payment_gateways.gateway(gatewayId).pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                GatewayResponse::class.java
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

    suspend fun fetchAllGateways(
        site: SiteModel
    ): WooPayload<Array<GatewayResponse>> {
        val url = WOOCOMMERCE.payment_gateways.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                emptyMap(),
                Array<GatewayResponse>::class.java
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

    data class GatewayResponse(
        @SerializedName("id") val gatewayId: String,
        @SerializedName("title") val title: String?,
        @SerializedName("description") val description: String?,
        @SerializedName("order") val order: String?,
        @SerializedName("enabled") val enabled: Boolean?,
        @SerializedName("method_title") val methodTitle: String?,
        @SerializedName("method_description") val methodDescription: String?,
        @SerializedName("method_supports") val features: List<String>?
    )
}
