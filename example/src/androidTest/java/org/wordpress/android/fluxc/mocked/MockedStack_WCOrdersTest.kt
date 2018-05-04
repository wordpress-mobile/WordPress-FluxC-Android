package org.wordpress.android.fluxc.mocked

import com.google.gson.JsonObject
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_WCOrdersTest : MockedStack_Base() {
    @Inject internal lateinit var orderRestClient: OrderRestClient
    @Inject internal lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by notNull()

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
    fun testOrderListFetchSuccess() {
        interceptor.respondWith("wc-orders-response-success.json")
        orderRestClient.fetchOrders(siteModel, 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDERS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrdersResponsePayload
        assertNull(payload.error)
        assertEquals(4, payload.orders.size)

        with(payload.orders[0]) {
            assertEquals(siteModel.id, localSiteId)
            assertEquals(949, remoteOrderId)
            assertEquals("949", number)
            assertEquals(OrderStatus.PROCESSING, status)
            assertEquals("USD", currency)
            assertEquals("2018-04-02T14:57:39Z", dateCreated)
            assertEquals("44.00", total)
            assertEquals("0.00", totalTax)
            assertEquals("4.00", shippingTotal)
            assertEquals("stripe", paymentMethod)
            assertEquals("Credit Card (Stripe)", paymentMethodTitle)
            assertFalse(pricesIncludeTax)
            assertEquals(2, getLineItemList().size)
            assertEquals(40.0, getOrderSubtotal(), 0.01)
        }

        // Customer note
        with(payload.orders[1]) {
            assertEquals("test checkout field editor note", customerNote)
        }

        // Refunded order
        with(payload.orders[2]) {
            assertEquals(85.0, getOrderSubtotal(), 0.01)
            assertEquals("7.00", shippingTotal)
            assertEquals("92.00", total)
            assertEquals(-92.0, refundTotal, 0.01)
        }

        // Order with coupons
        with(payload.orders[3]) {
            assertEquals(60.0, getOrderSubtotal(), 0.01)
            assertEquals("7.59", shippingTotal)
            assertEquals("7.59", total)
            assertEquals("60.00", discountTotal)
            assertEquals("20\$off, 40\$off", discountCodes)
        }
    }

    @Test
    fun testOrderListFetchError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        orderRestClient.fetchOrders(SiteModel(), 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDERS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrdersResponsePayload
        assertNotNull(payload.error)
    }

    @Test
    fun testOrderStatusUpdateSuccess() {
        val originalOrder = WCOrderModel().apply {
            id = 8
            localSiteId = siteModel.id
            status = OrderStatus.PROCESSING
            remoteOrderId = 88
            total = "15.00"
        }

        interceptor.respondWith("wc-order-update-response-success.json")
        orderRestClient.updateOrderStatus(originalOrder, siteModel, OrderStatus.REFUNDED)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCOrderAction.UPDATED_ORDER_STATUS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteOrderPayload
        with (payload) {
            assertNull(error)
            assertEquals(originalOrder.id, order.id)
            assertEquals(siteModel.id, order.localSiteId)
            assertEquals(originalOrder.remoteOrderId, order.remoteOrderId)
            assertEquals(OrderStatus.REFUNDED, order.status)
        }
    }

    @Test
    fun testOrderStatusUpdateError() {
        val originalOrder = WCOrderModel().apply {
            id = 8
            localSiteId = siteModel.id
            status = OrderStatus.PROCESSING
            remoteOrderId = 88
            total = "15.00"
        }

        val errorJson = JsonObject().apply {
            addProperty("error", "woocommerce_rest_shop_order_invalid_id")
            addProperty("message", "Invalid ID.")
        }

        interceptor.respondWithError(errorJson, 400)
        orderRestClient.updateOrderStatus(originalOrder, siteModel, OrderStatus.REFUNDED)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCOrderAction.UPDATED_ORDER_STATUS, lastAction!!.type)
        val payload = lastAction!!.payload as RemoteOrderPayload
        with (payload) {
            // Expecting a 'invalid id' error from the server
            assertNotNull(error)
            assertEquals(OrderErrorType.INVALID_ID, error.type)
        }
    }

    @Test
    fun testOrderNotesFetchSuccess() {
        interceptor.respondWith("wc-order-notes-response-success.json")
        orderRestClient.fetchOrderNotes(
                WCOrderModel().apply {
                    localSiteId = 5
                    id = 8
                    remoteOrderId = 88
                }, siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDER_NOTES, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderNotesResponsePayload
        assertNull(payload.error)
        assertEquals(7, payload.notes.size)

        // Verify basic order fields and private note
        with(payload.notes[0]) {
            assertEquals(1942, remoteNoteId)
            assertEquals("2018-04-27T20:48:10Z", dateCreated)
            assertEquals(5, localSiteId)
            assertEquals(8, localOrderId)
            assertEquals("Email queued: Poster Purchase Follow-Up scheduled " +
                    "on Poster Purchase Follow-Up<br/>Trigger: Poster Purchase Follow-Up", note)
            assertEquals(false, isCustomerNote)
        }

        // Verify customer note
        with(payload.notes[6]) {
            assertEquals("Please gift wrap", note)
            assertTrue(isCustomerNote)
        }
    }

    @Test
    fun testOrderNotesFetchError() {
        interceptor.respondWithError("wc-order-notes-response-failure-invalid-id.json", 404)
        orderRestClient.fetchOrderNotes(
                WCOrderModel().apply {
                    localSiteId = 5
                    id = 8
                    remoteOrderId = 88
                }, siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDER_NOTES, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderNotesResponsePayload
        with (payload) {
            // Expecting a 'invalid id' error from the server
            assertNotNull(error)
            assertEquals(OrderErrorType.INVALID_ID, error.type)
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
