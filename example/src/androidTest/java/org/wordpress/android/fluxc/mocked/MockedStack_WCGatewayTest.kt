package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayResponse
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import kotlin.properties.Delegates

class MockedStack_WCGatewayTest: MockedStack_Base() {
    @Inject lateinit var gatewayRestClient: GatewayRestClient
    @Inject lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by Delegates.notNull()

    private val siteModel = SiteModel().apply {
        id = 5
        siteId = 567
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        lastAction = null
    }
    @Test
    fun testGatewayPostSuccess() = runBlocking {
        val gatewayResponse = GatewayResponse(
            "cod",
            "Cash on Delivery",
            "Description",
            "5",
            true,
            "",
            "",
            listOf()
        )
        interceptor.respondWith("")
    }
}
