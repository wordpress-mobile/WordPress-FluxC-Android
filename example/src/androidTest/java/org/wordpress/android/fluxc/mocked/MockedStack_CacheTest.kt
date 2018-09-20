package org.wordpress.android.fluxc.mocked

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.Delegates.notNull

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_CacheTest : MockedStack_Base() {
    companion object {
        private const val requestUrl = "https://public-api.wordpress.com/rest/v1/testrequest"

        private val responseJson = JsonObject().apply { addProperty("success", "yes") }

        private val networkErrorHandler = { networkError: WPComGsonNetworkError ->
            throw AssertionError("Unexpected error with type: " + networkError.type)
        }
    }

    @Inject internal lateinit var wpComRestClient: WPComRestClientForTests
    @Inject internal lateinit var requestQueue: RequestQueue

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by notNull()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        lastAction = null
    }

    @Test
    fun testSingleRepeatCaching() {
        requestQueue.cache.clear()

        // Make initial request
        with(prepareAndCreateRequest()) {
            enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val firstRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(firstRequestCacheEntry)

        // Repeat same request
        with(prepareAndCreateRequest()) {
            enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val secondRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(secondRequestCacheEntry)
        // Verify that the cache has not been renewed,
        // which should mean that we read from it instead of making a network call
        assertEquals(firstRequestCacheEntry.ttl, secondRequestCacheEntry.ttl)
    }

    @Test
    fun testCacheExpiry() {
        requestQueue.cache.clear()

        // Make initial request with 1 millisecond cache expiry
        with(prepareAndCreateRequest()) {
            enableCaching(1)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        TestUtils.waitFor(2) // Make sure the cache expires

        val firstRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(firstRequestCacheEntry)
        assertTrue(firstRequestCacheEntry.isExpired) // Should already be expired by the time we get here

        // Repeat same request
        with(prepareAndCreateRequest()) {
            enableCaching(1)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val secondRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(secondRequestCacheEntry)
        // Verify that the cache has been renewed, since the entry was expired
        assertNotEquals(firstRequestCacheEntry.ttl, secondRequestCacheEntry.ttl)
    }

    @Test
    fun testGetParamCaching() {
        requestQueue.cache.clear()

        // Make initial request
        val paramMap = mutableMapOf("param" to "testvalue")
        with(prepareAndCreateRequest(paramMap)) {
            enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val firstRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(firstRequestCacheEntry)

        // Repeat same request but with a parameter change
        paramMap["param"] = "differentvalue"
        with(prepareAndCreateRequest(paramMap)) {
            enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val secondRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(secondRequestCacheEntry)
        // Verify that the cache has been renewed, since the request params were different
        assertNotEquals(firstRequestCacheEntry.ttl, secondRequestCacheEntry.ttl)
    }

    @Test
    fun testForcedUpdate() {
        requestQueue.cache.clear()

        // Make initial request
        with(prepareAndCreateRequest()) {
            enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val firstRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(firstRequestCacheEntry)

        // Make the same request, but this time force update the network request
        with(prepareAndCreateRequest()) {
            enableCaching(BaseRequest.DEFAULT_CACHE_LIFETIME)
            setShouldForceUpdate()
            wpComRestClient.exposedAdd(this)
        }

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        val secondRequestCacheEntry = requestQueue.cache.get(interceptor.lastRequestUrl)

        assertNotNull(secondRequestCacheEntry)
        // Verify that the cache has been renewed,
        // since we ignored it and updated it with the results of a forced request
        assertNotEquals(firstRequestCacheEntry.ttl, secondRequestCacheEntry.ttl)
    }

    private fun prepareAndCreateRequest(params: Map<String, String> = mapOf()): WPComGsonRequest<*> {
        interceptor.respondWith(responseJson)

        return WPComGsonRequest.buildGetRequest(
                requestUrl, params, Any::class.java,
                { countDownLatch.countDown() },
                networkErrorHandler
        )
    }

    @Singleton
    class WPComRestClientForTests @Inject constructor(
        appContext: Context,
        dispatcher: Dispatcher,
        requestQueue: RequestQueue,
        accessToken: AccessToken,
        userAgent: UserAgent
    ) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
        /**
         * Wraps and exposes the protected [add] method so that tests can add requests directly.
         */
        fun <T> exposedAdd(request: WPComGsonRequest<T>?) {
            add(request)
        }
    }
}
