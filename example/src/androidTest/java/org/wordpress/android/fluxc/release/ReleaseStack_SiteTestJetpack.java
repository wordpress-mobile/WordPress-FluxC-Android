package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload;
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_SiteTestJetpack extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TestEvents {
        NONE,
        SITE_CHANGED,
        SITE_REMOVED,
        ERROR_DUPLICATE_SITE
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

    public void testWPComJetpackOnlySiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        assertEquals(1, mSiteStore.getSitesCount());
        assertEquals(0, mSiteStore.getWPComSitesCount());
        assertEquals(1, mSiteStore.getJetpackSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
        assertFalse(mSiteStore.hasSelfHostedSite());
    }

    public void testWPComSingleJetpackSiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);

        assertEquals(2, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getWPComSitesCount());
        assertEquals(1, mSiteStore.getJetpackSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testWPComMultipleJetpackSiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_MULTIPLE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_MULTIPLE_JETPACK);

        assertEquals(3, mSiteStore.getSitesCount());
        assertEquals(1, mSiteStore.getWPComSitesCount());
        assertEquals(2, mSiteStore.getJetpackSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testWPComJetpackMultisiteSiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_JETPACK_MULTISITE,
                BuildConfig.TEST_WPCOM_PASSWORD_JETPACK_MULTISITE);

        int sitesCount = mSiteStore.getSitesCount();

        // Only one non-Jetpack site exists, all the other fetched sites should be Jetpack sites
        assertEquals(1, mSiteStore.getWPComSitesCount());
        assertEquals(sitesCount - 1, mSiteStore.getJetpackSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testXMLRPCNonJetpackSiteFetch() throws InterruptedException {
        // Add a non-Jetpack self-hosted site
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        assertEquals(1, mSiteStore.getSitesCount());
        assertEquals(0, mSiteStore.getWPComSitesCount());
        assertEquals(1, mSiteStore.getSelfHostedSitesCount());
        assertEquals(0, mSiteStore.getJetpackSitesCount());

        SiteModel site = mSiteStore.getSites().get(0);

        assertFalse(site.isJetpackConnected());
        assertFalse(site.isJetpackInstalled());
        assertEquals(0, site.getSiteId());
    }

    public void testXMLRPCJetpackConnectedSiteFetch() throws InterruptedException {
        // Add a Jetpack-connected site as self-hosted
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        assertEquals(1, mSiteStore.getSitesCount());
        assertEquals(0, mSiteStore.getWPComSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());
        assertEquals(1, mSiteStore.getJetpackSitesCount());

        SiteModel site = mSiteStore.getSites().get(0);

        assertTrue(site.isJetpackConnected());
        assertNotSame(0, site.getSiteId());
    }

    public void testXMLRPCJetpackDisconnectedSiteFetch() throws InterruptedException {
        // Add a self-hosted site with Jetpack installed and active but not connected to WP.com
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_JETPACK_DISCONNECTED,
                BuildConfig.TEST_WPORG_PASSWORD_JETPACK_DISCONNECTED,
                BuildConfig.TEST_WPORG_URL_JETPACK_DISCONNECTED_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        assertEquals(1, mSiteStore.getSitesCount());
        assertEquals(0, mSiteStore.getWPComSitesCount());
        assertEquals(1, mSiteStore.getSelfHostedSitesCount());
        assertEquals(0, mSiteStore.getJetpackSitesCount());

        SiteModel site = mSiteStore.getSites().get(0);

        assertFalse(site.isJetpackConnected());
        assertTrue(site.isJetpackInstalled());
        assertEquals(0, site.getSiteId());
    }

    public void testWPComJetpackToXMLRPCDuplicateSiteFetch() throws InterruptedException {
        // Authenticate as WP.com user with a single site, which is a Jetpack site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        assertEquals(1, mSiteStore.getSitesCount());
        assertTrue(mSiteStore.hasJetpackSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasSelfHostedSite());

        // Attempt to add the same Jetpack site as a self-hosted site
        RefreshSitesXMLRPCPayload xmlrpcPayload = new RefreshSitesXMLRPCPayload();
        xmlrpcPayload.username = BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY;
        xmlrpcPayload.password = BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY;
        xmlrpcPayload.url = BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT;

        // Expect a DUPLICATE_SITE error since we're already signed into this site with Jetpack
        mNextEvent = TestEvents.ERROR_DUPLICATE_SITE;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(xmlrpcPayload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Expect no DB changes
        assertEquals(1, mSiteStore.getSitesCount());
        assertTrue(mSiteStore.hasJetpackSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasSelfHostedSite());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testXMLRPCJetpackToWPComDuplicateSiteFetch() throws InterruptedException {
        // Add a Jetpack-connected site as self-hosted
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        assertEquals(1, mSiteStore.getSitesCount());
        // We added the site from an XMLRPC call, but it's a Jetpack site accessible via the .com REST API, but
        // not considered a .com site
        assertTrue(mSiteStore.hasJetpackSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasSelfHostedSite());

        // Authenticate as WP.com user with a single site, which is the Jetpack site we already added as self-hosted
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        // We expect the XML-RPC Jetpack site to be 'upgraded' to a WPcom Jetpack site
        assertEquals(1, mSiteStore.getSitesCount());
        assertTrue(mSiteStore.hasJetpackSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasSelfHostedSite());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testWPComToXMLRPCJetpackDifferentAccountsSiteFetch() throws InterruptedException {
        // Authenticate as WP.com user with no Jetpack sites
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        int wpComSiteCount = mSiteStore.getSitesCount();

        // Add a Jetpack-connected site as self-hosted (connected to a different WP.com account than the one above)
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT);

        // Fetch site details (including Jetpack status)
        SiteModel selfHostedSite = mSiteStore.getSelfHostedSites().get(0);
        fetchSite(selfHostedSite);

        assertEquals(wpComSiteCount + 1, mSiteStore.getSitesCount());
        // The site is connected to a different wpcom account but we don't make that difference yet.
        assertEquals(wpComSiteCount, mSiteStore.getWPComSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());

        assertTrue(selfHostedSite.isJetpackConnected());
        assertFalse(selfHostedSite.isWPCom());

        signOutWPCom();

        // Expect all WP.com sites to be removed (jetpack connected sites included)
        assertEquals(0, mSiteStore.getSitesCount());
        assertEquals(0, mSiteStore.getWPComSitesCount());
        assertEquals(0, mSiteStore.getJetpackSitesCount());
        assertEquals(0, mSiteStore.getSelfHostedSitesCount());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
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
            if (mNextEvent.equals(TestEvents.ERROR_DUPLICATE_SITE)) {
                assertEquals(SiteErrorType.DUPLICATE_SITE, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertTrue(mSiteStore.hasSite());
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
        assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void signOutWPCom() throws InterruptedException {
        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSitesXMLRPC(String username, String password, String endpointUrl)
            throws InterruptedException {
        RefreshSitesXMLRPCPayload payload = new RefreshSitesXMLRPCPayload();
        payload.username = username;
        payload.password = password;
        payload.url = endpointUrl;

        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload));

        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSite(SiteModel site) throws InterruptedException {
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
