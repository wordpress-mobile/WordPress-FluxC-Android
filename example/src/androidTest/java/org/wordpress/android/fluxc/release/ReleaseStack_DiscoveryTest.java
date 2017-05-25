package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.UrlUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_DiscoveryTest extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    private enum TestEvents {
        NONE,
        DISCOVERY_SUCCEEDED_XMLRPC,
        DISCOVERY_SUCCEEDED_WPAPI,
        DISCOVERY_SUCCEEDED_XMLRPC_ONLY,
        INVALID_URL_ERROR,
        NO_SITE_ERROR,
        WORDPRESS_COM_SITE,
        ERRONEOUS_SSL_CERTIFICATE,
        HTTP_AUTH_REQUIRED,
        XMLRPC_BLOCKED,
        XMLRPC_FORBIDDEN,
        MISSING_XMLRPC_METHOD,
        SITE_CHANGED,
        SITE_REMOVED
    }

    private TestEvents mNextEvent;
    private String mUrl;
    private String mUsername;
    private String mPassword;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    public void testNoUrlFetchSites() throws InterruptedException {
        mUrl = "";
        mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TestEvents.INVALID_URL_ERROR;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testInvalidUrlFetchSites() throws InterruptedException {
        mUrl = "notaurl&*@";
        mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TestEvents.NO_SITE_ERROR;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testNonWordPressFetchSites() throws InterruptedException {
        mUrl = "example.com";
        mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TestEvents.NO_SITE_ERROR;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPComUrlFetchSites() throws InterruptedException {
        mUrl = "mysite.wordpress.com";
        mUsername = BuildConfig.TEST_WPCOM_USERNAME_TEST1;
        mPassword = BuildConfig.TEST_WPCOM_PASSWORD_TEST1;

        mNextEvent = TestEvents.WORDPRESS_COM_SITE;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testXMLRPCSimpleFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_SIMPLE,
                BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCSimpleHTTPSFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPSToHTTPSRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPSToHTTPRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSSameDomainRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCSelfSignedSSLFetchSites() throws InterruptedException {
        checkSelfHostedSelfSignedSSLFetchForSite(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPAuthFetchSites() throws InterruptedException {
        checkSelfHostedHTTPAuthFetchForSite(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSRedirectWithEndpointSameDomainFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT_ENDPOINT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    // No protocol in URL tests

    public void testXMLRPCSimpleNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SIMPLE),
                BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCSimpleHTTPSNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTP_TO_HTTP_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTP_TO_HTTPS_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPSToHTTPSNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTPS_TO_HTTPS_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPSToHTTPNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTPS_TO_HTTP_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSSameDomainNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCSelfSignedSSLNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSelfSignedSSLFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPAuthNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedHTTPAuthFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH),
                BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    // Bad protocol tests

    public void testXMLRPCSimpleBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE),
                BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCSimpleHTTPSBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTP_TO_HTTP_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTP_TO_HTTP_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTP_TO_HTTPS_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTP_TO_HTTPS_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPSToHTTPSBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTPS_TO_HTTPS_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTPS_TO_HTTPS_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPSToHTTPBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(
                addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_HTTPS_TO_HTTP_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_HTTPS_TO_HTTP_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPToHTTPSSameDomainBadProtocolRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCSelfSignedSSLBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSelfSignedSSLFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCHTTPAuthBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedHTTPAuthFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH),
                BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCRsdFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_RSD,
                BuildConfig.TEST_WPORG_USERNAME_SH_RSD,
                BuildConfig.TEST_WPORG_PASSWORD_SH_RSD,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCNoRsdFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_NO_RSD,
                BuildConfig.TEST_WPORG_USERNAME_SH_NO_RSD,
                BuildConfig.TEST_WPORG_PASSWORD_SH_NO_RSD,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testIDNEmojiFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_IDN_EMOJI,
                BuildConfig.TEST_WPORG_USERNAME_SH_IDN_EMOJI,
                BuildConfig.TEST_WPORG_PASSWORD_SH_IDN_EMOJI,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testIDNJapaneseFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_IDN_JAPANESE,
                BuildConfig.TEST_WPORG_USERNAME_SH_IDN_JAPANESE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_IDN_JAPANESE,
                TestEvents.DISCOVERY_SUCCEEDED_XMLRPC);
    }

    public void testXMLRPCBlockedDiscovery() throws InterruptedException {
        mUrl = BuildConfig.TEST_WPORG_URL_SH_BLOCKED;
        mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_BLOCKED;
        mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_BLOCKED;

        mNextEvent = TestEvents.XMLRPC_BLOCKED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testXMLRPCForbiddenDiscovery() throws InterruptedException {
        mUrl = BuildConfig.TEST_WPORG_URL_SH_FORBIDDEN;
        mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_FORBIDDEN;
        mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_FORBIDDEN;

        mNextEvent = TestEvents.XMLRPC_FORBIDDEN;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testXMLRPCMissingMethodDiscovery() throws InterruptedException {
        mUrl = BuildConfig.TEST_WPORG_URL_SH_MISSING_METHODS;
        mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_MISSING_METHODS;
        mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_MISSING_METHODS;

        mNextEvent = TestEvents.MISSING_XMLRPC_METHOD;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPAPISimpleFetchSites() throws InterruptedException {
        if (org.wordpress.android.fluxc.BuildConfig.ENABLE_WPAPI) {
            mUrl = BuildConfig.TEST_WPORG_URL_SH_WPAPI_SIMPLE;
            mUsername = BuildConfig.TEST_WPORG_USERNAME_SH_WPAPI_SIMPLE;
            mPassword = BuildConfig.TEST_WPORG_PASSWORD_SH_WPAPI_SIMPLE;

            mNextEvent = TestEvents.DISCOVERY_SUCCEEDED_WPAPI;
            mCountDownLatch = new CountDownLatch(1);

            mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

            // Wait for a network response / onChanged event
            assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // TODO: Fetch site (and migrate this test to use checkSelfHostedSimpleFetchForSite)
        }
    }

    public void testWPAPIMissingV2SimpleFetchSites() throws InterruptedException {
        // If the wp/v2 namespace is unsupported, we don't expect a WP-API endpoint to be discovered
        // (but an XML-RPC endpoint should be found)
        if (org.wordpress.android.fluxc.BuildConfig.ENABLE_WPAPI) {
            checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_WPAPI_MISSING_V2,
                    BuildConfig.TEST_WPORG_USERNAME_SH_WPAPI_MISSING_V2,
                    BuildConfig.TEST_WPORG_PASSWORD_SH_WPAPI_MISSING_V2,
                    TestEvents.DISCOVERY_SUCCEEDED_XMLRPC_ONLY);
        }
    }

    private void checkSelfHostedSimpleFetchForSite(String url, String username, String password, TestEvents nextEvent)
            throws InterruptedException {
        mUrl = url;
        mUsername = username;
        mPassword = password;

        mNextEvent = nextEvent;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        fetchSites();
    }

    private void checkSelfHostedSelfSignedSSLFetchForSite(String url, String username, String password,
                                                          TestEvents nextEvent) throws InterruptedException {
        mMemorizingTrustManager.clearLocalTrustStore();

        mUrl = url;
        mUsername = username;
        mPassword = password;

        mNextEvent = TestEvents.ERRONEOUS_SSL_CERTIFICATE;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onAuthenticationChanged error event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Add an exception for the last certificate
        mMemorizingTrustManager.storeLastFailure();

        // Retry endpoint discovery, and attempt to fetch sites
        mNextEvent = nextEvent;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onAuthenticationChanged error event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        fetchSites();

        mMemorizingTrustManager.clearLocalTrustStore();
    }

    private void checkSelfHostedHTTPAuthFetchForSite(String url, String username, String password, String authUsername,
                                                     String authPassword, TestEvents nextEvent)
            throws InterruptedException {
        mUrl = url;
        mUsername = username;
        mPassword = password;

        mNextEvent = TestEvents.HTTP_AUTH_REQUIRED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onAuthenticationChanged error event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(authUsername, authPassword, mUrl, null);

        // Retry endpoint discovery, and attempt to fetch sites
        mNextEvent = nextEvent;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mUrl));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        fetchSites();
    }

    private void fetchSites() throws InterruptedException {
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.url = mUrl;
        payload.username = mUsername;
        payload.password = mPassword;

        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private static String addBadProtocolToUrl(String url) {
        return "hppt://" + UrlUtils.removeScheme(url);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type " + event.error.type);
        }
        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasSiteAccessedViaXMLRPC());
        assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(OnSiteRemoved event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type " + event.error.type);
        }
        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        throw new AssertionError("OnAuthenticationChanged called - that's not supposed to happen for discovery");
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDiscoveryResponse(OnDiscoveryResponse event) {
        if (event.isError()) {
            // ERROR :(
            AppLog.i(T.API, "Discovery error: " + event.error);
            if (event.error == DiscoveryError.INVALID_URL) {
                assertEquals(TestEvents.INVALID_URL_ERROR, mNextEvent);
            } else if (event.error == DiscoveryError.NO_SITE_ERROR) {
                assertEquals(TestEvents.NO_SITE_ERROR, mNextEvent);
            } else if (event.error == DiscoveryError.WORDPRESS_COM_SITE) {
                assertEquals(TestEvents.WORDPRESS_COM_SITE, mNextEvent);
            } else if (event.error == DiscoveryError.HTTP_AUTH_REQUIRED) {
                assertEquals(TestEvents.HTTP_AUTH_REQUIRED, mNextEvent);
            } else if (event.error == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE) {
                assertEquals(TestEvents.ERRONEOUS_SSL_CERTIFICATE, mNextEvent);
            } else if (event.error == DiscoveryError.XMLRPC_BLOCKED) {
                assertEquals(TestEvents.XMLRPC_BLOCKED, mNextEvent);
            } else if (event.error == DiscoveryError.XMLRPC_FORBIDDEN) {
                assertEquals(TestEvents.XMLRPC_FORBIDDEN, mNextEvent);
            } else if (event.error == DiscoveryError.MISSING_XMLRPC_METHOD) {
                assertEquals(TestEvents.MISSING_XMLRPC_METHOD, mNextEvent);
            } else {
                throw new AssertionError("Didn't get the correct error, expected: " + mNextEvent + ", and got: "
                        + event.error);
            }
            mUrl = event.failedEndpoint;
            mCountDownLatch.countDown();
        } else {
            // SUCCESS :)
            AppLog.i(T.API, "Discovery succeeded, XML-RPC endpoint: " + event.xmlRpcEndpoint + ", WP-API endpoint: "
                    + event.wpRestEndpoint);
            if (mNextEvent.equals(TestEvents.DISCOVERY_SUCCEEDED_WPAPI)) {
                assertTrue(event.wpRestEndpoint != null && !event.wpRestEndpoint.isEmpty());
                mUrl = event.wpRestEndpoint;
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.DISCOVERY_SUCCEEDED_XMLRPC)) {
                assertTrue(event.xmlRpcEndpoint != null && !event.xmlRpcEndpoint.isEmpty());
                mUrl = event.xmlRpcEndpoint;
                mCountDownLatch.countDown();
            } else if (mNextEvent.equals(TestEvents.DISCOVERY_SUCCEEDED_XMLRPC_ONLY)) {
                assertTrue(event.xmlRpcEndpoint != null && !event.xmlRpcEndpoint.isEmpty());
                assertTrue(event.wpRestEndpoint == null || event.wpRestEndpoint.isEmpty());
                mUrl = event.xmlRpcEndpoint;
                mCountDownLatch.countDown();
            }
        }
    }
}
