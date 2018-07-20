package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferErrorType;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferInitiated;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferStatusChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnPlanFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains;
import org.wordpress.android.fluxc.store.SiteStore.OnUserRolesChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnWPComSiteFetched;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainsPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

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
        PLANS_FETCHED,
        SITE_REMOVED,
        FETCHED_CONNECT_SITE_INFO,
        FETCHED_WPCOM_SITE_BY_URL,
        FETCHED_WPCOM_SUBDOMAIN_SUGGESTIONS,
        ERROR_INVALID_SITE,
        ERROR_UNKNOWN_SITE,
        INELIGIBLE_FOR_AUTOMATED_TRANSFER,
        INITIATE_INELIGIBLE_AUTOMATED_TRANSFER,
        AUTOMATED_TRANSFER_NOT_FOUND
    }

    private TestEvents mNextEvent;
    private int mExpectedRowsAffected;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
        mExpectedRowsAffected = 0;
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testFetchPlans() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch user roles
        mDispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(firstSite));
        mNextEvent = TestEvents.PLANS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }


    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testWpcomSubdomainSuggestions() throws InterruptedException {
        String keywords = "awesomesubdomain";
        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, false, 20);
        mDispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload));
        mNextEvent = TestEvents.FETCHED_WPCOM_SUBDOMAIN_SUGGESTIONS;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSiteQuotaAllowed() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);
        // Default quota for a wpcom site is 3Gb
        assertEquals(firstSite.getSpaceAllowed(), 3L * 1024 * 1024 * 1024);
    }

    @Test
    public void testIneligibleForAutomatedTransfer() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);
        SiteModel firstSite = mSiteStore.getSites().get(0);
        mNextEvent = TestEvents.INELIGIBLE_FOR_AUTOMATED_TRANSFER;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferEligibilityAction(firstSite));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testInitiateIneligibleAutomatedTransfer() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);
        SiteModel firstSite = mSiteStore.getSites().get(0);
        mNextEvent = TestEvents.INITIATE_INELIGIBLE_AUTOMATED_TRANSFER;
        mDispatcher.dispatch(SiteActionBuilder
                .newInitiateAutomatedTransferAction(new InitiateAutomatedTransferPayload(firstSite, "react")));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testCheckAutomatedTransferStatusNotFound() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);
        SiteModel firstSite = mSiteStore.getSites().get(0);
        mNextEvent = TestEvents.AUTOMATED_TRANSFER_NOT_FOUND;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(firstSite));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
    public void onPlanFetched(OnPlanFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.PLANS_FETCHED, mNextEvent);
        assertNotNull(event.plans);
        assertNotSame(0, event.plans.size());
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onSuggestedDomains(OnSuggestedDomains event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_WPCOM_SUBDOMAIN_SUGGESTIONS, mNextEvent);

        final String suffix = ".wordpress.com";
        for (DomainSuggestionResponse suggestionResponse : event.suggestions) {
            assertTrue("Was expecting the domain to end in " + suffix, suggestionResponse.domain_name.endsWith(suffix));
        }

        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferEligibilityChecked(OnAutomatedTransferEligibilityChecked event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.INELIGIBLE_FOR_AUTOMATED_TRANSFER);
        assertNotNull(event.site);
        assertFalse(event.isEligible);
        assertFalse(event.eligibilityErrorCodes.isEmpty());
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferInitiated(OnAutomatedTransferInitiated event) {
        assertEquals(mNextEvent, TestEvents.INITIATE_INELIGIBLE_AUTOMATED_TRANSFER);
        assertTrue(event.isError());
        assertEquals(event.error.type, AutomatedTransferErrorType.AT_NOT_ELIGIBLE);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferStatusChecked(OnAutomatedTransferStatusChecked event) {
        assertEquals(mNextEvent, TestEvents.AUTOMATED_TRANSFER_NOT_FOUND);
        assertTrue(event.isError());
        assertEquals(event.error.type, AutomatedTransferErrorType.NOT_FOUND);
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
