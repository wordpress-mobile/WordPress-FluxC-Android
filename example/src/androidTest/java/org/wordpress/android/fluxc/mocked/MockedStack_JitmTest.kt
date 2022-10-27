package org.wordpress.android.fluxc.mocked

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JitmRestClient
import javax.inject.Inject

class MockedStack_JitmTest : MockedStack_Base() {
    @Inject internal lateinit var restClient: JitmRestClient

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun whenValidMessagePathPassedForJitmThenSuccessReturned() = runBlocking {
        interceptor.respondWith("jitm-fetch-success.json")
        val messagePath = "woomobile:my_store:admin_notices"

        val result = restClient.fetchJitmMessage(SiteModel().apply { siteId = 123L }, messagePath)

        assertFalse(result.isError)
        assertTrue(!result.result.isNullOrEmpty())
    }

    @Test
    fun whenInValidMessagePathPassedForJitmThenSuccessReturnedWithEmptyJitms() = runBlocking {
        interceptor.respondWith("jitm-fetch-success-empty.json")
        val messagePath = ""

        val result = restClient.fetchJitmMessage(SiteModel().apply { siteId = 123L }, messagePath)

        assertFalse(result.isError)
        assertTrue(result.result.isNullOrEmpty())
    }

    @Test
    fun whenJitmFetchErrorThenReturnError() = runBlocking {
        interceptor.respondWithError("jitm-fetch-failure.json", 500)
        val messagePath = ""

        val result = restClient.fetchJitmMessage(SiteModel().apply { siteId = 123L }, messagePath)

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }
}
