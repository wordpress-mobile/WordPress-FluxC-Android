package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
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
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.PagedMode
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.StatsStore
import org.wordpress.android.fluxc.store.stats.insights.AllTimeInsightsStore
import org.wordpress.android.fluxc.store.stats.insights.CommentsStore
import org.wordpress.android.fluxc.store.stats.insights.FollowersStore
import org.wordpress.android.fluxc.store.stats.insights.LatestPostInsightsStore
import org.wordpress.android.fluxc.store.stats.insights.MostPopularInsightsStore
import org.wordpress.android.fluxc.store.stats.insights.PostingActivityStore
import org.wordpress.android.fluxc.store.stats.insights.PublicizeStore
import org.wordpress.android.fluxc.store.stats.insights.TagsStore
import org.wordpress.android.fluxc.store.stats.insights.TodayInsightsStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_InsightsTestJetpack : ReleaseStack_Base() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    @Inject lateinit var allTimeStore: AllTimeInsightsStore
    @Inject lateinit var commentsStore: CommentsStore
    @Inject lateinit var followersStore: FollowersStore
    @Inject lateinit var latestPostStore: LatestPostInsightsStore
    @Inject lateinit var mostPopularStore: MostPopularInsightsStore
    @Inject lateinit var publicizeStore: PublicizeStore
    @Inject lateinit var tagsStore: TagsStore
    @Inject lateinit var todayStore: TodayInsightsStore
    @Inject lateinit var postingActivityStore: PostingActivityStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var statsStore: StatsStore
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
        this.incomingActions.clear()
    }

    @Test
    fun testFetchAllTimeInsightsAndDeletesAfterwards() {
        val site = authenticate()

        val fetchedInsights = runBlocking { allTimeStore.fetchAllTimeInsights(site) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = allTimeStore.getAllTimeInsights(site)

        assertEquals(fetchedInsights.model, insightsFromDb)

        statsStore.deleteAllData()

        assertNull(allTimeStore.getAllTimeInsights(site))
    }

    @Test
    fun testFetchLatestPostInsightsAndDeletesBySiteAfter() {
        val site = authenticate()

        val fetchedInsights = runBlocking { latestPostStore.fetchLatestPostInsights(site) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = latestPostStore.getLatestPostInsights(site)

        assertEquals(fetchedInsights.model, insightsFromDb)

        statsStore.deleteSiteData(site)

        assertNull(latestPostStore.getLatestPostInsights(site))
    }

    @Test
    fun testFetchMostPopularInsights() {
        val site = authenticate()

        val fetchedInsights = runBlocking { mostPopularStore.fetchMostPopularInsights(site) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = mostPopularStore.getMostPopularInsights(site)

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testTodayInsights() {
        val site = authenticate()

        val fetchedInsights = runBlocking { todayStore.fetchTodayInsights(site) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = todayStore.getTodayInsights(site)

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testWpComFollowersInsights() {
        val site = authenticate()

        val pageSize = 5
        val fetchedInsights = runBlocking {
            followersStore.fetchWpComFollowers(site, PagedMode(pageSize, false))
        }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = followersStore.getWpComFollowers(site, LimitMode.Top(pageSize))

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testEmailFollowersInsights() {
        val site = authenticate()

        val pageSize = 5
        val fetchedInsights = runBlocking {
            followersStore.fetchEmailFollowers(site, PagedMode(pageSize, false))
        }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = followersStore.getEmailFollowers(site, LimitMode.Top(pageSize))

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testTopComments() {
        val site = authenticate()

        val pageSize = 5
        val fetchedInsights = runBlocking { commentsStore.fetchComments(site, LimitMode.Top(pageSize)) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = commentsStore.getComments(site, LimitMode.Top(pageSize))

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testTagsAndCategoriesInsights() {
        val site = authenticate()

        val pageSize = 5
        val fetchedInsights = runBlocking { tagsStore.fetchTags(site, LimitMode.Top(pageSize)) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = tagsStore.getTags(site, LimitMode.Top(pageSize))

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testPublicizeModel() {
        val site = authenticate()

        val limitMode = LimitMode.Top(5)
        val fetchedInsights = runBlocking { publicizeStore.fetchPublicizeData(site, limitMode) }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = publicizeStore.getPublicizeData(site, limitMode)

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    @Test
    fun testPostingActivity() {
        val site = authenticate()

        val startDate = Day(2019, 1, 1)
        val endDate = Day(2019, 2, 14)
        val fetchedInsights = runBlocking {
            postingActivityStore.fetchPostingActivity(
                    site,
                    startDate,
                    endDate,
                    false
            )
        }

        assertNotNull(fetchedInsights)
        assertNotNull(fetchedInsights.model)

        val insightsFromDb = postingActivityStore.getPostingActivity(site, startDate, endDate)

        assertEquals(fetchedInsights.model, insightsFromDb)
    }

    private fun authenticate(): SiteModel {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY)

        return siteStore.sites[0]
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
