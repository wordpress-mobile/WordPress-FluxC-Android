package org.wordpress.android.fluxc.release

import junit.framework.Assert.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.model.SiteModel
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
    fun testUnauthenticatedCallWorksEvenIfAuthFails() {
        val response = runBlocking {
            val site = SiteModel().apply {
                url = BuildConfig.TEST_WPORG_URL_SH_WPAPI_SIMPLE
                username = "bad_username"
                password = "bad_password"
            }

            // media queries with a context of 'view' do not require authentication
            reactNativeStore.executeRequest(site, "wp/v2/media?context=view")
        }

        val assertionMessage = "Call unexpectedly failed with error: ${(response as? Error)?.error?.message}"
        assertTrue(assertionMessage, response is Success)
        assertNotNull((response as Success).result)
    }

    @Test
    fun testAuthenticatedCallToCustomRestPath() {
        val response = runBlocking {
            // site has a custom rest endpoint of /index.php?rest_route=/ instead of /wp-json/
            val siteWithCustomRestEndpoint = SiteModel().apply {
                url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE
                username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE
                password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE
            }

            // media queries with a context of 'edit' require authentication
            reactNativeStore.executeRequest(siteWithCustomRestEndpoint, "wp/v2/media?context=edit")
        }

        val assertionMessage = "Call unexpectedly failed with error: ${(response as? Error)?.error?.message}"
        assertTrue(assertionMessage, response is Success)
        assertNotNull((response as Success).result)
    }

    @Ignore("failing because TEST_WPORG_URL_SH_SELFSIGNED_SSL redirects to a different domain")
    @Test
    fun testAuthenticatedCallToSelfSignedSslSite() {
        val response = runBlocking {
            val siteUsingSsl = SiteModel().apply {
                url = BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL
                username = BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL
                password = BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL
            }

            // media queries with a context of 'edit' require authentication
            reactNativeStore.executeRequest(siteUsingSsl, "wp/v2/media?context=edit")
        }

        val assertionMessage = "Call unexpectedly failed with error: ${(response as? Error)?.error?.message}"
        assertTrue(assertionMessage, response is Success)
        assertNotNull((response as Success).result)
    }

    @Test
    fun testIncorrectPathReturnsNotFoundError() {
        val response = runBlocking {
            val site = SiteModel().apply {
                url = BuildConfig.TEST_WPORG_URL_SH_WPAPI_SIMPLE
                username = BuildConfig.TEST_WPORG_USERNAME_SH_WPAPI_SIMPLE
                password = BuildConfig.TEST_WPORG_PASSWORD_SH_WPAPI_SIMPLE
            }

            reactNativeStore.executeRequest(site, "wp/v2/an-invalid-endpoint")
        }

        val assertionMessage = "Call should have failed with a 404, instead response was $response"
        val actualStatusCode = (response as? Error)?.error?.volleyError?.networkResponse?.statusCode
        assertEquals(assertionMessage, 404, actualStatusCode)
    }
}
