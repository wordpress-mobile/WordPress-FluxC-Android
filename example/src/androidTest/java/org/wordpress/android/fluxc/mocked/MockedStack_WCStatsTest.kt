package org.wordpress.android.fluxc.mocked

import com.android.volley.RequestQueue
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchVisitorStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates.notNull

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_WCStatsTest : MockedStack_Base() {
    @Inject internal lateinit var orderStatsRestClient: OrderStatsRestClient
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject @Named("regular") internal lateinit var requestQueue: RequestQueue

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

        with(payload.stats!!) {
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

        // Make the same stats request but adding custom stats
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY,
                "2018-04-20", 7, startDate = "2018-04-14", endDate = "2018-04-20")

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

    @Test
    fun testStatsFetchResponseNullError() {
        val errorJson = JsonObject().apply {
            addProperty("error", OrderStatsErrorType.RESPONSE_NULL.name)
            addProperty("message", "Response object is null")
        }

        interceptor.respondWithError(errorJson)
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY, "2019-02-01", 7)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_ORDER_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderStatsResponsePayload
        assertNotNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(OrderStatsApiUnit.DAY, payload.apiUnit)
        assertNull(payload.stats)
        assertEquals(OrderStatsErrorType.RESPONSE_NULL, payload.error.type)
    }

    @Test
    fun testFetchTopEarnersStatsSuccess() {
        interceptor.respondWith("wc-top-earners-response-success.json")
        orderStatsRestClient.fetchTopEarnersStats(siteModel, OrderStatsApiUnit.DAY, "2018-04-20", 10, true)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_TOP_EARNERS_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchTopEarnersStatsResponsePayload
        with(payload) {
            assertNull(error)
            assertEquals(siteModel, site)
            assertEquals(OrderStatsApiUnit.DAY, apiUnit)
            assertEquals(topEarners.size, 10)
        }
    }

    @Test
    fun testFetchTopEarnersStatsError() {
        val errorJson = JsonObject().apply {
            addProperty("error", "rest_invalid_param")
            addProperty("message", "Invalid parameter(s): date")
        }

        interceptor.respondWithError(errorJson)
        orderStatsRestClient.fetchTopEarnersStats(siteModel, OrderStatsApiUnit.DAY, "invalid", 10, true)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_TOP_EARNERS_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchTopEarnersStatsResponsePayload
        with(payload) {
            assertNotNull(error)
            assertEquals(siteModel, site)
            assertEquals(OrderStatsApiUnit.DAY, apiUnit)
            assertEquals(topEarners.size, 0)
            assertEquals(OrderStatsErrorType.INVALID_PARAM, error.type)
        }
    }

    @Test
    fun testVisitorStatsSuccess() {
        interceptor.respondWith("wc-visitor-stats-response-success.json")
        orderStatsRestClient.fetchVisitorStats(siteModel, OrderStatsApiUnit.MONTH, "2018-04-20", 12, true)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_VISITOR_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchVisitorStatsResponsePayload
        with(payload) {
            assertNull(error)
            assertEquals(siteModel, site)
            assertEquals(OrderStatsApiUnit.MONTH, apiUnit)
            assertNotNull(stats)
            assertNotNull(stats?.data)
            assertEquals(stats?.dataList?.size, 12)
        }
    }

    @Test
    fun testVisitorStatsError() {
        val errorJson = JsonObject().apply {
            addProperty("error", "rest_invalid_param")
            addProperty("message", "Invalid parameter(s): date")
        }

        interceptor.respondWithError(errorJson)
        orderStatsRestClient.fetchVisitorStats(siteModel, OrderStatsApiUnit.MONTH, "invalid", 1, true)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_VISITOR_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchVisitorStatsResponsePayload
        with(payload) {
            assertNotNull(error)
            assertEquals(siteModel, site)
            assertEquals(OrderStatsApiUnit.MONTH, apiUnit)
            assertNull(stats)
            assertEquals(OrderStatsErrorType.INVALID_PARAM, error.type)
        }
    }

    @Test
    fun testNewVisitorStatsSuccess() = runBlocking {
        interceptor.respondWith("wc-visitor-stats-response-success.json")
        val payload = orderStatsRestClient.fetchNewVisitorStats(
                siteModel, OrderStatsApiUnit.MONTH, StatsGranularity.YEARS, "2019-08-06", 8, true
        )

        with(payload) {
            assertNull(error)
            assertEquals(siteModel, site)
            assertEquals(StatsGranularity.YEARS, granularity)
            assertNotNull(stats)
            assertNotNull(stats?.data)
            assertEquals(stats?.dataList?.size, 12)
        }
    }

    @Test
    fun testNewVisitorStatsError() = runBlocking {
        val errorJson = JsonObject().apply {
            addProperty("error", "rest_invalid_param")
            addProperty("message", "Invalid parameter(s): date")
        }

        interceptor.respondWithError(errorJson)

        val payload = orderStatsRestClient.fetchNewVisitorStats(
                siteModel, OrderStatsApiUnit.MONTH, StatsGranularity.YEARS, "2019-08-06", 8, true
        )

        with(payload) {
            assertNotNull(error)
            assertEquals(siteModel, site)
            assertEquals(StatsGranularity.YEARS, granularity)
            assertNull(stats)
            assertEquals(OrderStatsErrorType.INVALID_PARAM, error.type)
        }
    }

    @Test
    fun testCustomStatsFetchSuccess() {
        interceptor.respondWith("wc-order-stats-response-success.json")
        orderStatsRestClient.fetchStats(siteModel, OrderStatsApiUnit.DAY,
                "2019-01-31", 31, startDate = "2019-01-01",
                endDate = "2019-01-31")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_ORDER_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchOrderStatsResponsePayload
        assertNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(OrderStatsApiUnit.DAY, payload.apiUnit)
        assertNotNull(payload.stats)
        assertEquals(OrderStatsApiUnit.DAY.name.toLowerCase(), payload.stats?.unit)
        assertEquals("2019-01-01", payload.stats?.startDate)
        assertEquals("2019-01-31", payload.stats?.endDate)
        assertEquals(true, payload.stats?.isCustomField)

        with(payload.stats!!) {
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
    fun testCustomVisitorStatsFetchSuccess() {
        interceptor.respondWith("wc-visitor-stats-response-success.json")
        orderStatsRestClient.fetchVisitorStats(
                siteModel, OrderStatsApiUnit.DAY, "2019-01-01", 12, true,
                startDate = "2019-01-01",
                endDate = "2019-01-12"
        )

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_VISITOR_STATS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchVisitorStatsResponsePayload
        assertNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(OrderStatsApiUnit.DAY, payload.apiUnit)
        assertNotNull(payload.stats)
        assertEquals(OrderStatsApiUnit.DAY.name.toLowerCase(), payload.stats?.unit)
        assertEquals("2019-01-01", payload.stats?.startDate)
        assertEquals("2019-01-12", payload.stats?.endDate)
        assertEquals(true, payload.stats?.isCustomField)

        with(payload.stats!!) {
            assertEquals(siteModel.id, localSiteId)
            assertEquals(OrderStatsApiUnit.DAY.toString(), unit)
            assertEquals(2, fieldsList.size)
            assertEquals(12, dataList.size)
            assertEquals(1.0, dataList[0][1])

            val visitorIndex = fieldsList.indexOf("visitors")
            assertEquals(12, dataList.map { (it[visitorIndex] as Number).toInt() }.sum())
        }
    }

    @Test
    fun testRevenueStatsDayFetchSuccess() = runBlocking {
        interceptor.respondWith("wc-revenue-stats-response-success.json")
        val payload = orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "2019-07-01T00:00:00", endDate = "2019-07-07T23:59:59",
                perPage = 35
        )

        assertNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(StatsGranularity.DAYS, payload.granularity)
        assertNotNull(payload.stats)

        with(payload.stats!!) {
            assertEquals(siteModel.id, localSiteId)
            assertEquals(StatsGranularity.DAYS.toString(), interval)

            val intervals = getIntervalList()
            val startInterval = intervals.first().interval
            val endInterval = intervals.last().interval
            assertEquals("2019-07-01", startInterval)
            assertEquals("2019-07-07", endInterval)

            val total = parseTotal()
            assertNotNull(total)
            assertEquals(11, total?.ordersCount)
            assertEquals(301.99, total?.totalSales)
        }
    }

    @Test
    fun testRevenueStatsDayFetchJsonError() = runBlocking {
        interceptor.respondWith("wc-revenue-stats-response-empty.json")
        val payload = orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "2019-07-07T00:00:00", endDate = "2019-07-01T23:59:59",
                perPage = 35
        )

        assertNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(StatsGranularity.DAYS, payload.granularity)
        assertNotNull(payload.stats)

        with(payload.stats!!) {
            assertEquals(siteModel.id, localSiteId)
            assertEquals(StatsGranularity.DAYS.toString(), interval)

            val intervals = getIntervalList()
            val total = parseTotal()
            assertEquals(0, intervals.size)
            assertNull(total)
        }
    }

    @Test
    fun testRevenueStatsFetchCaching() = runBlocking {
        requestQueue.cache.clear()

        // Make initial stats request
        interceptor.respondWith("wc-revenue-stats-response-success.json")
        orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "2019-07-01T00:00:00", endDate = "2019-07-07T23:59:59",
                perPage = 35
        )

        val firstRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(firstRequestCacheEntry)

        // Make the same stats request - this should hit the cache
        interceptor.respondWith("wc-revenue-stats-response-success.json")
        orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "2019-07-01T00:00:00", endDate = "2019-07-07T23:59:59",
                perPage = 35
        )

        val secondRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(secondRequestCacheEntry)
        // Verify that the cache has not been renewed,
        // which should mean that we read from it instead of making a network call
        assertEquals(firstRequestCacheEntry.ttl, secondRequestCacheEntry.ttl)

        // Make the same stats request, but this time pass force=true to force a network request
        interceptor.respondWith("wc-revenue-stats-response-success.json")
        orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "2019-07-01T00:00:00", endDate = "2019-07-07T23:59:59",
                perPage = 35, force = true
        )

        val thirdRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(thirdRequestCacheEntry)
        // The cache should have been renewed, since we ignored it and updated it with the results of a forced request
        assertNotEquals(secondRequestCacheEntry.ttl, thirdRequestCacheEntry.ttl)

        // New day, cache should be ignored
        interceptor.respondWith("wc-revenue-stats-response-success.json")
        orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "2019-07-02T00:00:00", endDate = "2019-07-08T23:59:59",
                perPage = 35
        )

        val newDayCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(newDayCacheEntry)
        // This should be a separate cache entry from the previous day's
        assertNotEquals(thirdRequestCacheEntry.ttl, newDayCacheEntry.ttl)
    }

    @Test
    fun testRevenueStatsFetchInvalidParamError() = runBlocking {
        val errorJson = JsonObject().apply {
            addProperty("error", "rest_invalid_param")
            addProperty("message", "Invalid parameter(s): after")
        }

        interceptor.respondWithError(errorJson)
        val payload = orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "invalid", endDate = "2019-07-07T23:59:59", perPage = 35
        )

        assertNotNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(StatsGranularity.DAYS, payload.granularity)
        assertNull(payload.stats)
        assertEquals(OrderStatsErrorType.INVALID_PARAM, payload.error.type)
    }

    @Test
    fun testRevenueStatsFetchResponseNullError() = runBlocking {
        val errorJson = JsonObject().apply {
            addProperty("error", OrderStatsErrorType.RESPONSE_NULL.name)
            addProperty("message", "Response object is null")
        }

        interceptor.respondWithError(errorJson)
        val payload = orderStatsRestClient.fetchRevenueStats(
                site = siteModel, granularity = StatsGranularity.DAYS,
                startDate = "invalid", endDate = "2019-07-07T23:59:59", perPage = 35
        )

        assertNotNull(payload.error)
        assertEquals(siteModel, payload.site)
        assertEquals(StatsGranularity.DAYS, payload.granularity)
        assertNull(payload.stats)
        assertEquals(OrderStatsErrorType.RESPONSE_NULL, payload.error.type)
    }

    @Test
    fun testFetchRevenueStatsAvailabilitySuccess() {
        interceptor.respondWith("wc-revenue-stats-response-success.json")
        orderStatsRestClient.fetchRevenueStatsAvailability(siteModel, "2019-07-30T00:00:00")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_REVENUE_STATS_AVAILABILITY, lastAction!!.type)
        val payload = lastAction!!.payload as FetchRevenueStatsAvailabilityResponsePayload
        with(payload) {
            assertNull(error)
            assertEquals(siteModel, site)
            assertTrue(available)
        }
    }

    @Test
    fun testFetchRevenueStatsAvailabilityError() {
        val errorJson = JsonObject().apply {
            addProperty("error", "rest_no_route")
            addProperty("message", "No route was found matching the URL and request method")
        }

        interceptor.respondWithError(errorJson)
        orderStatsRestClient.fetchRevenueStatsAvailability(siteModel, "2019-07-30T00:00:00")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCStatsAction.FETCHED_REVENUE_STATS_AVAILABILITY, lastAction!!.type)
        val payload = lastAction!!.payload as FetchRevenueStatsAvailabilityResponsePayload
        with(payload) {
            assertNotNull(error)
            assertEquals(siteModel, site)
            assertFalse(available)
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type is WCStatsAction) {
            lastAction = action
            countDownLatch.countDown()
        }
    }
}
