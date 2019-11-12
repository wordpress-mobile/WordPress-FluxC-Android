package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_ReactNativeWPAPIRequestTest : ReleaseStack_Base() {
    @Inject lateinit var reactNativeStore: ReactNativeStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Test
    fun testWPAPICall() {
        authenticate()

        val url = "http://do.wpmt.co/jp-1/wp-json/wp/v2/media/"
        val params = mapOf("context" to "view")
        val response = runBlocking { reactNativeStore.performWPAPIRequest(url, params) }

        val failureMessage = "Call failed with error: ${(response as? Error)?.error}"
        assertTrue(failureMessage, response is Success)
    }

    private fun authenticate() {
        val payload = AuthenticatePayload(
                BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY
        )
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        assertTrue(siteStore.sitesCount > 0)
    }

    @Subscribe
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        mCountDownLatch.countDown()
    }

    @Subscribe
    fun onAccountChanged(event: OnAccountChanged) {
        AppLog.d(T.TESTS, "Received OnAccountChanged event")
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        mCountDownLatch.countDown()
    }

    @Subscribe
    fun onSiteChanged(event: OnSiteChanged) {
        AppLog.i(T.TESTS, "site count " + siteStore.sitesCount)
        assertTrue(siteStore.hasSite())
        mCountDownLatch.countDown()
    }
}
