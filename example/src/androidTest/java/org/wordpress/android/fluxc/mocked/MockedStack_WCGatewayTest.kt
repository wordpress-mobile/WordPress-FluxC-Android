package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import javax.inject.Inject

class MockedStack_WCGatewayTest: MockedStack_Base() {
    @Inject internal lateinit var gatewayRestClient: GatewayRestClient
    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun givenSiteHasCODEnabledThenSuccessReturned() = runBlocking {
        interceptor.respondWith("")
    }
}



//@Test
//    fun givenSiteHasWCPayWhenFetchConnectionTokenInvokedThenTokenReturned() = runBlocking {
//        interceptor.respondWith("wc-pay-fetch-connection-token-response-success.json")
//
//        val result = restClient.fetchConnectionToken(WOOCOMMERCE_PAYMENTS, SiteModel().apply { siteId = 123L })
//
//        assertTrue(result.result?.token?.isNotEmpty() == true)
//        assertTrue(result.result?.isTestMode == true)
//    }