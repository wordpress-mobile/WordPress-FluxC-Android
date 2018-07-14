package org.wordpress.android.fluxc.mocked

import com.android.volley.RequestQueue
import com.google.gson.JsonObject
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_WCStatsTest : MockedStack_Base() {
    @Inject internal lateinit var orderStatsRestClient: OrderStatsRestClient
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var requestQueue: RequestQueue

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
    fun testStatsDayFetchSuccess() {
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "2018-04-20", 7)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_ORDER_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderStatsResponsePayload
        assertNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(OrderStatsApiUnit.DAY, payload.apiUnit)
        assertNotNull(payload.stats)

        with (payload.stats!!) {
            assertEquals(siteModel.id, localSiteId)
            assertEquals(OrderStatsApiUnit.DAY.toString(), unit)
            assertEquals(18, fieldsList.size)
            assertEquals(7, dataList.size)
            assertEquals(18, dataList[0].size)

            val revenueIndex = fieldsList.indexOf("total_sales")
            assertEquals(182.5, dataList.map { it[revenueIndex] as Double }.sum(), 0.01)

            val periodIndex = fieldsList.indexOf("period")
            assertEquals("2018-04-14", dataList.first()[periodIndex])
            assertEquals("2018-04-20", dataList.last()[periodIndex])
        }
    }

    @Test
    fun testStatsFetchCaching() {
        requestQueue.cache.clear()

        // Make initial stats request
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "2018-04-20", 7)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val firstRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(firstRequestCacheEntry)

        // Make the same stats request - this should hit the cache
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "2018-04-20", 7)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val secondRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(secondRequestCacheEntry)
        // Verify that the cache has not been renewed,
        // which should mean that we read from it instead of making a network call
        assertEquals(firstRequestCacheEntry.ttl, secondRequestCacheEntry.ttl)

        // Make the same stats request, but this time pass force=true to force a network request
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "2018-04-20", 7, true)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val thirdRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(thirdRequestCacheEntry)
        // The cache should have been renewed, since we ignored it and updated it with the results of a forced request
        assertNotEquals(secondRequestCacheEntry.ttl, thirdRequestCacheEntry.ttl)

        // New day, cache should be ignored
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "2018-04-21", 7)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val newDayCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(newDayCacheEntry)
        // This should be a separate cache entry from the previous day's
        assertNotEquals(thirdRequestCacheEntry.ttl, newDayCacheEntry.ttl)
    }

    @Test
    fun testStatsFetchInvalidParamError() {
        val errorJson = JsonObject().apply {
            addProperty("error", "rest_invalid_param")
            addProperty("message", "Invalid parameter(s): date")
        }

        interceptor.respondWithError(errorJson)
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "invalid", 7)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_ORDER_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderStatsResponsePayload
        assertNotNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(OrderStatsApiUnit.DAY, payload.apiUnit)
        assertNull(payload.stats)
        assertEquals(OrderStatsErrorType.INVALID_PARAM, payload.error.type)
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
