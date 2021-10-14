package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.Store.OnChanged
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_WCOrderExtTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_ORDER_SHIPMENT_PROVIDERS
    }

    @Inject internal lateinit var orderStore: WCOrderStore

    override fun buildAuthenticatePayload() = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JETPACK_EXTENSIONS,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JETPACK_EXTENSIONS)

    private var nextEvent: TestEvent = TestEvent.NONE
    private var lastEvent: OnChanged<OrderError>? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings
     * plugin by attempting to fetch the trackings for an order that
     * has shipment trackings recorded.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testFetchShipmentTrackingsForOrder_hasTrackings() = runBlocking {
        val orderModel = WCOrderModel().apply {
            id = 8
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }
        orderStore.fetchOrderShipmentTrackings(orderModel.id, orderModel.remoteOrderId, sSite)

        val trackings = orderStore.getShipmentTrackingsForOrder(
                sSite, orderModel.id
        )
        assertTrue(trackings.isNotEmpty())
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings
     * plugin by attempting to fetch the trackings for an order that
     * does not have any shipment trackings recorded.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testFetchShipmentTrackingsForOrder_noTrackings() = runBlocking {
        val orderModel = WCOrderModel().apply {
            id = 9
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITHOUT_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }

        orderStore.fetchOrderShipmentTrackings(orderModel.id, orderModel.remoteOrderId, sSite)

        val trackings = orderStore.getShipmentTrackingsForOrder(
                sSite, orderModel.id
        )
        assertTrue(trackings.isEmpty())
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings plugin by first
     * posting a new shipment tracking record for an order, and then deleting that same
     * shipment tracking record.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testAddAndDeleteShipmentTrackingForOrder_standardProvider() = runBlocking {
        /*
         * TEST 1: Add an order shipment tracking for an order
         */
        val orderModel = WCOrderModel().apply {
            id = 8
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }

        val testProvider = "TNT Express (consignment)"
        val testTrackingNumber = TestUtils.randomString(15)
        val testDateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())

        val trackingModel = WCOrderShipmentTrackingModel().apply {
            trackingProvider = testProvider
            trackingNumber = testTrackingNumber
            dateShipped = testDateShipped
        }
        orderStore.addOrderShipmentTracking(AddOrderShipmentTrackingPayload(
                sSite, orderModel.id, orderModel.remoteOrderId, trackingModel, isCustomProvider = false)
        )

        var trackings = orderStore.getShipmentTrackingsForOrder(
                sSite, orderModel.id
        )
        assertTrue(trackings.isNotEmpty())

        var trackingResult: WCOrderShipmentTrackingModel? = null
        trackings.forEach {
            if (it.trackingNumber == testTrackingNumber && it.dateShipped == testDateShipped) {
                trackingResult = it
                return@forEach
            }
        }
        assertNotNull(trackingResult)
        with(trackingResult!!) {
            assertEquals(trackingProvider, testProvider)
            assertEquals(trackingNumber, testTrackingNumber)
            assertEquals(dateShipped, testDateShipped)
        }

        /*
         * TEST 2: Delete the previously added shipment tracking record
         */
        val onOrderChanged = orderStore.deleteOrderShipmentTracking(
                DeleteOrderShipmentTrackingPayload(sSite, orderModel.id, orderModel.remoteOrderId, trackingResult!!)
        )

        assertFalse(onOrderChanged.isError)
        // Verify the tracking record is no longer in the database
        var currentCount = trackings.size
        trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.id)
        assertTrue(trackings.size == --currentCount)
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings plugin by first
     * posting a new shipment tracking record for an order using a custom provider, and then
     * deleting that same shipment tracking record.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testAddShipmentTrackingForOrder_customProvider() = runBlocking {
        /*
         * TEST 1: Add a tracking record using a custom provider
         */
        val orderModel = WCOrderModel().apply {
            id = 8
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }

        val testProvider = "Amanda Test Provider"
        val testTrackingNumber = TestUtils.randomString(15)
        val testDateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val testTrackingLink = "https://www.google.com"

        val trackingModel = WCOrderShipmentTrackingModel().apply {
            trackingProvider = testProvider
            trackingNumber = testTrackingNumber
            dateShipped = testDateShipped
            trackingLink = testTrackingLink
        }
        orderStore.addOrderShipmentTracking(
                AddOrderShipmentTrackingPayload(
                        sSite, orderModel.id, orderModel.remoteOrderId, trackingModel, isCustomProvider = true
                )
        )
        var trackings = orderStore.getShipmentTrackingsForOrder(
                sSite, orderModel.id
        )
        assertTrue(trackings.isNotEmpty())

        var trackingResult: WCOrderShipmentTrackingModel? = null
        trackings.forEach {
            if (it.trackingNumber == testTrackingNumber && it.dateShipped == testDateShipped) {
                trackingResult = it
                return@forEach
            }
        }
        assertNotNull(trackingResult)
        with(trackingResult!!) {
            assertEquals(trackingProvider, testProvider)
            assertEquals(trackingNumber, testTrackingNumber)
            assertEquals(dateShipped, testDateShipped)
            assertEquals(trackingLink, testTrackingLink)
        }

        /*
         * TEST 2: Delete the previously added shipment tracking record
         */
        val onOrderChanged = orderStore.deleteOrderShipmentTracking(
                DeleteOrderShipmentTrackingPayload(sSite, orderModel.id, orderModel.remoteOrderId, trackingResult!!)
        )

        assertFalse(onOrderChanged.isError)
        // Verify the tracking record is no longer in the database
        var currentCount = trackings.size
        trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.id)
        assertTrue(trackings.size == --currentCount)
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings
     * plugin by attempting to fetch the shipment providers for an order. This plugin requires
     * the order ID to fetch the list of shipment providers even though these providers are not
     * linked to an order_id. It's strange, but that's the way it's written currently.
     *
     * There is a ticket to get this fixed:
     * https://github.com/woocommerce/woocommerce-shipment-tracking/issues/97
     */
    @Throws(InterruptedException::class)
    @Test
    fun testFetchShipmentProviders() {
        nextEvent = TestEvent.FETCHED_ORDER_SHIPMENT_PROVIDERS
        mCountDownLatch = CountDownLatch(1)

        val orderModel = WCOrderModel().apply {
            id = 8
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }
        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(
                FetchOrderShipmentProvidersPayload(sSite, orderModel)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val providers = orderStore.getShipmentProvidersForSite(sSite)
        assertTrue(providers.isNotEmpty())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderShipmentProvidersChanged(event: OnOrderShipmentProvidersChanged) {
        event.error?.let {
            throw AssertionError("onOrderShipmentProvidersChanged has unexpected error: " + it.type)
        }

        lastEvent = event

        assertEquals(TestEvent.FETCHED_ORDER_SHIPMENT_PROVIDERS, nextEvent)
        mCountDownLatch.countDown()
    }
}
