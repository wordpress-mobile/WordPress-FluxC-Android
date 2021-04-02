package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pay.PayRestClient
import javax.inject.Inject

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
}
