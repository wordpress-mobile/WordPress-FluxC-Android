package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnProfileFetched;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock).
 * Skips self hosted site discovery, directly using the ENDPOINT URLs from tests.properties.
 */
public class ReleaseStack_SiteTestXMLRPC extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    private enum TestEvents {
        NONE,
        HTTP_AUTH_ERROR,
        INVALID_SSL_CERTIFICATE,
        SITE_CHANGED,
        POST_FORMATS_CHANGED,
        SITE_REMOVED,
        FETCHED_PROFILE
    }

    private TestEvents mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    public void testXMLRPCSimpleFetchProfile() throws InterruptedException {
        fetchProfile(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
    }

    public void testXMLRPCSimpleFetchSites() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);
    }

    public void testXMLRPCSimpleContributorFetchSites() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_CONTRIB,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_CONTRIB,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_CONTRIB_ENDPOINT);
    }

    public void testXMLRPCMultiSiteFetchSites() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_MULTISITE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_MULTISITE,
                BuildConfig.TEST_WPORG_URL_SH_MULTISITE_ENDPOINT);
    }

    public void testXMLRPCSimpleHTTPSFetchSites() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_ENDPOINT);
    }

    public void testXMLRPCSelfSignedSSLFetchSites() throws InterruptedException {
        mMemorizingTrustManager.clearLocalTrustStore();

        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL_ENDPOINT;

        // Expecting to receive an OnAuthenticationChanged event with error INVALID_SSL_CERTIFICATE, as well as an
        // OnSiteChanged event with error GENERIC_ERROR
        mNextEvent = TestEvents.INVALID_SSL_CERTIFICATE;
        mCountDownLatch = new CountDownLatch(2);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onAuthenticationChanged error event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Add an exception for the last certificate
        mMemorizingTrustManager.storeLastFailure();
        // Retry
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mMemorizingTrustManager.clearLocalTrustStore();
    }

    public void testXMLRPCHTTPAuthFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT;

        // Expecting to receive an OnAuthenticationChanged event with error HTTP_AUTH_ERROR, as well as an
        // OnSiteChanged event with error GENERIC_ERROR
        mNextEvent = TestEvents.HTTP_AUTH_ERROR;
        mCountDownLatch = new CountDownLatch(2);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onAuthenticationChanged error event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT,
                null);
        // Retry to fetch sites, this time we expect a site refresh
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testXMLRPCHTTPAuthFetchSites2() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT;
        mNextEvent = TestEvents.SITE_CHANGED;
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT,
                null);

        mCountDownLatch = new CountDownLatch(1);
        // Retry to fetch sites,we expect a site refresh
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testXMLRPCFetchAndDeleteSite() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        mNextEvent = TestEvents.SITE_REMOVED;
        mCountDownLatch = new CountDownLatch(1);
        SiteModel selfHostedSite = mSiteStore.getSitesAccessedViaXMLRPC().get(0);
        mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(selfHostedSite));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testXMLRPCFetchMultipleSites() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        // The second fetch for the same site should have replaced the first site
        Assert.assertEquals(1, mSiteStore.getSitesCount());

        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_ENDPOINT);

        // We should have two unique sites
        Assert.assertEquals(2, mSiteStore.getSitesCount());
    }

    public void testFetchPostFormats() throws InterruptedException {
        fetchSites(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch post formats
        mNextEvent = TestEvents.POST_FORMATS_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(firstSite));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));


        // Test fetched Post Formats
        List<PostFormatModel> postFormats = mSiteStore.getPostFormats(firstSite);
        Assert.assertEquals(10, postFormats.size());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "Received OnSiteChanged, site count: " + mSiteStore.getSitesCount());
        if (event.isError()) {
            AppLog.i(T.TESTS, "OnSiteChanged has error: " + event.error.type);
            if (mNextEvent.equals(TestEvents.HTTP_AUTH_ERROR)) {
                // SiteStore reports GENERIC_ERROR when it runs into authentication errors
                Assert.assertEquals(SiteErrorType.GENERIC_ERROR, event.error.type);
            } else if (mNextEvent.equals(TestEvents.INVALID_SSL_CERTIFICATE)) {
                // SiteStore reports GENERIC_ERROR when it runs into authentication errors
                Assert.assertEquals(SiteErrorType.GENERIC_ERROR, event.error.type);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            mCountDownLatch.countDown();
            return;
        }
        Assert.assertTrue(mSiteStore.hasSite());
        Assert.assertTrue(mSiteStore.hasSiteAccessedViaXMLRPC());
        Assert.assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onProfileFetched(OnProfileFetched event) {
        AppLog.i(T.TESTS, "Received OnProfileFetched, profile email: " + event.site.getEmail());
        if (event.isError()) {
            AppLog.i(T.TESTS, "OnProfileFetched has error: " + event.error.type);
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertEquals(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE, event.site.getDisplayName());
        Assert.assertEquals(TestEvents.FETCHED_PROFILE, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(OnSiteRemoved event) {
        AppLog.i(T.TESTS, "Received OnSiteRemoved, site count: " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());
        Assert.assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.i(T.TESTS, "OnAuthenticationChanged has error: " + event.error.type + " - " + event.error.message);
            if (event.error.type == AuthenticationErrorType.HTTP_AUTH_ERROR) {
                Assert.assertEquals(TestEvents.HTTP_AUTH_ERROR, mNextEvent);
            } else if (event.error.type == AuthenticationErrorType.INVALID_SSL_CERTIFICATE) {
                Assert.assertEquals(TestEvents.INVALID_SSL_CERTIFICATE, mNextEvent);
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostFormatsChanged(OnPostFormatsChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertEquals(TestEvents.POST_FORMATS_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void fetchProfile(String username, String password, String endpointUrl) throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.url = endpointUrl;

        // Expecting to receive a OnSiteChanged event
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onAuthenticationChanged error event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch the site user's Profile
        SiteModel site = mSiteStore.getSitesAccessedViaXMLRPC().get(0);
        mNextEvent = TestEvents.FETCHED_PROFILE;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchProfileXmlRpcAction(site));
        // Wait for a network response
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSites(String username, String password, String endpointUrl) throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.url = endpointUrl;

        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));

        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
