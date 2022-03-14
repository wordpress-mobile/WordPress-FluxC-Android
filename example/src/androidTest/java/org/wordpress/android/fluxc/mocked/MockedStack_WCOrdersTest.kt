package org.wordpress.android.fluxc.mocked

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
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
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload
import org.wordpress.android.util.DateTimeUtils
import java.util.concurrent.CountDownLatch
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
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDERS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrdersResponsePayload
        assertNull(payload.error)
        assertEquals(4, payload.orders.size)

        with(payload.orders[0]) {
            assertEquals(siteModel.id, localSiteId.value)
            assertEquals(949, orderId)
            assertEquals("949", number)
            assertEquals(CoreOrderStatus.PROCESSING.value, status)
            assertEquals("USD", currency)
            assertEquals("2018-04-02T14:57:39Z", dateCreated)
            assertEquals("44.00", total)
            assertEquals("0.00", totalTax)
            assertEquals("4.00", shippingTotal)
            assertEquals("stripe", paymentMethod)
            assertEquals("2018-04-11T18:58:54Z", datePaid)
            assertEquals("Credit Card (Stripe)", paymentMethodTitle)
            assertFalse(pricesIncludeTax)
            assertEquals(2, getLineItemList().size)
            assertEquals(40.0, getOrderSubtotal(), 0.01)
            assertEquals(2, getFeeLineList().size)
            assertEquals("10.00", getFeeLineList()[0].total)
            assertEquals("\$10.00 fee", getFeeLineList()[0].name)
            assertEquals("2.00", getFeeLineList()[0].totalTax)
            assertEquals(28L, getFeeLineList()[0].id)
            assertEquals(1, getTaxLineList().size)
            assertEquals(318L, getTaxLineList()[0].id)
            assertEquals(false, getTaxLineList()[0].compound)
            assertEquals("1.35", getTaxLineList()[0].taxTotal)
            assertEquals("State Tax", getTaxLineList()[0].label)
            assertEquals("0.00", getTaxLineList()[0].shippingTaxTotal)
            assertEquals(5.25f, getTaxLineList()[0].ratePercent)
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
            assertEquals("-92.0".toBigDecimal().stripTrailingZeros(), refundTotal.stripTrailingZeros())
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
    fun testSearchOrdersSuccess() {
        interceptor.respondWith("wc-orders-response-success.json")
        orderRestClient.searchOrders(siteModel, "", 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.SEARCHED_ORDERS, lastAction!!.type)
        val payload = lastAction!!.payload as SearchOrdersResponsePayload
        assertNull(payload.error)
        assertEquals(4, payload.orders.size)
    }

    @Test
    fun testSearchOrdersError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        orderRestClient.searchOrders(SiteModel(), "", 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.SEARCHED_ORDERS, lastAction!!.type)
        val payload = lastAction!!.payload as SearchOrdersResponsePayload
        assertNotNull(payload.error)
    }

    @Test
    fun testOrdersCountFetchSuccess() {
        val statusFilter = CoreOrderStatus.COMPLETED.value

        interceptor.respondWith("wc-order-count-response-success.json")
        orderRestClient.fetchOrderCount(siteModel, statusFilter)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDERS_COUNT, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrdersCountResponsePayload
        assertNull(payload.error)
        assertEquals(128, payload.count)
        assertEquals(statusFilter, payload.statusFilter)
    }

    @Test
    fun testOrderListFetchError() {
        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        orderRestClient.fetchOrders(SiteModel(), 0)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDERS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrdersResponsePayload
        assertNotNull(payload.error)
    }

    @Test
    fun testFetchSingleOrderSuccess() = runBlocking {
        val remoteOrderId = 88L
        interceptor.respondWith("wc-fetch-order-response-success.json")
        val response = orderRestClient.fetchSingleOrder(siteModel, remoteOrderId)

        with(response) {
            assertNull(error)
            assertEquals(remoteOrderId, order.orderId)
        }
    }

    @Test
    fun testFetchSingleOrderOrderKeySuccess() = runBlocking {
        val remoteOrderId = 88L
        val orderKey = "wc_order_5a77766b88986"
        interceptor.respondWith("wc-fetch-order-response-success.json")
        val response = orderRestClient.fetchSingleOrder(siteModel, remoteOrderId)

        with(response) {
            assertNull(error)
            assertEquals(orderKey, order.orderKey)
        }
    }

    @Test
    fun testFetchSingleOrderError() = runBlocking {
        val remoteOrderId = 88L

        interceptor.respondWithError("jetpack-tunnel-root-response-failure.json")
        val response = orderRestClient.fetchSingleOrder(siteModel, remoteOrderId)

        assertNotNull(response.error)
    }

    @Test
    fun testOrderStatusUpdateSuccess() = runBlocking {
        val originalOrder = OrderEntity(
            localSiteId = siteModel.localId(),
            status = CoreOrderStatus.PROCESSING.value,
            orderId = 88,
            total = "15.00"
        )

        interceptor.respondWith("wc-order-update-response-success.json")
        val payload = orderRestClient.updateOrderStatus(
                originalOrder, siteModel, CoreOrderStatus.REFUNDED.value
        )

        with(payload) {
            assertNull(error)
            assertEquals(siteModel.id, order.localSiteId.value)
            assertEquals(originalOrder.orderId, order.orderId)
            assertEquals(CoreOrderStatus.REFUNDED.value, order.status)
        }
    }

    @Test
    fun testOrderStatusUpdateError() = runBlocking {
        val originalOrder = OrderEntity(
            localSiteId = siteModel.localId(),
            status = CoreOrderStatus.PROCESSING.value,
            orderId = 88,
            total = "15.00"
        )

        val errorJson = JsonObject().apply {
            addProperty("error", "woocommerce_rest_shop_order_invalid_id")
            addProperty("message", "Invalid ID.")
        }

        interceptor.respondWithError(errorJson, 400)
        val payload = orderRestClient.updateOrderStatus(
            originalOrder, siteModel, CoreOrderStatus.REFUNDED.value
        )

        with(payload) {
            // Expecting a 'invalid id' error from the server
            assertNotNull(error)
            assertEquals(OrderErrorType.INVALID_ID, error.type)
        }
    }

    @Test
    fun testOrderNotesFetchSuccess() = runBlocking {
        interceptor.respondWith("wc-order-notes-response-success.json")
        val payload = orderRestClient.fetchOrderNotes(
                site = siteModel,
                orderId = 88
        )

        assertNull(payload.error)
        assertEquals(8, payload.result!!.size)

        // Verify basic order fields and private, system note
        with(payload.result!![0]) {
            assertEquals(1942, noteId.value)
            assertEquals(DateTimeUtils.dateUTCFromIso8601("2018-04-27T20:48:10Z"), dateCreated)
            assertEquals(siteModel.remoteId(), siteId)
            assertEquals(88, orderId.value)
            assertEquals(
                    "Email queued: Poster Purchase Follow-Up scheduled " +
                            "on Poster Purchase Follow-Up<br/>Trigger: Poster Purchase Follow-Up", note
            )
            assertFalse(isCustomerNote)
            assertTrue(isSystemNote)
        }

        // Verify private user-created note
        with(payload.result!![6]) {
            assertEquals("Interesting order!", note)
            assertFalse(isCustomerNote)
            assertFalse(isSystemNote)
        }

        // Verify customer-facing note
        with(payload.result!![7]) {
            assertEquals("Shipping soon!", note)
            assertTrue(isCustomerNote)
            assertFalse(isSystemNote)
        }
    }

    @Test
    fun testOrderNotesFetchError() = runBlocking {
        interceptor.respondWithError("wc-order-notes-response-failure-invalid-id.json", 404)
        val payload = orderRestClient.fetchOrderNotes(
                site = siteModel,
                orderId = 88
        )

        with(payload) {
            // Expecting a 'invalid id' error from the server
            assertNotNull(error)
            assertEquals(WooErrorType.INVALID_ID, error.type)
        }
    }

    @Test
    fun testOrderNotePostSuccess() = runBlocking {
        val orderModel = OrderEntity(orderId = 5, localSiteId = siteModel.localId())

        interceptor.respondWith("wc-order-note-post-response-success.json")
        val payload = orderRestClient.postOrderNote(
            site = siteModel,
            orderId = orderModel.orderId,
            note = "Test rest note",
            isCustomerNote = true
        )

        with(payload) {
            assertNull(error)
            assertEquals("Test rest note", result!!.note)
            assertEquals(true, result!!.isCustomerNote)
            assertFalse(result!!.isSystemNote) // Any note created from the app should be flagged as user-created
            assertEquals(orderModel.orderId, result!!.orderId.value)
            assertEquals(siteModel.remoteId(), result!!.siteId)
        }
    }

    @Test
    fun testOrderNotePostError() = runBlocking {
        val orderModel = OrderEntity(siteModel.localId(), orderId = 0)

        val errorJson = JsonObject().apply {
            addProperty("error", "woocommerce_rest_shop_order_invalid_id")
            addProperty("message", "Invalid ID.")
        }

        interceptor.respondWithError(errorJson, 404)
        val payload = orderRestClient.postOrderNote(
                site = siteModel,
                orderId = orderModel.orderId,
                note = "Test rest note",
                isCustomerNote = true
        )

        with(payload) {
            // Expecting a 'invalid id' error from the server
            assertNotNull(error)
            assertEquals(WooErrorType.INVALID_ID, error.type)
        }
    }

    @Test
    fun testHasAnyOrders() = runBlocking {
        interceptor.respondWith("wc-has-orders-response-success.json")
        val payload = orderRestClient.fetchHasOrders(siteModel, filterByStatus = null)
        with(payload) {
            assertNull(error)
            assertTrue(hasOrders)
            assertNull(statusFilter)
        }
    }

    @Test
    fun testHasAnyOrdersReturnsFalseWhenThereAreNoOrders() = runBlocking {
        interceptor.respondWith("wc-has-orders-response-success-no-orders.json")
        val payload = orderRestClient.fetchHasOrders(siteModel, filterByStatus = null)
        with(payload) {
            assertNull(error)
            assertFalse(hasOrders)
            assertNull(statusFilter)
        }
    }

    @Test
    fun testHasAnyOrdersReturnsErrorWhenMissingDataKey() = runBlocking {
        interceptor.respondWith("wc-has-orders-response-success-missing-data-key.json")
        val payload = orderRestClient.fetchHasOrders(siteModel, filterByStatus = null)
        with(payload) {
            assertNotNull(error)
            assertNull(statusFilter)
        }
    }

    @Test
    fun testOrderStatusOptionsFetchSuccess() {
        interceptor.respondWith("wc-fetch-order-status-options-success.json")
        orderRestClient.fetchOrderStatusOptions(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderStatusOptionsResponsePayload
        assertNull(payload.error)
        assertEquals(8, payload.labels.size)

        with(payload.labels[0]) {
            assertEquals("pending", statusKey)
            assertEquals("Pending payment", label)
        }
    }

    @Test
    fun testOrderShipmentTrackingsFetchSuccess() = runBlocking {
        val orderModel = OrderEntity(localSiteId = siteModel.localId(), 0)
        interceptor.respondWith("wc-order-shipment-trackings-success.json")
        val payload = orderRestClient.fetchOrderShipmentTrackings(
                siteModel, orderModel.orderId
        )

        assertNull(payload.error)
        assertEquals(2, payload.trackings.size)

        with(payload.trackings[0]) {
            assertEquals(remoteTrackingId, "19b28e4151dc5b4ae1c27294ede241f9")
            assertEquals(trackingProvider, "USPS")
            assertEquals(
                    trackingLink,
                    "https://tools.usps.com/go/TrackConfirmAction_input?qtc_tLabels1=11122233344466666")
            assertEquals(trackingNumber, "11122233344466666")
            assertEquals(dateShipped, "2019-02-19")
        }
    }

    @Test
    fun testAddOrderShipmentTrackingSuccess() = runBlocking {
        val orderModel = OrderEntity(localSiteId = siteModel.localId(), orderId = 0)
        val trackingModel = WCOrderShipmentTrackingModel().apply {
            trackingProvider = "TNT Express (consignment)"
            trackingNumber = "123456"
            dateShipped = "2019-04-18"
        }
        interceptor.respondWith("wc-post-order-shipment-tracking-success.json")
        val payload = orderRestClient.addOrderShipmentTrackingForOrder(
                siteModel, orderModel.orderId, trackingModel, isCustomProvider = false
        )

        assertNull(payload.error)
        assertNotNull(payload.tracking)

        with(payload.tracking!!) {
            assertEquals(remoteTrackingId, "95bb641d79d7c6974001d6a03fbdabc0")
            assertEquals(trackingNumber, "123456")
            assertEquals(trackingProvider, "TNT Express (consignment)")
            assertEquals(trackingLink, "http://www.tnt.com/webtracker/tracking.do?requestType=GEN&searchType=" +
                    "CON&respLang=en&respCountry=GENERIC&sourceID=1&sourceCountry=ww&cons=123456&navigation=1&g" +
                    "\nenericSiteIdent=")
            assertEquals(dateShipped, "2019-04-18")
        }
    }

    @Test
    fun testAddOrderShipmentTrackingCustomProviderSuccess() = runBlocking {
        val orderModel = OrderEntity(localSiteId = siteModel.localId(), orderId = 0)
        val trackingModel = WCOrderShipmentTrackingModel().apply {
            trackingProvider = "Amanda Test Provider"
            trackingNumber = "123456"
            trackingLink = "https://www.google.com"
            dateShipped = "2019-04-19"
        }
        interceptor.respondWith("wc-post-order-shipment-tracking-custom-success.json")
        val payload = orderRestClient.addOrderShipmentTrackingForOrder(
                siteModel, orderModel.orderId, trackingModel, isCustomProvider = true
        )

        assertNull(payload.error)
        assertNotNull(payload.tracking)

        with(payload.tracking!!) {
            assertEquals(remoteTrackingId, "ecfb139dcc180833b8dbe92e438913fc")
            assertEquals(trackingNumber, "123456")
            assertEquals(trackingProvider, "Amanda Test Provider")
            assertEquals(trackingLink, "https://www.google.com")
            assertEquals(dateShipped, "2019-04-19")
        }
    }

    @Test
    fun testDeleteOrderShipmentTrackingSuccess() = runBlocking {
        val orderModel = OrderEntity(localSiteId = siteModel.localId(), orderId = 0)
        val trackingModel = WCOrderShipmentTrackingModel().apply {
            remoteTrackingId = "95bb641d79d7c6974001d6a03fbdabc0"
        }

        interceptor.respondWith("wc-delete-order-shipment-tracking-success.json")
        val payload = orderRestClient.deleteShipmentTrackingForOrder(
                siteModel, orderModel.orderId, trackingModel
        )

        assertNull(payload.error)
        assertNotNull(payload.tracking)

        with(payload.tracking!!) {
            assertEquals(remoteTrackingId, "95bb641d79d7c6974001d6a03fbdabc0")
        }
    }

    @Test
    fun testOrderShipmentProvidersFetchSuccess() = runBlocking {
        val orderModel = OrderEntity(localSiteId = siteModel.localId(), orderId = 0)
        interceptor.respondWith("wc-order-shipment-providers-success.json")
        val payload = orderRestClient.fetchOrderShipmentProviders(siteModel, orderModel)
        assertNull(payload.error)
        assertEquals(54, payload.providers.size)

        with(payload.providers[0]) {
            assertEquals(localSiteId, siteModel.id)
            assertEquals("Australia", country)
            assertEquals("Australia Post", carrierName)
            assertEquals("http://auspost.com.au/track/track.html?id=%1\$s", carrierLink)
        }
    }

    /**
     * We had a user with a site that returned simply "failed" without an error when requesting the
     * shipment provider list, resulting in a crash when parsing the response. This tests that
     * situation and ensures we dispatch an error
     */
    @Test
    fun testOrderShipmentProvidersFetchFailed() = runBlocking {
        val orderModel = OrderEntity(localSiteId = siteModel.localId(), orderId = 0)
        interceptor.respondWith("wc-order-shipment-providers-failed.json")
        val payload = orderRestClient.fetchOrderShipmentProviders(siteModel, orderModel)
        assertNotNull(payload.error)
        assertEquals(payload.error.type, OrderErrorType.INVALID_RESPONSE)
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
