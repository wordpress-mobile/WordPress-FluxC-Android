package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayId.CASH_ON_DELIVERY
import javax.inject.Inject

class MockedStack_WCGatewayTest : MockedStack_Base() {
    @Inject internal lateinit var restClient: GatewayRestClient
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
    fun whenFetchGatewaySuccessReturnSuccess() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-gateway-response-success.json")

        val result = restClient.fetchGateway(
            site = testSite,
            gatewayId = "cod"
        )

        assertTrue(result.result != null)
        Assert.assertFalse(result.isError)
    }

    @Test
    fun whenFetchGatewayErrorReturnError() = runBlocking {
        interceptor.respondWithError("wc-pay-fetch-gateway-response-error.json", 500)

        val result = restClient.fetchGateway(
            site = testSite,
            gatewayId = "cod"
        )

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }

    @Test
    fun whenValidDataProvidedUpdateGatewayThenSuccessReturned() = runBlocking {
        interceptor.respondWith("wc-pay-update-gateway-response-success.json")

        val result = restClient.updatePaymentGateway(
            site = testSite,
            gatewayId = CASH_ON_DELIVERY,
            enabled = true,
            title = "Pay on Delivery",
            description = "Pay by cash or card on delivery"
        )

        Assert.assertFalse(result.isError)
        assertTrue(result.result != null)
    }

    @Test
    fun whenUpdateGatewayFailsDueToUnexpectedServerErrorThenServerErrorReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-update-gateway-response-error.json", 500)

        val result = restClient.updatePaymentGateway(
            site = testSite,
            gatewayId = CASH_ON_DELIVERY,
            enabled = false,
            title = "Pay on Delivery",
            description = "Pay by cash or card on delivery"
        )

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }

    @Test
    fun whenInvalidDataProvidedUpdateGatewayThenInvalidParamReturned() = runBlocking {
        interceptor.respondWithError("wc-pay-update-gateway-invalid-data-response-error.json", 500)

        val result = restClient.updatePaymentGateway(
            site = testSite,
            enabled = true,
            title = "THIS IS INVALID TITLE",
            description = "THIS IS INVALID DESCRIPTION",
            gatewayId = CASH_ON_DELIVERY
        )

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }

    @Test
    fun whenFetchAllGatewaysSucceedsReturnSuccess() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-all-gateways-response-success.json")

        val result = restClient.fetchAllGateways(testSite)

        Assert.assertFalse(result.isError)
        assertTrue(result.result != null)
    }

    @Test
    fun whenFetchAllGatewaysErrorReturnError() = runBlocking {
        interceptor.respondWithError("wc-pay-fetch-all-gateways-response-error.json", 500)

        val result = restClient.fetchAllGateways(testSite)

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }
}
