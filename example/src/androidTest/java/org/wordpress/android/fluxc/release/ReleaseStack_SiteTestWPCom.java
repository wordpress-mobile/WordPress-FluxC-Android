package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnUserRolesChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.OnWPComSiteFetched;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_SiteTestWPCom extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TestEvents {
        NONE,
        SITE_CHANGED,
        POST_FORMATS_CHANGED,
        USER_ROLES_CHANGED,
        SITE_REMOVED,
        FETCHED_CONNECT_SITE_INFO,
        FETCHED_WPCOM_SITE_BY_URL,
        ERROR_INVALID_SITE,
        ERROR_UNKNOWN_SITE
    }

    private TestEvents mNextEvent;
    private int mExpectedRowsAffected;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
        mExpectedRowsAffected = 0;
    }

    public void testWPComSiteFetchAndLogout() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mExpectedRowsAffected = mSiteStore.getSitesCount();
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchPostFormats() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch post formats
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(firstSite));
        mNextEvent = TestEvents.POST_FORMATS_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Test fetched Post Formats
        List<PostFormatModel> postFormats = mSiteStore.getPostFormats(firstSite);
        assertNotSame(0, postFormats.size());
    }

    public void testFetchUserRoles() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch user roles
        mDispatcher.dispatch(SiteActionBuilder.newFetchUserRolesAction(firstSite));
        mNextEvent = TestEvents.USER_ROLES_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Test fetched user roles
        List<RoleModel> roles = mSiteStore.getUserRoles(firstSite);
        assertNotSame(0, roles.size());
    }

    public void testFetchConnectSiteInfo() throws InterruptedException {
        String site = "http://www.example.com";
        mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(site));
        mNextEvent = TestEvents.FETCHED_CONNECT_SITE_INFO;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        site = "";
        mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(site));
        mNextEvent = TestEvents.ERROR_INVALID_SITE;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchWPComSiteByUrl() throws InterruptedException {
        String site = "http://en.blog.wordpress.com";
        mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(site));
        mNextEvent = TestEvents.FETCHED_WPCOM_SITE_BY_URL;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Sites in subfolders should be handled and return a response distinct from their host
        site = "http://en.blog.wordpress.com/nonexistentsubdomain";
        mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(site));
        mNextEvent = TestEvents.ERROR_UNKNOWN_SITE;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // A Jetpack-connected site in a subfolder should have a successful response
        site = BuildConfig.TEST_WPORG_URL_JETPACK_SUBFOLDER;
        mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(site));
        mNextEvent = TestEvents.FETCHED_WPCOM_SITE_BY_URL;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        site = "http://definitelynotawpcomsite.impossible";
        mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(site));
        mNextEvent = TestEvents.ERROR_UNKNOWN_SITE;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        site = "";
        mDispatcher.dispatch(SiteActionBuilder.newFetchWpcomSiteByUrlAction(site));
        mNextEvent = TestEvents.ERROR_INVALID_SITE;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPComSiteFetchAndLogoutCollision() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Fetch sites again and immediately logout and clear WP.com sites
        mCountDownLatch = new CountDownLatch(3); // Wait for OnAuthenticationChanged, OnAccountChanged and OnSiteRemoved
        mNextEvent = TestEvents.SITE_REMOVED;
        mExpectedRowsAffected = mSiteStore.getSitesCount();
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());

        // Wait for OnAuthenticationChanged, OnAccountChanged and OnSiteRemoved from logout/site removal
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for OnSiteChanged event from fetch
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(0, mSiteStore.getSitesCount());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        AppLog.d(T.TESTS, "Received OnAuthenticationChanged event");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        AppLog.d(T.TESTS, "Received OnAccountChanged event");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        if (mSiteStore.getSitesCount() > 0) {
            assertTrue(mSiteStore.hasWPComSite());
        }
        assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(OnSiteRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mExpectedRowsAffected, event.mRowsAffected);
        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostFormatsChanged(OnPostFormatsChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.POST_FORMATS_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUserRolesChanged(OnUserRolesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.USER_ROLES_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onFetchedConnectSiteInfo(OnConnectSiteInfoChecked event) {
        if (event.isError()) {
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_SITE)) {
                assertEquals(SiteErrorType.INVALID_SITE, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occured with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_CONNECT_SITE_INFO, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onWPComSiteFetched(OnWPComSiteFetched event) {
        if (event.isError()) {
            if (mNextEvent.equals(TestEvents.ERROR_INVALID_SITE)) {
                assertEquals(SiteErrorType.INVALID_SITE, event.error.type);
                mCountDownLatch.countDown();
                return;
            } else if (mNextEvent.equals(TestEvents.ERROR_UNKNOWN_SITE)) {
                assertEquals(SiteErrorType.UNKNOWN_SITE, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_WPCOM_SITE_BY_URL, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void authenticateAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for OnSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSiteStore.getSitesCount() > 0);
    }
}
