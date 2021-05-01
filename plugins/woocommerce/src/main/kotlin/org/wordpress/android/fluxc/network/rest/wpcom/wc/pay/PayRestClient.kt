package org.wordpress.android.fluxc.network.rest.wpcom.wc.pay

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.CUSTOM_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.CaptureFailed
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.MissingOrder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.OtherError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.UncapturablePayment
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.WCPayServerError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PayRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchConnectionToken(site: SiteModel): WooPayload<ConnectionTokenApiResponse, WooError> {
        val url = WOOCOMMERCE.payments.connection_tokens.pathV3
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                mapOf(),
                ConnectionTokenApiResponse::class.java
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

    suspend fun capturePayment(
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ): WooPayload<CapturePaymentApiResponse, CaptureTerminalPaymentError> {
        // TODO cardreader add error handling + introduce tests for both happy and error paths
        val url = WOOCOMMERCE.payments.orders.id(orderId).capture_terminal_payment.pathV3
        val params = mapOf(
                "payment_intent_id" to paymentId
        )
        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                this,
                site,
                url,
                params,
                CapturePaymentApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(
                        when (response.error.apiError) {
                            "wcpay_missing_order" -> MissingOrder(
                                    original = response.error.type,
                                    message = response.error.message
                            )
                            "wcpay_payment_uncapturable" -> UncapturablePayment(
                                    original = response.error.type,
                                    message = response.error.message
                            )
                            "wcpay_capture_error" -> CaptureFailed(
                                    original = response.error.type,
                                    message = response.error.message
                            )
                            "wcpay_server_error" -> WCPayServerError(
                                    original = response.error.type,
                                    message = response.error.message
                            )
                            else -> OtherError(response.error.toWooError())
                        }
                )
            }
        }
    }

    sealed class CaptureTerminalPaymentError(
        type: WooErrorType = CUSTOM_ERROR,
        original: GenericErrorType,
        message: String? = null
    ) : WooError(type, original, message) {
        class MissingOrder(
            original: GenericErrorType,
            message: String? = null
        ) : CaptureTerminalPaymentError(original = original, message = message)

        class UncapturablePayment(
            original: GenericErrorType,
            message: String? = null
        ) : CaptureTerminalPaymentError(original = original, message = message)

        class CaptureFailed(
            original: GenericErrorType,
            message: String? = null
        ) : CaptureTerminalPaymentError(original = original, message = message)

        class WCPayServerError(
            original: GenericErrorType,
            message: String? = null
        ) : CaptureTerminalPaymentError(original = original, message = message)

        data class OtherError(val error: WooError) : CaptureTerminalPaymentError(
                error.type,
                error.original,
                error.message
        )
    }
}
