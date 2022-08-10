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

//@Test
//fun testOrderNotePostSuccess() = runBlocking {
//    val orderModel = OrderEntity(orderId = 5, localSiteId = siteModel.localId())
//
//    interceptor.respondWith("wc-order-note-post-response-success.json")
//    val payload = orderRestClient.postOrderNote(
//        site = siteModel,
//        orderId = orderModel.orderId,
//        note = "Test rest note",
//        isCustomerNote = true
//    )
//
//    with(payload) {
//        org.junit.Assert.assertNull(error)
//        org.junit.Assert.assertEquals("Test rest note", result!!.note)
//        org.junit.Assert.assertEquals(true, result!!.isCustomerNote)
//        org.junit.Assert.assertFalse(result!!.isSystemNote) // Any note created from the app should be flagged as user-created
//        org.junit.Assert.assertEquals(orderModel.orderId, result!!.orderId.value)
//        org.junit.Assert.assertEquals(siteModel.remoteId(), result!!.siteId)
//    }
//}