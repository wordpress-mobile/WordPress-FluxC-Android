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
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsAvailabilityResponsePayload
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
        origin = SiteModel.ORIGIN_WPCOM_REST
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
            perPage = 35, forceRefresh = true
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
