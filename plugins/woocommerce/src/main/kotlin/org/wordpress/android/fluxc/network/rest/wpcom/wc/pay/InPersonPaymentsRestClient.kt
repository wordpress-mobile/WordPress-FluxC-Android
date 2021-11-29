package org.wordpress.android.fluxc.network.rest.wpcom.wc.pay

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentError
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentResponsePayload
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.payments.inperson.WCCreateCustomerByOrderIdResult
import org.wordpress.android.fluxc.model.payments.inperson.TerminalStoreLocationError
import org.wordpress.android.fluxc.model.payments.inperson.TerminalStoreLocationErrorType
import org.wordpress.android.fluxc.model.payments.inperson.TerminalStoreLocationResult
import org.wordpress.android.fluxc.model.payments.inperson.TerminalStoreLocationResult.StoreAddress
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class InPersonPaymentsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchConnectionToken(site: SiteModel): WooPayload<ConnectionTokenApiResponse> {
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
    ): CapturePaymentResponsePayload {
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
                response.data?.let { data ->
                    CapturePaymentResponsePayload(site, paymentId, orderId, data.status)
                } ?: CapturePaymentResponsePayload(
                        mapToCapturePaymentError(error = null, message = "status field is null, but isError == false"),
                        site,
                        paymentId,
                        orderId
                )
            }
            is JetpackError -> {
                CapturePaymentResponsePayload(
                        mapToCapturePaymentError(response.error, response.error.message ?: "Unexpected error"),
                        site,
                        paymentId,
                        orderId
                )
            }
        }
    }

    suspend fun loadAccount(site: SiteModel): WooPayload<WCPaymentAccountResult> {
        val url = WOOCOMMERCE.payments.accounts.pathV3
        val params = mapOf("_fields" to ACCOUNT_REQUESTED_FIELDS)

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                params,
                WCPaymentAccountResult::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun createCustomerByOrderId(
        site: SiteModel,
        orderId: Long
    ): WooPayload<WCCreateCustomerByOrderIdResult> {
        val url = WOOCOMMERCE.payments.orders.order(orderId).create_customer.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncPostRequest(
                restClient = this,
                site = site,
                url = url,
                body = emptyMap(),
                clazz = WCCreateCustomerByOrderIdResult::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun getStoreLocationForSite(site: SiteModel): TerminalStoreLocationResult {
        val url = WOOCOMMERCE.payments.terminal.locations.store.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                mapOf(),
                StoreLocationApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> {
                response.data?.let { data ->
                    TerminalStoreLocationResult(
                            locationId = data.id,
                            displayName = data.displayName,
                            liveMode = data.liveMode,
                            address = StoreAddress(
                                    city = data.address?.city,
                                    country = data.address?.country,
                                    line1 = data.address?.line1,
                                    line2 = data.address?.line2,
                                    postalCode = data.address?.postalCode,
                                    state = data.address?.state
                            )
                    )
                } ?: TerminalStoreLocationResult(
                        mapToStoreLocationForSiteError(
                                error = null,
                                message = "status field is null, but isError == false"
                        )
                )
            }
            is JetpackError -> {
                TerminalStoreLocationResult(
                        mapToStoreLocationForSiteError(response.error, response.error.message ?: "Unexpected error")
                )
            }
        }
    }

    private fun mapToCapturePaymentError(error: WPComGsonNetworkError?, message: String): CapturePaymentError {
        val type = when {
            error == null -> GENERIC_ERROR
            error.apiError == "wcpay_missing_order" -> MISSING_ORDER
            error.apiError == "wcpay_payment_uncapturable" -> PAYMENT_ALREADY_CAPTURED
            error.apiError == "wcpay_capture_error" -> CAPTURE_ERROR
            error.apiError == "wcpay_server_error" -> SERVER_ERROR
            error.type == GenericErrorType.TIMEOUT -> NETWORK_ERROR
            error.type == GenericErrorType.NO_CONNECTION -> NETWORK_ERROR
            error.type == GenericErrorType.NETWORK_ERROR -> NETWORK_ERROR
            else -> GENERIC_ERROR
        }
        return CapturePaymentError(type, message)
    }

    private fun mapToStoreLocationForSiteError(error: WPComGsonNetworkError?, message: String):
            TerminalStoreLocationError {
        val type = when {
            error == null -> TerminalStoreLocationErrorType.GenericError
            error.apiError == "store_address_is_incomplete" -> {
                if (error.message.isNullOrBlank()) TerminalStoreLocationErrorType.GenericError
                else TerminalStoreLocationErrorType.MissingAddress(error.message)
            }
            error.apiError == "postal_code_invalid" -> TerminalStoreLocationErrorType.InvalidPostalCode
            error.type == GenericErrorType.TIMEOUT -> TerminalStoreLocationErrorType.NetworkError
            error.type == GenericErrorType.NO_CONNECTION -> TerminalStoreLocationErrorType.NetworkError
            error.type == GenericErrorType.NETWORK_ERROR -> TerminalStoreLocationErrorType.NetworkError
            else -> TerminalStoreLocationErrorType.GenericError
        }
        return TerminalStoreLocationError(type, message)
    }

    companion object {
        private const val ACCOUNT_REQUESTED_FIELDS: String =
                "status,has_pending_requirements,has_overdue_requirements,current_deadline,statement_descriptor," +
                        "store_currencies,country,is_live,test_mode"
    }
}
