package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchedActivityLogPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_ActivityLogTestJetpack : ReleaseStack_Base() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    private val incomingChangeEvents: MutableList<Store.OnChanged<ActivityLogStore.ActivityError>> = mutableListOf()
    @Inject lateinit var activityLogStore: ActivityLogStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore

    private var mNextEvent: TestEvents? = null

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
        mNextEvent = TestEvents.NONE
        this.incomingActions.clear()
        this.incomingChangeEvents.clear()
    }

    @Test
    fun testFetchActivities() {
        val site = authenticate()

        this.mCountDownLatch = CountDownLatch(1)
        val numOfActivitiesRequested = 1
        val payload = ActivityLogStore.FetchActivityLogPayload(site, numOfActivitiesRequested, 0)
        activityLogStore.onAction(ActivityLogActionBuilder.newFetchActivitiesAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(incomingActions.size == 1)
        assertNotNull((incomingActions[0].payload as FetchedActivityLogPayload)
                .activityLogModels)
    }

    @Test
    fun testFetchRewindState() {
        val site = authenticate()

        this.mCountDownLatch = CountDownLatch(1)
        val payload = ActivityLogStore.FetchRewindStatePayload(site)
        activityLogStore.onAction(ActivityLogActionBuilder.newFetchRewindStateAction(payload))
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(incomingActions.size == 1)
        val fetchedRewindStatePayload = incomingActions[0].payload as ActivityLogStore.FetchedRewindStatePayload
        with(fetchedRewindStatePayload) {
            assertNotNull(this.rewindStatusModelResponse)
            assertNotNull(this.rewindStatusModelResponse?.state)
        }
    }

    @Test
    fun storeAndRetrieveActivityLogInOrderByDateFromDb() {
        val site = authenticate()
        this.incomingChangeEvents.clear()

        this.mCountDownLatch = CountDownLatch(1)

        val firstActivity = activityLogModel(1)
        val secondActivity = activityLogModel(2)
        val thirdActivity = activityLogModel(3)
        val activityModels = listOf(firstActivity, secondActivity, thirdActivity)
        val payload = FetchedActivityLogPayload(activityModels, site, 3, 0)

        activityLogStore.onAction(ActivityLogActionBuilder.newFetchedActivitiesAction(payload))

        val activityLogForSite = awaitActivities(site, 3, ascending = false)

        assertEquals(activityLogForSite.size, 3)
        assertEquals(activityLogForSite[0], thirdActivity)
        assertEquals(activityLogForSite[1], secondActivity)
        assertEquals(activityLogForSite[2], firstActivity)
    }

    @Test
    fun updatesActivityWithTheSameActivityId() {
        val site = authenticate()

        this.mCountDownLatch = CountDownLatch(1)

        val activity = activityLogModel(1)
        val payload = FetchedActivityLogPayload(listOf(activity), site, 1, 0)

        activityLogStore.onAction(ActivityLogActionBuilder.newFetchedActivitiesAction(payload))

        val activityLogForSite = awaitActivities(site, 1)

        assertEquals(activityLogForSite.size, 1)
        assertEquals(activityLogForSite[0], activity)

        val updatedName = "updatedName"
        val updatedPayload = FetchedActivityLogPayload(listOf(activity.copy(name = updatedName)), site, 1, 0)

        activityLogStore.onAction(ActivityLogActionBuilder.newFetchedActivitiesAction(updatedPayload))

        val updatedActivityLogForSite = awaitActivities(site, 1)

        assertEquals(updatedActivityLogForSite.size, 1)
        assertEquals(updatedActivityLogForSite[0].activityID, activity.activityID)
        assertEquals(updatedActivityLogForSite[0].name, updatedName)
    }

    private fun authenticate(): SiteModel {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY)

        return siteStore.sites[0]
    }

    private fun activityLogModel(index: Long): ActivityLogModel {
        return ActivityLogModel(activityID = "$index",
                summary = "summary$index",
                text = "text$index",
                name = "name$index",
                type = "type",
                discarded = false,
                published = Date(index * 100),
                rewindID = null,
                gridicon = null,
                rewindable = true,
                status = "status")
    }

    private fun awaitActivities(site: SiteModel, count: Int, ascending: Boolean = true): List<ActivityLogModel> {
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertEquals(incomingChangeEvents.size, 1)
        val onFetchedEvent = incomingChangeEvents[0] as ActivityLogStore.OnActivityLogFetched
        with(onFetchedEvent) {
            assertEquals(onFetchedEvent.rowsAffected, count)
            assertNull(onFetchedEvent.error)
        }
        incomingChangeEvents.clear()
        return activityLogStore.getActivityLogForSite(site, ascending = ascending)
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
            if (mNextEvent == TestEvents.ERROR_DUPLICATE_SITE) {
                assertEquals(SiteErrorType.DUPLICATE_SITE, event.error.type)
                mCountDownLatch.countDown()
                return
            }
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertTrue(siteStore.hasSite())
        assertEquals(TestEvents.SITE_CHANGED, mNextEvent)
        mCountDownLatch.countDown()
    }

    @Subscribe
    fun onSiteRemoved(event: OnSiteRemoved) {
        AppLog.e(T.TESTS, "site count " + siteStore.sitesCount)
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent)
        mCountDownLatch.countDown()
    }

    @Subscribe
    fun onAction(action: Action<*>) {
        if (action.type is ActivityLogAction) {
            incomingActions.add(action)
            mCountDownLatch?.countDown()
        }
    }

    @Subscribe
    fun onActivityLogFetched(onChangedEvent: Store.OnChanged<ActivityLogStore.ActivityError>) {
        if (onChangedEvent is ActivityLogStore.OnActivityLogFetched) {
            incomingChangeEvents.add(onChangedEvent)
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
        mNextEvent = TestEvents.SITE_CHANGED
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
        assertTrue(siteStore.sitesCount > 0)
    }
}
