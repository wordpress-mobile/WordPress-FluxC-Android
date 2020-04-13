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
import org.wordpress.android.fluxc.store.SiteStore.AccessCookieErrorType;
import org.wordpress.android.fluxc.store.SiteStore.AutomatedTransferErrorType;
import org.wordpress.android.fluxc.store.SiteStore.DomainAvailabilityStatus;
import org.wordpress.android.fluxc.store.SiteStore.DomainMappabilityStatus;
import org.wordpress.android.fluxc.store.SiteStore.FetchPrivateAtomicCookiePayload;
import org.wordpress.android.fluxc.store.SiteStore.OnPrivateAtomicCookieFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferInitiated;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferStatusChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnDomainAvailabilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnDomainSupportedCountriesFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnDomainSupportedStatesFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnPlansFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteEditorsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.OnSuggestedDomains;
import org.wordpress.android.fluxc.store.SiteStore.OnUserRolesChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnWPComSiteFetched;
import org.wordpress.android.fluxc.store.SiteStore.PlansErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.fluxc.store.SiteStore.SuggestDomainErrorType;
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
        SITE_EDITORS_CHANGED,
        PLANS_FETCHED,
        PLANS_UNKNOWN_BLOG_ERROR,
        SITE_REMOVED,
        FETCHED_CONNECT_SITE_INFO,
        FETCHED_WPCOM_SITE_BY_URL,
        FETCHED_DOMAIN_SUGGESTIONS,
        FETCHED_TLDS_FILTERED_DOMAINS,
        DOMAIN_SUGGESTION_ERROR_INVALID_QUERY,
        ERROR_INVALID_SITE,
        ERROR_INVALID_SITE_TYPE,
        ERROR_UNKNOWN_SITE,
        INELIGIBLE_FOR_AUTOMATED_TRANSFER,
        INITIATE_INELIGIBLE_AUTOMATED_TRANSFER,
        AUTOMATED_TRANSFER_NOT_FOUND,
        CHECK_BLACKLISTED_DOMAIN_AVAILABILITY,
        FETCHED_DOMAIN_SUPPORTED_STATES,
        FETCHED_DOMAIN_SUPPORTED_COUNTRIES
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
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_HAS_POST_FORMATS,
                BuildConfig.TEST_WPCOM_PASSWORD_HAS_POST_FORMATS);

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
    public void testFetchSiteEditors() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch user roles
        mDispatcher.dispatch(SiteActionBuilder.newFetchSiteEditorsAction(firstSite));
        mNextEvent = TestEvents.SITE_EDITORS_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        String siteEditor = firstSite.getMobileEditor();
        // Test mobile editors for a wpcom site
        assertTrue(siteEditor.equals("")
                || siteEditor.equals("aztec") || siteEditor.equals("gutenberg"));
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

        // Fetch site plans
        mDispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(firstSite));
        mNextEvent = TestEvents.PLANS_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testUnknownBlogErrorOnFetchPlans() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        // Initialize a WP.com site with an invalid siteId.
        // siteModel.isWPCom is set to true, to avoid PlansErrorType.NOT_AVAILABLE error.
        SiteModel siteModel = new SiteModel();
        siteModel.setIsWPCom(true);
        siteModel.setSiteId(0);

        // Try to fetch plans for that invalid site.
        mDispatcher.dispatch(SiteActionBuilder.newFetchPlansAction(siteModel));
        mNextEvent = TestEvents.PLANS_UNKNOWN_BLOG_ERROR;
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
    public void testWpcomSubdomainSuggestions() throws InterruptedException {
        String keywords = "awesomesubdomain";
        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, false, 20, false);
        testSuggestDomains(payload, TestEvents.FETCHED_DOMAIN_SUGGESTIONS);
    }

    @Test
    public void testWpcomSubdomainDotBlogSuggestions() throws InterruptedException {
        String keywords = "awesomesubdomain";
        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, true, 20, true);
        testSuggestDomains(payload, TestEvents.FETCHED_DOMAIN_SUGGESTIONS);
    }

    @Test
    public void testTldsFilteredSuggestions() throws InterruptedException {
        String keyword = "awesomedomain";

        SuggestDomainsPayload payload = new SuggestDomainsPayload(keyword, 20, "blog");
        testSuggestDomains(payload, TestEvents.FETCHED_TLDS_FILTERED_DOMAINS);
    }

    @Test
    public void testEmptyDomainSuggestions() throws InterruptedException {
        // This query should return 0 results which returns a 400 error from the API. This test verifies that
        // we are converting it to a successful response.
        String keywords = "test";
        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, false, 20, false);
        testSuggestDomains(payload, TestEvents.FETCHED_DOMAIN_SUGGESTIONS);
    }

    @Test
    public void testInvalidQueryDomainSuggestions() throws InterruptedException {
        // This query should return
        String keywords = ".";
        SuggestDomainsPayload payload = new SuggestDomainsPayload(keywords, true, true, false, 20, false);
        testSuggestDomains(payload, TestEvents.DOMAIN_SUGGESTION_ERROR_INVALID_QUERY);
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

    // TODO Disabled due to server-side issues - can be restored once D23247 is merged and tested
//    @Test
//    public void testInitiateIneligibleAutomatedTransfer() throws InterruptedException {
//        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
//                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);
//        SiteModel firstSite = mSiteStore.getWPComSites().get(0);
//        mNextEvent = TestEvents.INITIATE_INELIGIBLE_AUTOMATED_TRANSFER;
//        mDispatcher.dispatch(SiteActionBuilder
//                .newInitiateAutomatedTransferAction(new InitiateAutomatedTransferPayload(firstSite, "react")));
//        mCountDownLatch = new CountDownLatch(1);
//        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void testCheckAutomatedTransferStatusNotFound() throws InterruptedException {
//        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
//                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);
//        SiteModel firstSite = mSiteStore.getWPComSites().get(0);
//        mNextEvent = TestEvents.AUTOMATED_TRANSFER_NOT_FOUND;
//        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(firstSite));
//        mCountDownLatch = new CountDownLatch(1);
//        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
//    }

    @Test
    public void testCheckDomainAvailability() throws InterruptedException {
        authenticateUser(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        // Check availability for 'Wordpress.com'.
        mDispatcher.dispatch(SiteActionBuilder.newCheckDomainAvailabilityAction("Wordpress.com"));
        mNextEvent = TestEvents.CHECK_BLACKLISTED_DOMAIN_AVAILABILITY;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchSupportedStates() throws InterruptedException {
        authenticateUser(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        // Fetch Supported states
        mDispatcher.dispatch(SiteActionBuilder.newFetchDomainSupportedStatesAction("US"));
        mNextEvent = TestEvents.FETCHED_DOMAIN_SUPPORTED_STATES;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchSupportedCountries() throws InterruptedException {
        authenticateUser(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        // Fetch supported countries
        mDispatcher.dispatch(SiteActionBuilder.newFetchDomainSupportedCountriesAction());
        mNextEvent = TestEvents.FETCHED_DOMAIN_SUPPORTED_COUNTRIES;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchingPrivateAtomicCookieForNonExistentSite() throws InterruptedException {
        authenticateUser(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        mDispatcher.dispatch(SiteActionBuilder.newFetchPrivateAtomicCookieAction(
                new FetchPrivateAtomicCookiePayload(-1)));
        mNextEvent = TestEvents.ERROR_INVALID_SITE;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchingPrivateAtomicCookieForNonPrivateAtSite() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        SiteModel nonAtomicSite = mSiteStore.getSites().get(0);

        mDispatcher.dispatch(SiteActionBuilder.newFetchPrivateAtomicCookieAction(
                new FetchPrivateAtomicCookiePayload(nonAtomicSite.getSiteId())));
        mNextEvent = TestEvents.ERROR_INVALID_SITE_TYPE;
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
    public void onSiteEditorsChanged(OnSiteEditorsChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.SITE_EDITORS_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPlansFetched(OnPlansFetched event) {
        if (event.isError()) {
            AppLog.i(T.API, "onPlansFetched has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.PLANS_UNKNOWN_BLOG_ERROR)) {
                assertEquals(PlansErrorType.UNKNOWN_BLOG, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.PLANS_FETCHED, mNextEvent);
        assertNotNull(event.plans);
        assertFalse(event.plans.isEmpty());
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
            if (mNextEvent == TestEvents.DOMAIN_SUGGESTION_ERROR_INVALID_QUERY) {
                assertEquals(event.error.type, SuggestDomainErrorType.INVALID_QUERY);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }

        final String wpcomSuffix = ".wordpress.com";
        final String dotBlogSuffix = ".blog";
        final String dotNetSuffix = ".net";

        if (mNextEvent == TestEvents.FETCHED_DOMAIN_SUGGESTIONS) {
            for (DomainSuggestionResponse suggestionResponse : event.suggestions) {
                String domain = suggestionResponse.domain_name;
                assertTrue("Was expecting the domain to end in " + wpcomSuffix + " or " + dotBlogSuffix,
                        domain.endsWith(wpcomSuffix) || domain.endsWith(dotBlogSuffix));
            }
        } else if (mNextEvent == TestEvents.FETCHED_TLDS_FILTERED_DOMAINS) {
            for (DomainSuggestionResponse suggestionResponse : event.suggestions) {
                String domain = suggestionResponse.domain_name;
                assertTrue("Was expecting the domain to end in " + dotNetSuffix, domain.endsWith(dotBlogSuffix));
            }
        } else {
            throw new AssertionError("Unexpected event type: " + mNextEvent);
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onDomainAvailabilityChecked(OnDomainAvailabilityChecked event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.CHECK_BLACKLISTED_DOMAIN_AVAILABILITY, mNextEvent);
        assertEquals(event.status, DomainAvailabilityStatus.BLACKLISTED_DOMAIN);
        assertEquals(event.mappable, DomainMappabilityStatus.BLACKLISTED_DOMAIN);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDomainSupportedStatesFetched(OnDomainSupportedStatesFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_DOMAIN_SUPPORTED_STATES, mNextEvent);
        assertNotNull(event.supportedStates);
        assertFalse(event.supportedStates.isEmpty());
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDomainSupportedCountriesFetched(OnDomainSupportedCountriesFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCHED_DOMAIN_SUPPORTED_COUNTRIES, mNextEvent);
        assertNotNull(event.supportedCountries);
        assertFalse(event.supportedCountries.isEmpty());
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccessCookieFetched(OnPrivateAtomicCookieFetched event) {
        if (event.isError()) {
            if (mNextEvent == TestEvents.ERROR_INVALID_SITE) {
                assertEquals(event.error.type, AccessCookieErrorType.SITE_MISSING_FROM_STORE);
            } else if (mNextEvent == TestEvents.ERROR_INVALID_SITE_TYPE) {
                assertEquals(event.error.type, AccessCookieErrorType.NON_PRIVATE_AT_SITE);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        }
        mCountDownLatch.countDown();
    }

    private void authenticateAndFetchSites(String username, String password) throws InterruptedException {
        authenticateUser(username, password);

        // Fetch sites from REST API, and wait for OnSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSiteStore.getSitesCount() > 0);
    }

    private void authenticateUser(String username, String password) throws InterruptedException {
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
    }

    private void testSuggestDomains(SuggestDomainsPayload payload, TestEvents nextEvent) throws InterruptedException {
        mDispatcher.dispatch(SiteActionBuilder.newSuggestDomainsAction(payload));
        mNextEvent = nextEvent;
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
