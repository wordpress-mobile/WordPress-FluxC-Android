package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class ReleaseStack_ReactNativeWPAPIRequestTest : ReleaseStack_Base() {
    @Inject lateinit var reactNativeStore: ReactNativeStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
    }

    @Test
    fun testWPAPICallOnSelfHosted() {
        val url = BuildConfig.TEST_WPORG_URL_SH_WPAPI_SIMPLE + "/wp-json/wp/v2/media/"
        val params = mapOf("context" to "view")
        val response = runBlocking { reactNativeStore.performWPAPIRequest(url, params) }

        val failureMessage = "Call failed with error: ${(response as? Error)?.error}"
        assertTrue(failureMessage, response is Success)
    }
}
