package org.wordpress.android.fluxc.release

import com.yarolegovich.wellsql.SelectQuery
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.UNAVAILABLE
import org.wordpress.android.fluxc.persistence.ActivityLogSqlUtils
import org.wordpress.android.fluxc.release.ReleaseStack_ActivityLogTestJetpack.Sites.CompleteJetpackSite
import org.wordpress.android.fluxc.release.ReleaseStack_ActivityLogTestJetpack.Sites.FreeJetpackSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityTypesPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityTypesFetched
import org.wordpress.android.fluxc.store.ActivityLogStore.RewindErrorType
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.tools.FormattableContent
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
    @Inject lateinit var activityLogStore: ActivityLogStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var activityLogSqlUtils: ActivityLogSqlUtils

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
        this.incomingActions.clear()
    }

    @Test
    fun testFetchActivities() {
        val site = authenticate(FreeJetpackSite)

        val payload = ActivityLogStore.FetchActivityLogPayload(site)
        val fetchActivities = runBlocking { activityLogStore.fetchActivities(payload) }

        val activityLogForSite = activityLogStore.getActivityLogForSite(
                site = site,
                ascending = true,
                rewindableOnly = false
        )

        assertNotNull(fetchActivities)
        assertEquals(fetchActivities.rowsAffected, activityLogForSite.size)
    }

    @Test
    fun testFetchRewindableActivities() {
        val site = authenticate(FreeJetpackSite)

        val payload = ActivityLogStore.FetchActivityLogPayload(site)
        val fetchActivities = runBlocking { activityLogStore.fetchActivities(payload) }

        val activityLogForSite = activityLogStore.getActivityLogForSite(
                site = site,
                ascending = true,
                rewindableOnly = true
        )

        assertNotNull(fetchActivities)
        assertEquals(fetchActivities.rowsAffected, PAGE_SIZE) // All activities are persisted.
        assertEquals(activityLogForSite.size, 0) // Non retrieved, all activities are non-rewindable.
    }

    @Test
    fun testFetchActivitiesActivityTypeFilter() {
        val site = authenticate(CompleteJetpackSite)

        val rewindPayload = ActivityLogStore.FetchActivityLogPayload(site, groups = listOf("rewind"))
        val userPayload = ActivityLogStore.FetchActivityLogPayload(site, groups = listOf("user"))

        runBlocking { activityLogStore.fetchActivities(rewindPayload) }
        val rewindActivities = activityLogStore.getActivityLogForSite(site)

        runBlocking { activityLogStore.fetchActivities(userPayload) }
        val userActivities = activityLogStore.getActivityLogForSite(site)

        assertTrue(userActivities != rewindActivities)
    }

    @Test
    fun testFetchRewindState() {
        val site = authenticate(FreeJetpackSite)

        this.mCountDownLatch = CountDownLatch(1)
        val payload = ActivityLogStore.FetchRewindStatePayload(site)

        val fetchedRewindStatePayload = runBlocking { activityLogStore.fetchActivitiesRewind(payload) }

        val rewindStatusForSite = activityLogStore.getRewindStatusForSite(site)

        assertNotNull(fetchedRewindStatePayload)
        assertNotNull(rewindStatusForSite)
        rewindStatusForSite?.apply {
            assertNotNull(this.state)
            assertEquals(this.state, UNAVAILABLE)
        }
    }

    @Test
    fun storeAndRetrieveActivityLogInOrderByDateFromDb() {
        val site = authenticate(FreeJetpackSite)

        this.mCountDownLatch = CountDownLatch(1)

        val firstActivity = activityLogModel(index = 1, rewindable = true)
        val secondActivity = activityLogModel(index = 2, rewindable = true)
        val thirdActivity = activityLogModel(index = 3, rewindable = true)
        val activityModels = listOf(firstActivity, secondActivity, thirdActivity)

        activityLogSqlUtils.insertOrUpdateActivities(site, activityModels)

        val activityLogForSite = activityLogSqlUtils.getActivitiesForSite(site, SelectQuery.ORDER_DESCENDING)

        assertEquals(activityLogForSite.size, 3)
        assertEquals(activityLogForSite[0], thirdActivity)
        assertEquals(activityLogForSite[1], secondActivity)
        assertEquals(activityLogForSite[2], firstActivity)
    }

    @Test
    fun storeAndRetrieveRewindableActivityLogInOrderByDateFromDb() {
        val site = authenticate(FreeJetpackSite)

        this.mCountDownLatch = CountDownLatch(1)

        val firstActivity = activityLogModel(index = 1, rewindable = true)
        val secondActivity = activityLogModel(index = 2, rewindable = false)
        val thirdActivity = activityLogModel(index = 3, rewindable = true)
        val activityModels = listOf(firstActivity, secondActivity, thirdActivity)

        activityLogSqlUtils.insertOrUpdateActivities(site, activityModels)

        val activityLogForSite = activityLogSqlUtils.getRewindableActivitiesForSite(site, SelectQuery.ORDER_DESCENDING)

        assertEquals(activityLogForSite.size, 2)
        assertEquals(activityLogForSite[0], thirdActivity)
        assertEquals(activityLogForSite[1], firstActivity)
    }

    @Test
    fun updatesActivityWithTheSameActivityId() {
        val site = authenticate(FreeJetpackSite)

        this.mCountDownLatch = CountDownLatch(1)

        val activity = activityLogModel(index = 1, rewindable = true)

        activityLogSqlUtils.insertOrUpdateActivities(site, listOf(activity))

        val activityLogForSite = activityLogSqlUtils.getActivitiesForSite(site, SelectQuery.ORDER_DESCENDING)

        assertEquals(activityLogForSite.size, 1)
        assertEquals(activityLogForSite[0], activity)

        val updatedName = "updatedName"

        activityLogSqlUtils.insertOrUpdateActivities(site, listOf(activity.copy(name = updatedName)))

        val updatedActivityLogForSite = activityLogSqlUtils.getActivitiesForSite(site, SelectQuery.ORDER_DESCENDING)

        assertEquals(updatedActivityLogForSite.size, 1)
        assertEquals(updatedActivityLogForSite[0].activityID, activity.activityID)
        assertEquals(updatedActivityLogForSite[0].name, updatedName)
    }

    @Test
    fun rewindOperationFailsOnNonexistentId() {
        val site = authenticate(FreeJetpackSite)

        val payload = ActivityLogStore.RewindPayload(site, "123")

        val rewindResult = runBlocking { activityLogStore.rewind(payload) }

        assertTrue(rewindResult.isError)
        assertEquals(rewindResult.error.message, "Site does not have rewind active")
        assertEquals(rewindResult.error.type, RewindErrorType.GENERIC_ERROR)
    }

    @Test
    fun insertAndRetrieveRewindStatus() {
        val site = authenticate(FreeJetpackSite)
        this.mCountDownLatch = CountDownLatch(1)

        val rewindId = "rewindId"
        val restoreId: Long = 123
        val status = RUNNING
        val progress = 30
        val rewind = Rewind(rewindId, restoreId, status, progress, null)
        val model = RewindStatusModel(ACTIVE, null, Date(), null, null, rewind)

        activityLogSqlUtils.replaceRewindStatus(site, model)

        val rewindState = activityLogStore.getRewindStatusForSite(site)

        assertNotNull(rewindState)
        assertNotNull(rewindState?.rewind)
        rewindState?.rewind?.apply {
            assertEquals(rewindId, this.rewindId)
            assertEquals(restoreId, this.restoreId)
            assertEquals(status, this.status)
            assertEquals(progress, this.progress)
        }
    }

    @Test
    fun testFetchActivityTypes() {
        val site = authenticate(FreeJetpackSite)
        val payload = FetchActivityTypesPayload(site.siteId, null, null)

        val resultPayload: OnActivityTypesFetched = runBlocking { activityLogStore.fetchActivityTypes(payload) }

        assertNotNull(resultPayload)
        assertFalse(resultPayload.isError)
        assertEquals(site.siteId, resultPayload.remoteSiteId)
        assertNotNull(resultPayload.activityTypeModels)
    }

    private fun authenticate(site: Sites): SiteModel {
        authenticateWPComAndFetchSites(site.wpUserName, site.wpPassword)

        return siteStore.sites.find { it.unmappedUrl == site.siteUrl }!!
    }

    private fun activityLogModel(index: Long, rewindable: Boolean): ActivityLogModel {
        return ActivityLogModel(
                activityID = "$index",
                summary = "summary$index",
                content = FormattableContent(text = "text$index"),
                name = "name$index",
                type = "type",
                published = Date(index * 100),
                rewindID = null,
                gridicon = null,
                rewindable = rewindable,
                status = "status"
        )
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

    private sealed class Sites(val wpUserName: String, val wpPassword: String, val siteUrl: String) {
        object FreeJetpackSite : Sites(
                wpUserName = BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                wpPassword = BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY,
                siteUrl = BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY
        )

        object CompleteJetpackSite : Sites(
                wpUserName = BuildConfig.TEST_WPCOM_USERNAME_JETPACK,
                wpPassword = BuildConfig.TEST_WPCOM_PASSWORD_JETPACK,
                siteUrl = BuildConfig.TEST_WPORG_URL_JETPACK_COMPLETE
        )
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
