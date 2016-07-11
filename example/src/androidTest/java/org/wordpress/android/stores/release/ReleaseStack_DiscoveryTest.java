package org.wordpress.android.stores.release;

import com.squareup.otto.Subscribe;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.TestUtils;
import org.wordpress.android.stores.action.SiteAction;
import org.wordpress.android.stores.example.BuildConfig;
import org.wordpress.android.stores.generated.SiteActionBuilder;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.MemorizingTrustManager;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.stores.store.SiteStore.OnSiteChanged;
import org.wordpress.android.stores.store.SiteStore.RefreshSitesXMLRPCPayload;
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
    @Inject SelfHostedEndpointFinder mSelfHostedEndpointFinder;

    CountDownLatch mCountDownLatch;

    String mLastEndpoint;

    enum TEST_EVENTS {
        NONE,
        NOT_AUTHENTICATED,
        HTTP_AUTH_ERROR,
        INVALID_SSL_CERTIFICATE,
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
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint("notaurl&*@", payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {
                        if (error.equals(Error.INVALID_SOURCE_URL)) {
                            mCountDownLatch.countDown();
                        }
                    }

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(BuildConfig.TEST_WPORG_URL_SH_SIMPLE, payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleHTTPSFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL,
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPToHTTPSRedirectFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT,
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSelfSignedSSLFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SELFSIGNED_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SELFSIGNED_SSL;

        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(BuildConfig.TEST_WPORG_URL_SH_SELFSIGNED_SSL,
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {
                        assertEquals(Error.SSL_ERROR, error);
                        mLastEndpoint = lastEndpoint;
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        throw new AssertionError("Expected failure due to SSL error but discovery succeeded");
                    }
                });

        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Add an exception for the last certificate
        mMemorizingTrustManager.storeLastFailure();

        // Retry endpoint discovery, and attempt to fetch sites
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(mLastEndpoint,
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mMemorizingTrustManager.clearLocalTrustStore();
    }

    public void testSelfHostedHTTPAuthFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_HTTPAUTH;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_HTTPAUTH;

        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(BuildConfig.TEST_WPORG_URL_SH_HTTPAUTH,
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {
                        assertEquals(Error.HTTP_AUTH_ERROR, error);
                        mLastEndpoint = lastEndpoint;
                        mCountDownLatch.countDown();
                    }

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        throw new AssertionError("Expected failure due to HTTP AUTH error but discovery succeeded");
                    }
                });

        // Wait for a network response / onAuthenticationChanged error event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Set known HTTP Auth credentials
        mHTTPAuthManager.addHTTPAuthCredentials(
                BuildConfig.TEST_WPORG_HTTPAUTH_USERNAME_SH_HTTPAUTH,
                BuildConfig.TEST_WPORG_HTTPAUTH_PASSWORD_SH_HTTPAUTH,
                mLastEndpoint, null);

        // Retry endpoint discovery, and attempt to fetch sites
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(mLastEndpoint,
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
            @Override
            public void onError(Error error, String lastEndpoint) {}

            @Override
            public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                payload.xmlrpcEndpoint = xmlrpcEndpoint;
                mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
            }
        });

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // No protocol in URL tests

    public void testSelfHostedSimpleNoProtocolFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SIMPLE),
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleHTTPSNoProtocolFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL),
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPToHTTPSNoProtocolFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint(UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT),
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    // Bad protocol tests

    public void testSelfHostedSimpleBadProtocolFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint("hppt://" +
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_SIMPLE), payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedSimpleHTTPSBadProtocolFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint("hppt://" +
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL), payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSelfHostedHTTPToHTTPSBadProtocolRedirectFetchSites() throws InterruptedException {
        final RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = BuildConfig.TEST_WPORG_USERNAME_SH_VALID_SSL_REDIRECT;
        payload.password = BuildConfig.TEST_WPORG_PASSWORD_SH_VALID_SSL_REDIRECT;

        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mSelfHostedEndpointFinder.findEndpoint("hppt://" +
                UrlUtils.removeScheme(BuildConfig.TEST_WPORG_URL_SH_VALID_SSL_REDIRECT),
                payload.username, payload.password,
                new SelfHostedEndpointFinder.DiscoveryCallback() {
                    @Override
                    public void onError(Error error, String lastEndpoint) {}

                    @Override
                    public void onSuccess(String xmlrpcEndpoint, String restEndpoint) {
                        payload.xmlrpcEndpoint = xmlrpcEndpoint;
                        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));
                    }
                });

        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
    public void OnSitesRemoved(SiteStore.OnSitesRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(false, mSiteStore.hasSite());
        assertEquals(false, mSiteStore.hasDotOrgSite());
        assertEquals(TEST_EVENTS.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        if (event.isError) {
            AppLog.i(T.TESTS, "error " + event.authError);
            if (event.authError == AuthError.HTTP_AUTH_ERROR) {
                assertEquals(TEST_EVENTS.HTTP_AUTH_ERROR, mNextEvent);
            } else if (event.authError == AuthError.NOT_AUTHENTICATED) {
                assertEquals(TEST_EVENTS.NOT_AUTHENTICATED, mNextEvent);
            } else if (event.authError == AuthError.INVALID_SSL_CERTIFICATE) {
                assertEquals(TEST_EVENTS.INVALID_SSL_CERTIFICATE, mNextEvent);
            }
        }
        mCountDownLatch.countDown();
    }
}
