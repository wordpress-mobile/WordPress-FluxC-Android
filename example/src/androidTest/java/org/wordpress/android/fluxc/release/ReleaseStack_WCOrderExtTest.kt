package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import java.lang.AssertionError
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_WCOrderExtTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_ORDER_SHIPMENT_TRACKINGS
    }

    @Inject internal lateinit var orderStore: WCOrderStore

    override fun buildAuthenticatePayload() = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_WOO_JETPACK_EXTENSIONS,
            BuildConfig.TEST_WPCOM_PASSWORD_WOO_JETPACK_EXTENSIONS)

    private var nextEvent: TestEvent = TestEvent.NONE
    private var lastEvent: OnOrderChanged? = null

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
    fun testFetchShipmentTrackingsForOrder_hasTrackings() {
        nextEvent = TestEvent.FETCHED_ORDER_SHIPMENT_TRACKINGS
        mCountDownLatch = CountDownLatch(1)

        val orderModel = WCOrderModel().apply {
            id = 8
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITH_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }
        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(
                FetchOrderShipmentTrackingsPayload(sSite, orderModel)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val trackings = orderStore.getShipmentTrackingsForOrder(orderModel)
        assertTrue(trackings.isNotEmpty())
        assertEquals(2, trackings.size)
    }

    /**
     * Tests the Woo mobile implementation of the Shipment Trackings
     * plugin by attempting to fetch the trackings for an order that
     * does not have any shipment trackings recorded.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testFetchShipmentTrackingsForOrder_noTrackings() {
        nextEvent = TestEvent.FETCHED_ORDER_SHIPMENT_TRACKINGS
        mCountDownLatch = CountDownLatch(1)

        val orderModel = WCOrderModel().apply {
            id = 9
            remoteOrderId = BuildConfig.TEST_WC_ORDER_WITHOUT_SHIPMENT_TRACKINGS_ID.toLong()
            localSiteId = sSite.id
        }
        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(
                FetchOrderShipmentTrackingsPayload(sSite, orderModel)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val trackings = orderStore.getShipmentTrackingsForOrder(orderModel)
        assertTrue(trackings.isEmpty())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        event.error?.let {
            throw AssertionError("OnOrderChanged has unexpected error: " + it.type)
        }

        lastEvent = event

        when (event.causeOfChange) {
            FETCH_ORDER_SHIPMENT_TRACKINGS -> {
                assertEquals(TestEvent.FETCHED_ORDER_SHIPMENT_TRACKINGS, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }
}
