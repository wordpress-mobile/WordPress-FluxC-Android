package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayId.CASH_ON_DELIVERY
import javax.inject.Inject


class MockedStack_WCGatewayTest: MockedStack_Base() {
    @Inject internal lateinit var restClient: GatewayRestClient
    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun givenSiteHasCodEnabledThenSuccessReturned() = runBlocking {
        interceptor.respondWith("wc-pay-fetch-cod-enabled-response-success.json")

        val result = restClient.updatePaymentGateway(
            SiteModel().apply { siteId = 123L },
            CASH_ON_DELIVERY,
            enabled = true
        )
    }
}