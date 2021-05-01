package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.CaptureFailed
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.MissingOrder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.UncapturablePayment
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient.CaptureTerminalPaymentError.WCPayServerError
import javax.inject.Inject

private const val DUMMY_PAYMENT_ID = "dummy payment id"

class MockedStack_WCPayTest : MockedStack_Base() {
    @Inject internal lateinit var payRestClient: PayRestClient

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun givenSiteHasWCPayWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-connection-token-response-success.json")

        val result = payRestClient.fetchConnectionToken(SiteModel().apply { siteId = 123L })

        Assert.assertTrue(result.result?.token?.isNotEmpty() == true)
        Assert.assertTrue(result.result?.isTestMode == true)
    }

    @Test
    fun whenValidDataProvidedForCapturePaymentThenSuccessReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-success.json", 200)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertFalse(result.isError)
        Assert.assertTrue(result.result != null)
    }

    @Test
    fun whenInvalidOrderIdProvidedForCapturePaymentThenInvalidIdIsReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-missing-order.json", 404)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertTrue(result.error is MissingOrder)
    }

    @Test
    fun whenPaymentAlreadyCapturedThenUncapturableErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-uncapturable.json", 409)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertTrue(result.error is UncapturablePayment)
    }

    @Test
    fun whenPaymentCaptureFailsThenCaptureFailedErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-capture-error.json", 502)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertTrue(result.error is CaptureFailed)
    }

    @Test
    fun whenUnexpectedErrorOccursDuringCaptureThenWCPayServerErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-unexpected-error.json", 500)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertTrue(result.error is WCPayServerError)
    }
}
