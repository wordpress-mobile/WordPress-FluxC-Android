package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult.WCPaymentAccountStatus
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType.GenericError
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType.InvalidPostalCode
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType.MissingAddress
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.inperson.InPersonPaymentsRestClient
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import javax.inject.Inject

private const val DUMMY_PAYMENT_ID = "dummy payment id"

class MockedStack_InPersonPaymentsTest : MockedStack_Base() {
    @Inject internal lateinit var restClient: InPersonPaymentsRestClient

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun givenSiteHasWCPayWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-connection-token-response-success.json")

        val result = restClient.fetchConnectionToken(SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.token?.isNotEmpty() == true)
        assertTrue(result.result?.isTestMode == true)
    }

    @Test
    fun whenValidDataProvidedForCapturePaymentThenSuccessReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-success.json", 200)

        val result = restClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        Assert.assertFalse(result.isError)
        assertTrue(result.status != null)
    }

    @Test
    fun whenInvalidOrderIdProvidedForCapturePaymentThenInvalidIdIsReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-missing-order.json", 404)

        val result = restClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == MISSING_ORDER)
    }

    @Test
    fun whenPaymentAlreadyCapturedThenUncapturableErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-uncapturable.json", 409)

        val result = restClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == PAYMENT_ALREADY_CAPTURED)
    }

    @Test
    fun whenPaymentCaptureFailsThenCaptureFailedErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-capture-error.json", 502)

        val result = restClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == CAPTURE_ERROR)
    }

    @Test
    fun whenUnexpectedErrorOccursDuringCaptureThenWCPayServerErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-unexpected-error.json", 500)

        val result = restClient.capturePayment(SiteModel().apply { siteId = 123L }, DUMMY_PAYMENT_ID, -10L)

        assertTrue(result.error?.type == SERVER_ERROR)
    }

    @Test
    fun whenLoadAccountInvalidStatusThenFallbacksToUnknown() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-new-status.json", 200)

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.status == WCPaymentAccountStatus.UNKNOWN)
    }

    @Test
    fun whenLoadAccountEmptyStatusThenFallbackToNoAccount() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-empty-status.json", 200)

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.status == WCPaymentAccountStatus.NO_ACCOUNT)
    }

    @Test
    fun whenOverdueRequirementsThenCurrentDeadlineCorrectlyParsed() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-current-deadline.json", 200)

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.currentDeadline == 1628258304L)
    }

    @Test
    fun whenLoadAccountRestrictedSoonStatusThenRestrictedSoonStatusReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-restricted-soon-status.json", 200)

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, SiteModel().apply { siteId = 123L })

        assertTrue(result.result?.status == WCPaymentAccountStatus.RESTRICTED_SOON)
    }

    @Test
    fun whenLoadAccountIsLiveThenIsLiveFlagIsTrue() = runBlocking {
        interceptor.respondWithError("wc-pay-load-account-response-is-live-account.json", 200)

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, SiteModel().apply { siteId = 123L })

        assertTrue(result.result!!.isLive)
    }

    @Test
    fun whenGetStoreLocationForSiteErrorWithUrl() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-address-missing-with-url-error.json", 500)

        val result = restClient.getStoreLocationForSite(SiteModel().apply { siteId = 123L })

        assertTrue(result.isError)
        assertTrue(result.error?.type is MissingAddress)
        val expectedUrl = "https://myusernametestsite2020151673500.wpcomstaging.com/" +
                "wp-admin/admin.php?page=wc-settings&tab=general"
        assertEquals(expectedUrl, (result.error?.type as MissingAddress).addressEditingUrl)
    }

    @Test
    fun whenGetStoreLocationForSiteErrorWithEmptyUrl() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-address-missing-with-empty-url-error.json", 500)

        val result = restClient.getStoreLocationForSite(SiteModel().apply { siteId = 123L })

        assertTrue(result.isError)
        assertTrue(result.error?.type is GenericError)
    }

    @Test
    fun whenGetStoreLocationForSiteErrorWithoutUrl() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-address-missing-without-url-error.json", 500)

        val result = restClient.getStoreLocationForSite(SiteModel().apply { siteId = 123L })

        assertTrue(result.isError)
        assertTrue(result.error?.type is GenericError)
    }

    @Test
    fun whenGetStoreLocationForSiteWithInvalidPostalCodeError() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-invalid-postal-code-error.json", 500)

        val result = restClient.getStoreLocationForSite(SiteModel().apply { siteId = 123L })

        assertTrue(result.isError)
        assertTrue(result.error?.type is InvalidPostalCode)
    }
}
