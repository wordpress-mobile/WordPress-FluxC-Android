package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
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
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.fluxc.store.stats.time.AuthorsStore
import org.wordpress.android.fluxc.store.stats.time.ClicksStore
import org.wordpress.android.fluxc.store.stats.time.CountryViewsStore
import org.wordpress.android.fluxc.store.stats.time.FileDownloadsStore
import org.wordpress.android.fluxc.store.stats.time.PostAndPageViewsStore
import org.wordpress.android.fluxc.store.stats.time.ReferrersStore
import org.wordpress.android.fluxc.store.stats.time.SearchTermsStore
import org.wordpress.android.fluxc.store.stats.time.VideoPlaysStore
import org.wordpress.android.fluxc.store.stats.time.VisitsAndViewsStore
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val ITEMS_TO_LOAD = 8
private val LIMIT_MODE = LimitMode.Top(ITEMS_TO_LOAD)
private val SELECTED_DATE = Date(10)

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
class ReleaseStack_TimeStatsTestJetpack : ReleaseStack_Base() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    @Inject lateinit var postAndPageViewsStore: PostAndPageViewsStore
    @Inject lateinit var referrersStore: ReferrersStore
    @Inject lateinit var clicksStore: ClicksStore
    @Inject lateinit var visitsAndViewsStore: VisitsAndViewsStore
    @Inject lateinit var countryViewsStore: CountryViewsStore
    @Inject lateinit var authorsStore: AuthorsStore
    @Inject lateinit var searchTermsStore: SearchTermsStore
    @Inject lateinit var videoPlaysStore: VideoPlaysStore
    @Inject lateinit var fileDownloadsStore: FileDownloadsStore
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
        this.incomingActions.clear()
    }

    @Test
    fun testFetchPostAndPageViews() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                postAndPageViewsStore.fetchPostAndPageViews(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = postAndPageViewsStore.getPostAndPageViews(
                    site,
                    granularity,
                    LIMIT_MODE,
                    SELECTED_DATE
            )

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchReferrers() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                referrersStore.fetchReferrers(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = referrersStore.getReferrers(site, granularity, LIMIT_MODE, SELECTED_DATE)
            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchClicks() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                clicksStore.fetchClicks(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = clicksStore.getClicks(site, granularity, LIMIT_MODE, SELECTED_DATE)
            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchVisitsAndViews() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                visitsAndViewsStore.fetchVisits(
                        site,
                        granularity,
                        LIMIT_MODE,
                        true
                )
            }
            if (!fetchedInsights.isError) {
                assertNotNull(fetchedInsights)
                assertNotNull(fetchedInsights.model)

                val insightsFromDb = visitsAndViewsStore.getVisits(site, granularity, LIMIT_MODE)

                assertEquals(fetchedInsights.model, insightsFromDb)
            }
        }
    }

    @Test
    fun testFetchCountryViews() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                countryViewsStore.fetchCountryViews(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = countryViewsStore.getCountryViews(site, granularity, LIMIT_MODE, SELECTED_DATE)

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchAuthors() {
        val site = authenticate()

        for (period in StatsGranularity.values()) {
            val fetchedInsights = runBlocking { authorsStore.fetchAuthors(site, period, LIMIT_MODE, SELECTED_DATE) }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = authorsStore.getAuthors(site, period, LIMIT_MODE, SELECTED_DATE)

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchSearchTerms() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                searchTermsStore.fetchSearchTerms(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = searchTermsStore.getSearchTerms(site, granularity, LIMIT_MODE, SELECTED_DATE)

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchVideoPlays() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                videoPlaysStore.fetchVideoPlays(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = videoPlaysStore.getVideoPlays(site, granularity, LIMIT_MODE, SELECTED_DATE)

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    @Test
    fun testFetchFileDownloads() {
        val site = authenticate()

        for (granularity in StatsGranularity.values()) {
            val fetchedInsights = runBlocking {
                fileDownloadsStore.fetchFileDownloads(
                        site,
                        granularity,
                        LIMIT_MODE,
                        SELECTED_DATE,
                        true
                )
            }

            assertNotNull(fetchedInsights)
            assertNotNull(fetchedInsights.model)

            val insightsFromDb = fileDownloadsStore.getFileDownloads(site, granularity, LIMIT_MODE, SELECTED_DATE)

            assertEquals(fetchedInsights.model, insightsFromDb)
        }
    }

    private fun authenticate(): SiteModel {
        authenticateWPComAndFetchSites(
                BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY
        )

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
