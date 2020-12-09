package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.example.BuildConfig
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel.ScanProgressStatus
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.IDLE
import org.wordpress.android.fluxc.model.scan.ScanStateModel.State.SCANNING
import org.wordpress.android.fluxc.persistence.ScanSqlUtils
import org.wordpress.android.fluxc.release.ReleaseStack_ScanTestJetpack.Sites.CompleteJetpackSite
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_ScanTestJetpack : ReleaseStack_Base() {
    private val incomingActions: MutableList<Action<*>> = mutableListOf()
    @Inject internal lateinit var scanStore: ScanStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var scanSqlUtils: ScanSqlUtils

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
    fun testFetchScanState() {
        val site = authenticate(CompleteJetpackSite)
        val payload = ScanStore.FetchScanStatePayload(site)

        mCountDownLatch = CountDownLatch(1)
        val fetchedScanStatePayload = runBlocking { scanStore.fetchScanState(payload) }
        val scanStateForSite = scanStore.getScanStateForSite(site)

        assertNotNull(fetchedScanStatePayload)
        assertNotNull(scanStateForSite)
        scanStateForSite?.apply {
            assertNotNull(state)
            Assert.assertTrue(listOf(IDLE, SCANNING).contains(state))
        }
    }

    @Test
    fun insertAndRetrieveScanIdleState() {
        val site = authenticate(CompleteJetpackSite)
        this.mCountDownLatch = CountDownLatch(1)

        val mostRecentStatus = ScanProgressStatus(
            startDate = Date(),
            duration = 40,
            progress = 30,
            error = false,
            isInitial = false
        )
        val model = ScanStateModel(
            state = IDLE,
            hasCloud = false,
            mostRecentStatus = mostRecentStatus
        )

        scanSqlUtils.replaceScanState(site, model)

        val scanState = scanStore.getScanStateForSite(site)

        assertNotNull(scanState)
        assertEquals(model.state, scanState?.state)
        assertEquals(model.hasCloud, scanState?.hasCloud)

        scanState?.mostRecentStatus?.apply {
            assertEquals(mostRecentStatus.startDate, startDate)
            assertEquals(mostRecentStatus.duration, duration)
            assertEquals(mostRecentStatus.progress, progress)
            assertEquals(mostRecentStatus.error, error)
            assertEquals(mostRecentStatus.isInitial, isInitial)
        }
    }

    @Test
    fun insertAndRetrieveScanScanningState() {
        val site = authenticate(CompleteJetpackSite)
        this.mCountDownLatch = CountDownLatch(1)

        val currentStatus = ScanProgressStatus(
            startDate = Date(),
            progress = 30,
            isInitial = false
        )
        val model = ScanStateModel(
            state = SCANNING,
            hasCloud = false,
            currentStatus = currentStatus
        )

        scanSqlUtils.replaceScanState(site, model)

        val scanState = scanStore.getScanStateForSite(site)

        assertNotNull(scanState)
        assertEquals(model.state, scanState?.state)
        assertEquals(model.hasCloud, scanState?.hasCloud)

        scanState?.currentStatus?.apply {
            assertEquals(currentStatus.startDate, startDate)
            assertEquals(currentStatus.duration, 0)
            assertEquals(currentStatus.progress, progress)
            assertEquals(currentStatus.error, false)
            assertEquals(currentStatus.isInitial, isInitial)
        }
    }

    @Test
    fun testStartScan() {
        val site = authenticate(CompleteJetpackSite)
        val payload = ScanStore.ScanStartPayload(site)

        mCountDownLatch = CountDownLatch(1)
        val scanStartResultPayload = runBlocking { scanStore.startScan(payload) }

        assertNotNull(scanStartResultPayload)
        assertNull(scanStartResultPayload.error)
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
        Assert.assertTrue(siteStore.hasSite())
        assertEquals(TestEvents.SITE_CHANGED, nextEvent)
        mCountDownLatch.countDown()
    }

    private fun authenticate(site: Sites): SiteModel {
        authenticateWPComAndFetchSites(site.wpUserName, site.wpPassword)

        return siteStore.sites.find { it.unmappedUrl == site.siteUrl }!!
    }

    @Throws(InterruptedException::class)
    private fun authenticateWPComAndFetchSites(username: String, password: String) {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        val payload = AuthenticatePayload(username, password)
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
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction())

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        Assert.assertTrue(siteStore.sitesCount > 0)
    }

    private sealed class Sites(val wpUserName: String, val wpPassword: String, val siteUrl: String) {
        object CompleteJetpackSite : Sites(
            wpUserName = BuildConfig.TEST_WPCOM_USERNAME_JETPACK,
            wpPassword = BuildConfig.TEST_WPCOM_PASSWORD_JETPACK,
            siteUrl = BuildConfig.TEST_WPORG_URL_JETPACK_COMPLETE
        )
    }
}
