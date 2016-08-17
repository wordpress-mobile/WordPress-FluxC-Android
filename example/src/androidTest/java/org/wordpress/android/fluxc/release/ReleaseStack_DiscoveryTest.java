package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
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
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject HTTPAuthManager mHTTPAuthManager;
    @Inject MemorizingTrustManager mMemorizingTrustManager;

    RefreshSitesXMLRPCPayload mPayload;
    CountDownLatch mCountDownLatch;

    enum TEST_EVENTS {
        NONE,
        DISCOVERY_SUCCEEDED,
        NO_SITE_ERROR,
        WORDPRESS_COM_SITE,
        ERRONEOUS_SSL_CERTIFICATE,
        HTTP_AUTH_REQUIRED,
        SITE_CHANGED,
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

    public void testInvalidUrlFetchSites() throws InterruptedException {
        mPayload = new RefreshSitesXMLRPCPayload();
        mPayload.url = "notaurl&*@";
        mPayload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        mPayload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TEST_EVENTS.NO_SITE_ERROR;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testNonWordPressFetchSites() throws InterruptedException {
        mPayload = new RefreshSitesXMLRPCPayload();
        mPayload.url = "example.com";
        mPayload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        mPayload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TEST_EVENTS.NO_SITE_ERROR;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWordPressCOMUrlFetchSites() throws InterruptedException {
        mPayload = new RefreshSitesXMLRPCPayload();
        mPayload.url = "mysite.wordpress.com";
        mPayload.username = BuildConfig.TEST_WPCOM_USERNAME_TEST1;
        mPayload.password = BuildConfig.TEST_WPCOM_PASSWORD_TEST1;

        mNextEvent = TEST_EVENTS.WORDPRESS_COM_SITE;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_SIMPLE,
                BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
    }

    public void testSelfHostedSimpleHTTPSFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL);
    }

    public void testSelfHostedHTTPToHTTPSRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT);
    }

    public void testSelfHostedSelfSignedSSLFetchSites() throws InterruptedException {
        checkSelfHostedSelfSignedSSLFetchForSite(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL);
    }

    public void testSelfHostedHTTPAuthFetchSites() throws InterruptedException {
        checkSelfHostedHTTPAuthFetchForSite(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH);
    }

    public void testSelfHostedHTTPToHTTPSRedirectWithEndpointFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT_ENDPOINT,
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT);
    }

    // No protocol in URL tests

    public void testSelfHostedSimpleNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SIMPLE),
                BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
    }

    public void testSelfHostedSimpleHTTPSNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL);
    }

    public void testSelfHostedHTTPToHTTPSNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT);
    }

    public void testSelfHostedSelfSignedSSLNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSelfSignedSSLFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL);
    }

    public void testSelfHostedHTTPAuthNoProtocolFetchSites() throws InterruptedException {
        checkSelfHostedHTTPAuthFetchForSite(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH),
                BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH);
    }

    // Bad protocol tests

    public void testSelfHostedSimpleBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_SIMPLE),
                BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE);
    }

    public void testSelfHostedSimpleHTTPSBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL);
    }

    public void testSelfHostedHTTPToHTTPSBadProtocolRedirectFetchSites() throws InterruptedException {
        checkSelfHostedSimpleFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT),
                BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT,
                BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT);
    }

    public void testSelfHostedSelfSignedSSLBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedSelfSignedSSLFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL),
                BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL);
    }

    public void testSelfHostedHTTPAuthBadProtocolFetchSites() throws InterruptedException {
        checkSelfHostedHTTPAuthFetchForSite(addBadProtocolToUrl(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH),
                BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH);
    }

    private void checkSelfHostedSimpleFetchForSite(String url, String username, String password)
            throws InterruptedException {
        mPayload = new RefreshSitesXMLRPCPayload();
        mPayload.url = url;
        mPayload.username = username;
        mPayload.password = password;

        mNextEvent = TEST_EVENTS.DISCOVERY_SUCCEEDED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        fetchSites();
    }

    private void checkSelfHostedSelfSignedSSLFetchForSite(String url, String username, String password)
            throws InterruptedException {
        mPayload = new RefreshSitesXMLRPCPayload();
        mPayload.url = url;
        mPayload.username = username;
        mPayload.password = password;

        mNextEvent = TEST_EVENTS.ERRONEOUS_SSL_CERTIFICATE;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Add an exception for the last certificate
        mMemorizingTrustManager.storeLastFailure();

        // Retry endpoint discovery, and attempt to fetch sites
        mNextEvent = TEST_EVENTS.DISCOVERY_SUCCEEDED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        fetchSites();

        mMemorizingTrustManager.clearLocalTrustStore();
    }

    private void checkSelfHostedHTTPAuthFetchForSite(String url, String username, String password, String auth_username,
                                                    String auth_password) throws InterruptedException {
        mPayload = new RefreshSitesXMLRPCPayload();
        mPayload.url = url;
        mPayload.username = username;
        mPayload.password = password;

        mNextEvent = TEST_EVENTS.HTTP_AUTH_REQUIRED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(auth_username, auth_password, mPayload.url, null);

        // Retry endpoint discovery, and attempt to fetch sites
        mNextEvent = TEST_EVENTS.DISCOVERY_SUCCEEDED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(mPayload));

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        fetchSites();
    }

    private void fetchSites() throws InterruptedException {
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(mPayload));

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private static String addBadProtocolToUrl(String url) {
        return "hppt://" + UrlUtils.removeScheme(url);
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void OnSiteRemoved(SiteStore.OnSiteRemoved event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(false, mSiteStore.hasSite());
        assertEquals(false, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        throw new AssertionError("OnAuthenticationChanged called - that's not supposed to happen for discovery");
    }

    @Subscribe
    public void onDiscoveryResponse(AccountStore.OnDiscoveryResponse event) {
        if (event.isError()) {
            // ERROR :(
            AppLog.i(T.API, "Discovery error: " + event.error);
            if (event.error == DiscoveryError.NO_SITE_ERROR) {
                assertEquals(TEST_EVENTS.NO_SITE_ERROR, mNextEvent);
            } else if (event.error == DiscoveryError.WORDPRESS_COM_SITE) {
                assertEquals(TEST_EVENTS.WORDPRESS_COM_SITE, mNextEvent);
            } else if (event.error == DiscoveryError.HTTP_AUTH_REQUIRED) {
                assertEquals(TEST_EVENTS.HTTP_AUTH_REQUIRED, mNextEvent);
            } else if (event.error == DiscoveryError.ERRONEOUS_SSL_CERTIFICATE) {
                assertEquals(TEST_EVENTS.ERRONEOUS_SSL_CERTIFICATE, mNextEvent);
            } else {
                throw new AssertionError("Didn't get the correct error, expected: " + mNextEvent + ", and got: "
                        + event.error);
            }
            mPayload.url = event.failedEndpoint;
        } else {
            // SUCCESS :)
            AppLog.i(T.API, "Discovery succeeded, endpoint: " + event.xmlRpcEndpoint);
            assertEquals(TEST_EVENTS.DISCOVERY_SUCCEEDED, mNextEvent);
            mPayload.url = event.xmlRpcEndpoint;
            mCountDownLatch.countDown();
        }
        mCountDownLatch.countDown();
    }
}
