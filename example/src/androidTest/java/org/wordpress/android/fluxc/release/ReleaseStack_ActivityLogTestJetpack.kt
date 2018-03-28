package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.ActivityLogActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_ActivityLogTestJetpack : ReleaseStack_Base() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    @Inject lateinit var activityLogStore: ActivityLogStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore

    private var nextEvent: TestEvents? = null

    internal enum class TestEvents {
        NONE,
        SITE_CHANGED,
        SITE_REMOVED,
        ERROR_DUPLICATE_SITE
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvents.NONE
    }

    @Test
    fun testFetchActivities() {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY)

        val site = siteStore.sites[0]

        this.mCountDownLatch = CountDownLatch(1)
        val numOfActivitiesRequested = 1
        val payload = ActivityLogStore.FetchActivityLogPayload(site, numOfActivitiesRequested, 0)
        activityLogStore.onAction(ActivityLogActionBuilder.newFetchActivitiesAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(incomingActions.size == 1)
        assertNotNull((incomingActions[0].payload as ActivityLogStore.FetchedActivityLogPayload)
                .activityLogModels)
    }

    @Test
    fun testFetchRewindState() {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY)

        val site = siteStore.sites[0]

        this.mCountDownLatch = CountDownLatch(1)
        val payload = ActivityLogStore.FetchRewindStatePayload(site, 10, 0)
        activityLogStore.onAction(ActivityLogActionBuilder.newFetchRewindStateAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(incomingActions.size == 1)
        val fetchedRewindStatePayload = incomingActions[0].payload as ActivityLogStore.FetchedRewindStatePayload
        with(fetchedRewindStatePayload) {
            assertNotNull(this.rewindStatusModelResponse)
            assertNotNull(this.rewindStatusModelResponse?.state)
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
                assertEquals(SiteErrorType.DUPLICATE_SITE, event.error.type)
                mCountDownLatch.countDown()
                return
            }
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertTrue(siteStore.hasSite())
        assertEquals(TestEvents.SITE_CHANGED, nextEvent)
        mCountDownLatch.countDown()
    }

    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type is ActivityLogAction) {
            incomingActions.add(action)
            mCountDownLatch?.countDown()
        }
    }

    @Throws(InterruptedException::class)
    private fun authenticateWPComAndFetchSites(username: String, password: String) {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        val payload = AuthenticatePayload(username, password)
        mCountDownLatch = CountDownLatch(1)

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload))
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = CountDownLatch(1)
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = CountDownLatch(1)
        nextEvent = TestEvents.SITE_CHANGED
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(siteStore.sitesCount > 0)
    }
}
