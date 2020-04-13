package org.wordpress.android.fluxc.release

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeStore
import javax.inject.Inject

class ReleaseStack_ReactNativeWPComRequestTest : ReleaseStack_WPComBase() {
    @Inject lateinit var reactNativeStore: ReactNativeStore

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Test
    fun testWpComCall_leading_slash() {
        assertSuccessWithPath("/wp/v2/media?context=edit")
    }

    @Test
    fun testWpComCall_no_leading_slash() {
        assertSuccessWithPath("wp/v2/media?context=edit")
    }

    private fun assertSuccessWithPath(path: String) {
        val response = runBlocking { reactNativeStore.executeRequest(sSite, path) }
        val failureMessage = "Call failed with error: ${(response as? Error)?.error}"
        assertTrue(failureMessage, response is Success)
    }

    @Test
    fun testWpComCall_fails() {
        val response = runBlocking { reactNativeStore.executeRequest(sSite, "an-invalid-extension") }
        val assertionMessage = "Call should have failed with a 404, instead response was $response"
        val actualStatusCode = (response as? Error)?.error?.volleyError?.networkResponse?.statusCode
        assertEquals(assertionMessage, 404, actualStatusCode)
    }
}
