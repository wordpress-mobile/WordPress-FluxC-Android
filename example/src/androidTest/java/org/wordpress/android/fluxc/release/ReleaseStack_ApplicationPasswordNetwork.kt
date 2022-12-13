package org.wordpress.android.fluxc.release

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SiteModel.SiteOrigin
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordNetwork
import org.wordpress.android.fluxc.release.ReleaseStack_PostSchedulingTestJetpack.TestEvents
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType.DUPLICATE_SITE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

@RunWith(Parameterized::class)
internal class ReleaseStack_ApplicationPasswordNetwork(
    @SiteOrigin private val origin: Int
) : ReleaseStack_Base() {
    companion object {
        @Parameters
        @JvmStatic
        fun cases() = listOf(SiteModel.ORIGIN_WPCOM_REST, SiteModel.ORIGIN_XMLRPC)
    }

    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject internal lateinit var applicationPasswordNetwork: ApplicationPasswordNetwork
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore

    private lateinit var site: SiteModel

    private var nextEvent: TestEvents? = null

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
        site = getSite()
    }

    @Test
    fun testApplicationPassword() = runBlocking {
        val response = applicationPasswordNetwork.executeGetGsonRequest(site, WPAPI.users.me.urlV2, Unit::class.java)
        Assert.assertTrue(response is WPAPIResponse.Success)
    }

    override fun tearDown() {
        runBlocking {
            applicationPasswordNetwork.deleteApplicationPassword(site)
        }
        super.tearDown()
    }

    private fun getSite(): SiteModel {
        return when (origin) {
            SiteModel.ORIGIN_WPCOM_REST -> getJetpackSite()
            SiteModel.ORIGIN_XMLRPC -> getNonJetpackSite()
            else -> error("Unsupported test case")
        }
    }

    private fun getJetpackSite(): SiteModel {
        authenticateWPComAndFetchSites()
        return siteStore.sites[0]
    }

    private fun getNonJetpackSite(): SiteModel {
        return SiteModel().apply {
            id = 1
            selfHostedSiteId = 0
            // TODO update this to the value TEST_WPORG_URL_SH_SIMPLE when the non-jetpack site's WP version is updated
            url = BuildConfig.TEST_WPORG_URL_JETPACK_SUBFOLDER.replace("http:", "https:")
            username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE
            password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE
        }
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
        if (event.isError) {
            if (nextEvent == TestEvents.ERROR_DUPLICATE_SITE) {
                Assert.assertEquals(DUPLICATE_SITE, event.error.type)
                mCountDownLatch.countDown()
                return
            }
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        mCountDownLatch.countDown()
    }

    @Throws(InterruptedException::class)
    private fun authenticateWPComAndFetchSites() {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        val payload = AuthenticatePayload(
            BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
            BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY
        )
        mCountDownLatch = CountDownLatch(1)

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload))
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = CountDownLatch(1)
        nextEvent = TestEvents.SITE_CHANGED
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(FetchSitesPayload()))

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        Assert.assertTrue(siteStore.sitesCount > 0)
    }
}
