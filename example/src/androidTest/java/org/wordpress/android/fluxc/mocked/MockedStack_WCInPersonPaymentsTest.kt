package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
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
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import javax.inject.Inject

private const val DUMMY_PAYMENT_ID = "dummy payment id"

class MockedStack_InPersonPaymentsTest : MockedStack_Base() {
    @Inject internal lateinit var restClient: InPersonPaymentsRestClient

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private val testSite = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        siteId = 123L
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun givenSiteHasWCPayWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-connection-token-response-success.json")

        val result = restClient.fetchConnectionToken(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result?.token?.isNotEmpty() == true)
        assertTrue(result.result?.isTestMode == true)
    }

    @Test
    fun whenValidDataProvidedForCapturePaymentThenSuccessReturned() = runBlocking {
        interceptor.respondWith("wc-pay-capture-terminal-payment-response-success.json")

        val result = restClient.capturePayment(
            WOOCOMMERCE_PAYMENTS,
            testSite,
            DUMMY_PAYMENT_ID,
            -10L
        )

        Assert.assertFalse(result.isError)
        assertTrue(result.status != null)
    }

    @Test
    fun whenInvalidOrderIdProvidedForCapturePaymentThenInvalidIdIsReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-missing-order.json", 404)

        val result = restClient.capturePayment(
            WOOCOMMERCE_PAYMENTS,
            testSite,
            DUMMY_PAYMENT_ID,
            -10L
        )

        assertTrue(result.error?.type == MISSING_ORDER)
    }

    @Test
    fun whenPaymentAlreadyCapturedThenUncapturableErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-uncapturable.json", 409)

        val result = restClient.capturePayment(
            WOOCOMMERCE_PAYMENTS,
            testSite,
            DUMMY_PAYMENT_ID,
            -10L
        )

        assertTrue(result.error?.type == PAYMENT_ALREADY_CAPTURED)
    }

    @Test
    fun whenPaymentCaptureFailsThenCaptureFailedErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-capture-error.json", 502)

        val result = restClient.capturePayment(
            WOOCOMMERCE_PAYMENTS,
            testSite,
            DUMMY_PAYMENT_ID,
            -10L
        )

        assertTrue(result.error?.type == CAPTURE_ERROR)
    }

    @Test
    fun whenUnexpectedErrorOccursDuringCaptureThenWCPayServerErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-capture-terminal-payment-response-unexpected-error.json", 500)

        val result = restClient.capturePayment(
            WOOCOMMERCE_PAYMENTS,
            testSite,
            DUMMY_PAYMENT_ID,
            -10L
        )

        assertTrue(result.error?.type == SERVER_ERROR)
    }

    @Test
    fun whenLoadAccountInvalidStatusThenFallbacksToUnknown() = runBlocking {
        interceptor.respondWith("wc-pay-load-account-response-new-status.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result?.status == WCPaymentAccountStatus.UNKNOWN)
    }

    @Test
    fun whenLoadAccountEmptyStatusThenFallbackToNoAccount() = runBlocking {
        interceptor.respondWith("wc-pay-load-account-response-empty-status.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result?.status == WCPaymentAccountStatus.NO_ACCOUNT)
    }

    @Test
    fun whenOverdueRequirementsThenCurrentDeadlineCorrectlyParsed() = runBlocking {
        interceptor.respondWith("wc-pay-load-account-response-current-deadline.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result?.currentDeadline == 1628258304L)
    }

    @Test
    fun whenLoadAccountRestrictedSoonStatusThenRestrictedSoonStatusReturned() = runBlocking {
        interceptor.respondWith("wc-pay-load-account-response-restricted-soon-status.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result?.status == WCPaymentAccountStatus.RESTRICTED_SOON)
    }

    @Test
    fun whenLoadAccountEnabledStatusThenEnabledStatusReturned() = runBlocking {
        interceptor.respondWith("wc-pay-load-account-response-enabled-status.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result?.status == WCPaymentAccountStatus.ENABLED)
    }

    @Test
    fun whenLoadAccountIsLiveThenIsLiveFlagIsTrue() = runBlocking {
        interceptor.respondWith("wc-pay-load-account-response-is-live-account.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.result!!.isLive)
    }

    @Test
    fun whenStatementeDescriptorNullThenFieldSetToNull() = runBlocking {
        interceptor.respondWith("stripe-extension-statement-descriptor-null.json")

        val result = restClient.loadAccount(WOOCOMMERCE_PAYMENTS, testSite)

        assertNull(result.result!!.statementDescriptor)
    }

    @Test
    fun whenGetStoreLocationForSiteErrorWithUrl() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-address-missing-with-url-error.json", 500)

        val result = restClient.getStoreLocationForSite(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.isError)
        assertTrue(result.error?.type is MissingAddress)
        val expectedUrl = "https://myusernametestsite2020151673500.wpcomstaging.com/" +
            "wp-admin/admin.php?page=wc-settings&tab=general"
        assertEquals(expectedUrl, (result.error?.type as MissingAddress).addressEditingUrl)
    }

    @Test
    fun whenGetStoreLocationForSiteErrorWithEmptyUrl() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-address-missing-with-empty-url-error.json", 500)

        val result = restClient.getStoreLocationForSite(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.isError)
        assertTrue(result.error?.type is GenericError)
    }

    @Test
    fun whenGetStoreLocationForSiteErrorWithoutUrl() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-address-missing-without-url-error.json", 500)

        val result = restClient.getStoreLocationForSite(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.isError)
        assertTrue(result.error?.type is GenericError)
    }

    @Test
    fun whenGetStoreLocationForSiteWithInvalidPostalCodeError() = runBlocking {
        interceptor.respondWithError("wc-pay-store-location-for-site-invalid-postal-code-error.json", 500)

        val result = restClient.getStoreLocationForSite(WOOCOMMERCE_PAYMENTS, testSite)

        assertTrue(result.isError)
        assertTrue(result.error?.type is InvalidPostalCode)
    }

    @Test
    fun whenFetchTransactionsSummaryDateParamShouldNotBeSent() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-transactions-summary-response.json")

        restClient.fetchTransactionsSummary(WOOCOMMERCE_PAYMENTS, testSite, null)

        assertNull(interceptor.lastRequest?.url?.queryParameter("dateAfter"))
    }

    @Test
    fun whenFetchTransactionsSummaryForSpecificTimeSlotDateParamShouldBeSent() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-transactions-summary-response.json")

        val dateAfterParam = "2023-01-01"
        restClient.fetchTransactionsSummary(WOOCOMMERCE_PAYMENTS, testSite, dateAfterParam)

        assertTrue(interceptor.lastRequest!!.url.query!!.contains("\"date_after\":\"$dateAfterParam\""))
    }

    @Test
    fun whenFetchTransactionsSummaryInvokedWithStripePluginShouldThrowException() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-transactions-summary-response.json")
        try {
            restClient.fetchTransactionsSummary(STRIPE, testSite, null)
        } catch (e: IllegalStateException) {
            assertNotNull(e)
            assertThat(e, instanceOf(IllegalStateException::class.java))
            assertEquals("Stripe does not support fetching transactions summary", e.message)
        }
        Unit
    }
}
