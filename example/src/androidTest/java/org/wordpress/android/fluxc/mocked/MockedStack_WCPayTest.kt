package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.pay.WCPaymentAccountResult.WCPayAccountStatusEnum
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient
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

        assertTrue(result.result?.token?.isNotEmpty() == true)
        assertTrue(result.result?.isTestMode == true)
    }

    @Test
    fun whenValidDataProvidedForCapturePaymentThenSuccessReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-success.json", 200)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertFalse(result.isError)
        assertTrue(result.status != null)
    }

    @Test
    fun whenInvalidOrderIdProvidedForCapturePaymentThenInvalidIdIsReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-missing-order.json", 404)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == MISSING_ORDER)
    }

    @Test
    fun whenPaymentAlreadyCapturedThenUncapturableErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-uncapturable.json", 409)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == PAYMENT_ALREADY_CAPTURED)
    }

    @Test
    fun whenPaymentCaptureFailsThenCaptureFailedErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-capture-error.json", 502)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == CAPTURE_ERROR)
    }

    @Test
    fun whenUnexpectedErrorOccursDuringCaptureThenWCPayServerErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-unexpected-error.json", 500)

        val result = payRestClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == SERVER_ERROR)
    }

    @Test
    fun whenLoadAccountInvalidStatusThenFallbacksToUnknown() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-new-status.json", 200)

        val result = payRestClient.loadAccount(SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.status == WCPayAccountStatusEnum.UNKNOWN)
    }

    @Test
    fun whenLoadAccountEmptyStatusThenFallbackToNoAccount() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-empty-status.json", 200)

        val result = payRestClient.loadAccount(SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.status == WCPayAccountStatusEnum.NO_ACCOUNT)
    }

    @Test
    fun whenOverdueRequirementsThenCurrentDeadlineCorrectlyParsed() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-current-deadline.json", 200)

        val result = payRestClient.loadAccount(SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.currentDeadline == 1628258304L)
    }

    @Test
    fun whenLoadAccountRestrictedSoonStatusThenRestrictedSoonStatusReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-restricted-soon-status.json", 200)

        val result = payRestClient.loadAccount(SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.status == WCPayAccountStatusEnum.RESTRICTED_SOON)
    }

    @Test
    fun whenLoadAccountIsLiveThenIsLiveFlagIsTrue() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-is-live-account.json", 200)

        val result = payRestClient.loadAccount(SiteModel().apply { siteId = 123L })

        assertTrue(result.result!!.isLive)
    }
}
