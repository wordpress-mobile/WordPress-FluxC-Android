@file:Suppress("DEPRECATION_ERROR")

package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult.Success
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersFetchedByIds
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrdersSearched
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_WCOrderTest : ReleaseStack_WCBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_ORDERS,
        FETCHED_ORDERS_COUNT,
        SEARCHED_ORDERS,
        ERROR_ORDER_STATUS_NOT_FOUND,
        FETCHED_ORDER_STATUS_OPTIONS
    }

    @Inject internal lateinit var orderStore: WCOrderStore

    private var nextEvent: TestEvent = TestEvent.NONE
    private val orderModel by lazy {
        OrderEntity(
                localSiteId = sSite.localId(),
                orderId = 1125,
                number = "1125",
                dateCreated = "2018-04-20T15:45:14Z"
        )
    }
    private var lastEvent: OnOrderChanged? = null
    private val orderSearchQuery = "bogus query"

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
        runBlocking {
            nextEvent = TestEvent.FETCHED_ORDERS
            mCountDownLatch = CountDownLatch(1)

            mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(FetchOrdersPayload(sSite, loadMore = false)))

            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            val firstFetchOrders = orderStore.getOrdersForSite(sSite).size

            assertTrue(firstFetchOrders > 0 && firstFetchOrders <= WCOrderStore.NUM_ORDERS_PER_FETCH)
        }
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrdersByStatus() {
        runBlocking {
            nextEvent = TestEvent.FETCHED_ORDERS
            mCountDownLatch = CountDownLatch(1)
            val statusFilter = "completed"

            mDispatcher.dispatch(
                WCOrderActionBuilder.newFetchOrdersAction(
                    FetchOrdersPayload(
                        sSite,
                        statusFilter,
                        false
                    )
                )
            )
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            val firstFetchOrders = orderStore.getOrdersForSite(sSite)
            val isValid = firstFetchOrders.stream().allMatch { it.status == statusFilter }
            assertTrue(
            firstFetchOrders.isNotEmpty() &&
                firstFetchOrders.size <= WCOrderStore.NUM_ORDERS_PER_FETCH && isValid
            )
        }
    }

    @Test
    fun testFetchOrdersById() {
        runBlocking {
            val idsToRequest = listOf(1128L, 1129L)
            mCountDownLatch = CountDownLatch(1)
            mDispatcher.dispatch(
                WCOrderActionBuilder.newFetchOrdersByIdsAction(
                    FetchOrdersByIdsPayload(
                        sSite,
                        idsToRequest
                    )
                )
            )
            assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

            assertEquals(idsToRequest.size, orderStore.getOrdersForSite(sSite).count())
        }
    }

    @Throws(InterruptedException::class)
    @Test
    fun testSearchOrders() {
        nextEvent = TestEvent.SEARCHED_ORDERS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(
            WCOrderActionBuilder.newSearchOrdersAction(
                SearchOrdersPayload(
                    sSite,
                    orderSearchQuery,
                    0
                )
            )
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrdersCount_completedFilter() {
        nextEvent = TestEvent.FETCHED_ORDERS_COUNT
        mCountDownLatch = CountDownLatch(1)
        val statusFilter = CoreOrderStatus.COMPLETED.value

        mDispatcher.dispatch(
            WCOrderActionBuilder.newFetchOrdersCountAction(FetchOrdersCountPayload(sSite, statusFilter))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        this.lastEvent?.let {
            assertEquals(it.statusFilter, statusFilter)
        } ?: fail()
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrdersCount_emptyFilter() {
        nextEvent = TestEvent.ERROR_ORDER_STATUS_NOT_FOUND
        mCountDownLatch = CountDownLatch(1)
        val statusFilter = ""

        mDispatcher.dispatch(
            WCOrderActionBuilder.newFetchOrdersCountAction(FetchOrdersCountPayload(sSite, statusFilter))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchSingleOrder() = runBlocking {
        orderStore.fetchSingleOrder(sSite, orderModel.orderId)

        val orderFromDb = orderStore.getOrderByIdAndSite(orderModel.orderId, sSite)
        assertTrue(orderFromDb != null && orderFromDb.orderId == orderModel.orderId)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrderNotes() = runBlocking {
        // Grab a list of orders
        nextEvent = TestEvent.FETCHED_ORDERS
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(FetchOrdersPayload(sSite, loadMore = false)))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Fetch notes for the first order returned
        val firstOrder = orderStore.getOrdersForSite(sSite)[0]
        orderStore.fetchOrderNotes(sSite, firstOrder.orderId)

        // Verify results
        val fetchedNotes = orderStore.getOrderNotesForOrder(sSite, firstOrder.orderId)
        assertTrue(fetchedNotes.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testPostOrderNote() = runBlocking {
        orderStore.postOrderNote(
                site = sSite,
                orderId = orderModel.orderId,
                note = "Test rest note",
                isCustomerNote = true
        )

        // Verify results
        val fetchedNotes = orderStore.getOrderNotesForOrder(sSite, orderModel.orderId)
        assertTrue(fetchedNotes.isNotEmpty())
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchHasOrders() = runBlocking {
        val result = orderStore.fetchHasOrders(sSite, status = null)
        assertTrue((result as Success).hasOrders)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchOrderStatusOptions() {
        nextEvent = TestEvent.FETCHED_ORDER_STATUS_OPTIONS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(
            WCOrderActionBuilder
                .newFetchOrderStatusOptionsAction(FetchOrderStatusOptionsPayload(sSite))
        )
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val orderStatusOptions = orderStore.getOrderStatusOptionsForSite(sSite)
        assertTrue(orderStatusOptions.isNotEmpty())
    }

    /**
     * Tests the woo mobile implementation of the Shipment Trackings
     * plugin. This test specifically tests a site where the plugin is not installed.
     */
    @Throws(InterruptedException::class)
    @Test
    fun testFetchShipmentTrackingsForOrder_pluginNotInstalled() = runBlocking {
        val result = orderStore.fetchOrderShipmentTrackings(orderModel.orderId, sSite)
        assertTrue(result.isError)
        assertEquals(OrderErrorType.PLUGIN_NOT_ACTIVE, result.error.type)
        val trackings = orderStore.getShipmentTrackingsForOrder(sSite, orderModel.orderId)
        assertTrue(trackings.isEmpty())
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        event.error?.let {
            when (event.causeOfChange) {
                WCOrderAction.FETCH_ORDERS_COUNT -> {
                    assertEquals(TestEvent.ERROR_ORDER_STATUS_NOT_FOUND, nextEvent)
                    mCountDownLatch.countDown()
                    return
                }
                else -> throw AssertionError("OnOrderChanged has unexpected error: " + it.type)
            }
        }

        lastEvent = event

        when (event.causeOfChange) {
            WCOrderAction.FETCH_ORDERS -> {
                assertEquals(TestEvent.FETCHED_ORDERS, nextEvent)
                mCountDownLatch.countDown()
            }
            WCOrderAction.FETCH_ORDERS_COUNT -> {
                assertEquals(TestEvent.FETCHED_ORDERS_COUNT, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: " + event.causeOfChange)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrdersSearched(event: OnOrdersSearched) {
        event.error?.let {
            throw AssertionError("OnOrdersSearched has error: " + it.type)
        }

        assertEquals(event.searchQuery, orderSearchQuery)
        assertEquals(TestEvent.SEARCHED_ORDERS, nextEvent)
        mCountDownLatch.countDown()
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        event.error?.let {
            throw AssertionError("OnOrderStatusOptionsChanged has error: " + it.type)
        }

        assertEquals(TestEvent.FETCHED_ORDER_STATUS_OPTIONS, nextEvent)
        mCountDownLatch.countDown()
    }

    @Subscribe(threadMode = MAIN)
    fun onOrdersFetchedByIds(event: OnOrdersFetchedByIds) {
        event.error?.let {
            throw AssertionError("OnOrderStatusOptionsChanged has error: " + it.type)
        }
        mCountDownLatch.countDown()
    }
}
