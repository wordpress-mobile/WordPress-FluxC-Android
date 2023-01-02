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

    private val testSite = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
        siteId = 123L
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
    }

    @Test
    fun whenValidMessagePathPassedForJitmThenSuccessReturned() = runBlocking {
        interceptor.respondWith("jitm-fetch-success.json")
        val messagePath = "woomobile:my_store:admin_notices"

        val result = restClient.fetchJitmMessage(testSite, messagePath, "")

        assertFalse(result.isError)
        assertTrue(!result.result.isNullOrEmpty())
    }

    @Test
    fun whenInValidMessagePathPassedForJitmThenSuccessReturnedWithEmptyJitms() = runBlocking {
        interceptor.respondWith("jitm-fetch-success-empty.json")
        val messagePath = ""

        val result = restClient.fetchJitmMessage(testSite, messagePath, "")

        assertFalse(result.isError)
        assertTrue(result.result.isNullOrEmpty())
    }

    @Test
    fun whenJitmFetchErrorThenReturnError() = runBlocking {
        interceptor.respondWithError("jitm-fetch-failure.json", 500)
        val messagePath = ""

        val result = restClient.fetchJitmMessage(testSite, messagePath, "")

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }

    @Test
    fun whenJitmDismissedSuccessfullyThenSuccessReturned() = runBlocking {
        interceptor.respondWith("jitm-dismiss-success.json")

        val result = restClient.dismissJitmMessage(
            testSite,
            jitmId = "123",
            featureClass = ""
        )

        assertFalse(result.isError)
        assertTrue(result.result!!)
    }

    @Test
    fun whenJitmDismissedfailsThenFailureReturned() = runBlocking {
        interceptor.respondWithError("jitm-dismiss-failure.json", 500)

        val result = restClient.dismissJitmMessage(
            testSite,
            jitmId = "123",
            featureClass = ""
        )

        assertTrue(result.isError)
        assertEquals(API_ERROR, result.error.type)
    }
}
