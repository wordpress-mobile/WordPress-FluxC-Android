package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.CapturePaymentResponsePayload
import org.wordpress.android.fluxc.model.payments.ConnectionTokenResult
import org.wordpress.android.fluxc.model.payments.PaymentAccountResult
import org.wordpress.android.fluxc.model.payments.CreateCustomerByOrderIdResult
import org.wordpress.android.fluxc.model.payments.WCTerminalStoreLocationResult
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCPayStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: PayRestClient
) {
    suspend fun fetchConnectionToken(site: SiteModel): WooResult<ConnectionTokenResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchConnectionToken") {
            val response = restClient.fetchConnectionToken(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    WooResult(ConnectionTokenResult(response.result.token, response.result.isTestMode))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun capturePayment(site: SiteModel, paymentId: String, orderId: Long): CapturePaymentResponsePayload {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "capturePayment") {
            restClient.capturePayment(site, paymentId, orderId)
        }
    }

    suspend fun loadAccount(site: SiteModel): WooResult<PaymentAccountResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "loadAccount") {
            restClient.loadAccount(site).asWooResult()
        }
    }

    suspend fun createCustomerByOrderId(
        site: SiteModel,
        orderId: Long
    ): WooResult<CreateCustomerByOrderIdResult> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createCustomerByOrderId") {
            restClient.createCustomerByOrderId(site, orderId).asWooResult()
        }
    }

    suspend fun getStoreLocationForSite(site: SiteModel): WCTerminalStoreLocationResult {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "getStoreLocationForSite") {
            restClient.getStoreLocationForSite(site)
        }
    }
}
