package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
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
public class ReleaseStack_SiteTest extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    CountDownLatch mCountDownLatch;

    enum TEST_EVENTS {
        NONE,
        HTTP_AUTH_ERROR,
        INVALID_SSL_CERTIFICATE,
        SITE_CHANGED,
        POST_FORMATS_CHANGED,
        SITE_REMOVED
    }
    private TEST_EVENTS mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mNextEvent = TEST_EVENTS.NONE;
    }

    public void testSelfHostedSimpleFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleContributorFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE_CONTRIB;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE_CONTRIB;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_CONTRIB_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedMultiSiteFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_MULTISITE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_MULTISITE;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_MULTISITE_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleHTTPSFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSelfSignedSSLFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL_ENDPOINT;

        //  We're expecting a SSL Warning event
        mNextEvent = TEST_EVENTS.INVALID_SSL_CERTIFICATE;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Add an exception for the last certificate
        mMemorizingTrustManager.storeLastFailure();
        // Retry
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mMemorizingTrustManager.clearLocalTrustStore();
    }

    public void testSelfHostedHTTPAuthFetchSites() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT;

        // We're expecting a HTTP_AUTH_ERROR
        mNextEvent = TEST_EVENTS.HTTP_AUTH_ERROR;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT,
                null);
        // Retry to fetch sites, this time we expect a site refresh
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPAuthFetchSites2() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH, BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH_ENDPOINT,
                null);

        mCountDownLatch = new CountDownLatch(1);
        // Retry to fetch sites,we expect a site refresh
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        // Wait for a network response
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedFetchAndDeleteSite() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TEST_EVENTS.SITE_REMOVED;
        mCountDownLatch = new CountDownLatch(1);
        SiteModel dotOrgSite = mSiteStore.getDotOrgSites().get(0);
        mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(dotOrgSite));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchPostFormats() throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;
        payload.url = BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT;
        mNextEvent = TEST_EVENTS.SITE_CHANGED;

        // Fetch sites
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch post formats
        mNextEvent = TEST_EVENTS.POST_FORMATS_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(firstSite));
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));


        // Test fetched Post Formats
        List<PostFormatModel> postFormats = mSiteStore.getPostFormats(firstSite);
        assertEquals(10, postFormats.size());
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            return;
        }
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void OnSiteRemoved(SiteStore.OnSiteRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(false, mSiteStore.hasSite());
        assertEquals(false, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError()) {
            AppLog.i(T.TESTS, "error " + event.error.type + " - " + event.error.message);
            if (event.error.type == AuthenticationErrorType.GENERIC_ERROR) {
                assertEquals(TEST_EVENTS.HTTP_AUTH_ERROR, mNextEvent);
            }
            if (event.error.type == AuthenticationErrorType.INVALID_SSL_CERTIFICATE) {
                assertEquals(TEST_EVENTS.INVALID_SSL_CERTIFICATE, mNextEvent);
            }
        }
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostFormatsChanged(OnPostFormatsChanged event) {
        assertEquals(TEST_EVENTS.POST_FORMATS_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
