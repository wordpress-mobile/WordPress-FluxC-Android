package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReleaseStack_WCOrderTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_ORDERS,
        FETCHED_ORDER_NOTES
    }

    @Inject internal lateinit var orderStore: WCOrderStore

    private var nextEvent: TestEvent = TestEvent.NONE

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrders() {
        nextEvent = TestEvent.FETCHED_ORDERS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(FetchOrdersPayload(sSite, false)))

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val firstFetchOrders = orderStore.getOrdersForSite(sSite).size

        assertTrue(firstFetchOrders > 0 && firstFetchOrders <= WCOrderStore.NUM_ORDERS_PER_FETCH)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrderNotes() {
        // Grab a list of orders
        nextEvent = TestEvent.FETCHED_ORDERS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(FetchOrdersPayload(sSite, false)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Fetch notes for the first order returned
        val firstOrder = orderStore.getOrdersForSite(sSite)[0]
        nextEvent = TestEvent.FETCHED_ORDER_NOTES
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(FetchOrderNotesPayload(firstOrder, sSite)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Verify results
        val fetchedNotes = orderStore.getOrderNotesForOrder(firstOrder)
        assertTrue(fetchedNotes.isNotEmpty())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        event.error?.let {
            throw AssertionError("OnOrderChanged has error: " + it.type)
        }

        when (event.causeOfChange) {
            WCOrderAction.FETCH_ORDERS -> {
                assertEquals(TestEvent.FETCHED_ORDERS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCOrderAction.FETCH_ORDER_NOTES -> {
                assertEquals(TestEvent.FETCHED_ORDER_NOTES, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }
}
