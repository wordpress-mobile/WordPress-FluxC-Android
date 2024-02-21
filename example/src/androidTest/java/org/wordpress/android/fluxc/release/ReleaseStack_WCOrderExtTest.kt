@file:Suppress("DEPRECATION_ERROR")
package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

class ReleaseStack_WCOrderExtTest : ReleaseStack_WCBase() {
    @Inject internal lateinit var orderStore: WCOrderStore

    override fun buildAuthenticatePayload() = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JETPACK_EXTENSIONS,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JETPACK_EXTENSIONS)

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings
     * plugin by attempting to fetch the trackings for an order that
     * has shipment trackings recorded.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testFetchShipmentTrackingsForOrder_hasTrackings() = runBlocking {
        val orderModel = OrderEntity(
            orderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong(),
            localSiteId = sSite.localId()
        )
        orderStore.fetchOrderShipmentTrackings(orderModel.orderId, sSite)

        val trackings = orderStore.getShipmentTrackingsForOrder(
                sSite, orderModel.orderId
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
        val orderModel = OrderEntity(
            orderId = BuildConfig.TEST_WC_ORDER_WITHOUT_SHIPMENT_TRACKINGS_ID.toLong(),
            localSiteId = sSite.localId()
        )

        orderStore.fetchOrderShipmentTrackings(orderModel.orderId, sSite)

        val trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.orderId)
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
        val orderModel = OrderEntity(
            orderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong(),
            localSiteId = sSite.localId()
        )

        val testProvider = "TNT Express (consignment)"
        val testTrackingNumber = TestUtils.randomString(15)
        val testDateShipped = SimpleDateFormat("yyyy-MM-dd").format(Date())

        val trackingModel = WCOrderShipmentTrackingModel().apply {
            trackingProvider = testProvider
            trackingNumber = testTrackingNumber
            dateShipped = testDateShipped
        }
        orderStore.addOrderShipmentTracking(AddOrderShipmentTrackingPayload(
                sSite, orderModel.orderId, trackingModel, isCustomProvider = false)
        )

        var trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.orderId)
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
                DeleteOrderShipmentTrackingPayload(sSite, orderModel.orderId, trackingResult!!)
        )

        assertFalse(onOrderChanged.isError)
        // Verify the tracking record is no longer in the database
        var currentCount = trackings.size
        trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.orderId)
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
        val orderModel = OrderEntity(
            orderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong(),
            localSiteId = sSite.localId()
        )

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
                        sSite, orderModel.orderId, trackingModel, isCustomProvider = true
                )
        )
        var trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.orderId)
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
                DeleteOrderShipmentTrackingPayload(
                        sSite, orderModel.orderId, trackingResult!!
                )
        )

        assertFalse(onOrderChanged.isError)
        // Verify the tracking record is no longer in the database
        var currentCount = trackings.size
        trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.orderId)
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
    fun testFetchShipmentProviders() = runBlocking {
        val orderModel = OrderEntity(
                orderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong(),
                localSiteId = sSite.localId()
        )

        orderStore.fetchOrderShipmentProviders(FetchOrderShipmentProvidersPayload(sSite, orderModel))

        val providers = orderStore.getShipmentProvidersForSite(sSite)
        assertTrue(providers.isNotEmpty())
    }

    @Test
    fun givenOrderDoesntExit_WhenFetchOrderReceipt_ThenErrorReturned() = runBlocking {
        val result = orderStore.fetchOrdersReceipt(
            sSite,
            Long.MAX_VALUE,
        )

        assertTrue(result.isError)
    }
}
