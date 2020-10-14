package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
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

    suspend fun verifyAddress(
        site: SiteModel,
        address: ShippingLabelAddress,
        type: ShippingLabelAddress.Type
    ): WooPayload<VerifyAddressResponse> {
        val url = WOOCOMMERCE.connect.normalize_address.pathV1
        val params = mapOf(
                "address" to Gson().toJson(address).toString(),
                "type" to type.name
        )

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                VerifyAddressResponse::class.java
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

    data class VerifyAddressResponse(
        @SerializedName("success") val isSuccess: Boolean,
        @SerializedName("is_trivial_normalization") val isTrivialNormalization: Boolean,
        @SerializedName("normalized") val suggestedAddress : ShippingLabelAddress?,
        @SerializedName("field_errors") val error: Error?
    ) {
        data class Error(
            @SerializedName("general") val message: String,
            @SerializedName("address") val address: String
        )
    }
}
