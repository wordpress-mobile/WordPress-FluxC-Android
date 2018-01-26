package org.wordpress.android.fluxc.release;

import android.text.TextUtils;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
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

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaWPComRestCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaXMLRPCCount());

        SiteModel site = mSiteStore.getSites().get(0);
        Assert.assertFalse(TextUtils.isEmpty(site.getJetpackVersion()));

        signOutWPCom();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
        Assert.assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());
    }

    public void testWPComSingleJetpackSiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);

        Assert.assertEquals(2, mSiteStore.getSitesCount());
        Assert.assertEquals(1, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(2, mSiteStore.getSitesAccessedViaWPComRestCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaXMLRPCCount());

        signOutWPCom();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
    }

    public void testWPComMultipleJetpackSiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_MULTIPLE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_MULTIPLE_JETPACK);

        Assert.assertEquals(3, mSiteStore.getSitesCount());
        Assert.assertEquals(1, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(3, mSiteStore.getSitesAccessedViaWPComRestCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaXMLRPCCount());

        signOutWPCom();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
    }

    public void testWPComJetpackMultisiteSiteFetch() throws InterruptedException {
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_JETPACK_MULTISITE,
                BuildConfig.TEST_WPCOM_PASSWORD_JETPACK_MULTISITE);

        int sitesCount = mSiteStore.getSitesCount();

        // Only one non-Jetpack site exists, all the other fetched sites should be Jetpack sites
        Assert.assertEquals(1, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(sitesCount, mSiteStore.getSitesAccessedViaWPComRestCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaXMLRPCCount());

        signOutWPCom();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
    }

    public void testXMLRPCNonJetpackSiteFetch() throws InterruptedException {
        // Add a non-Jetpack self-hosted site
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SH_SIMPLE,
                BuildConfig.TEST_WPORG_PASSWORD_SH_SIMPLE,
                BuildConfig.TEST_WPORG_URL_SH_SIMPLE_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaWPComRestCount());

        SiteModel site = mSiteStore.getSites().get(0);

        Assert.assertFalse(site.isJetpackConnected());
        Assert.assertFalse(site.isJetpackInstalled());
        Assert.assertEquals(0, site.getSiteId());
    }

    public void testXMLRPCJetpackConnectedSiteFetch() throws InterruptedException {
        // Add a Jetpack-connected site as self-hosted
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaWPComRestCount());

        SiteModel site = mSiteStore.getSites().get(0);

        Assert.assertTrue(site.isJetpackConnected());
        Assert.assertNotSame(0, site.getSiteId());
    }

    public void testXMLRPCJetpackDisconnectedSiteFetch() throws InterruptedException {
        // Add a self-hosted site with Jetpack installed and active but not connected to WP.com
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_JETPACK_DISCONNECTED,
                BuildConfig.TEST_WPORG_PASSWORD_JETPACK_DISCONNECTED,
                BuildConfig.TEST_WPORG_URL_JETPACK_DISCONNECTED_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaWPComRestCount());

        SiteModel site = mSiteStore.getSites().get(0);

        Assert.assertFalse(site.isJetpackConnected());
        Assert.assertTrue(site.isJetpackInstalled());
        Assert.assertEquals(0, site.getSiteId());
    }

    public void testWPComJetpackToXMLRPCDuplicateSiteFetch() throws InterruptedException {
        // Authenticate as WP.com user with a single site, which is a Jetpack site
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertTrue(mSiteStore.hasSitesAccessedViaWPComRest());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());

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
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Expect no DB changes
        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertTrue(mSiteStore.hasSitesAccessedViaWPComRest());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());

        signOutWPCom();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
    }

    public void testXMLRPCJetpackToWPComDuplicateSiteFetch() throws InterruptedException {
        // Add a Jetpack-connected site as self-hosted
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT);

        // Fetch site details (including Jetpack status)
        fetchSite(mSiteStore.getSites().get(0));

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        // We added the site from an XMLRPC call, but it's a Jetpack site accessible via the .com REST API but accessed
        // via XMLRPC (ie. not pull from the REST call).
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
        // not considered a .com site
        Assert.assertFalse(mSiteStore.hasWPComSite());
        // accessed via XMLRPC, that's why we consider it a "self hosted site"
        Assert.assertTrue(mSiteStore.hasSiteAccessedViaXMLRPC());

        // Authenticate as WP.com user with a single site, which is the Jetpack site we already added as self-hosted
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        // We expect the XML-RPC Jetpack site to be 'upgraded' to a WPcom Jetpack site
        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertTrue(mSiteStore.hasSitesAccessedViaWPComRest());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());

        signOutWPCom();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertFalse(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSitesAccessedViaWPComRest());
    }

    public void testWPComToXMLRPCJetpackDifferentAccountsSiteFetch() throws InterruptedException {
        // Authenticate as WP.com user
        authenticateWPComAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        int wpComSiteCount = mSiteStore.getWPComSitesCount();
        int totalSiteCount = mSiteStore.getSitesCount();
        int accessedViaWPComRestSiteCount = mSiteStore.getSitesAccessedViaWPComRestCount();
        int selfhostedSiteCount = mSiteStore.getSitesAccessedViaXMLRPCCount();

        Assert.assertEquals(0, selfhostedSiteCount);
        Assert.assertEquals(totalSiteCount, accessedViaWPComRestSiteCount);

        // Add a Jetpack-connected site as self-hosted (connected to a different WP.com account than the one above)
        fetchSitesXMLRPC(BuildConfig.TEST_WPORG_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_PASSWORD_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPORG_URL_SINGLE_JETPACK_ONLY_ENDPOINT);

        // Fetch site details (including Jetpack status)
        SiteModel selfHostedSite = mSiteStore.getSitesAccessedViaXMLRPC().get(0);
        fetchSite(selfHostedSite);

        Assert.assertEquals(totalSiteCount + 1, mSiteStore.getSitesCount());
        // The site is accessible via WPCom (Jetpack connected) but accessed via XMLRPC, so previous
        // accessedViaWPComRestSiteCount must be the same
        Assert.assertEquals(accessedViaWPComRestSiteCount, mSiteStore.getSitesAccessedViaWPComRestCount());
        Assert.assertEquals(selfhostedSiteCount + 1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(wpComSiteCount, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());

        Assert.assertTrue(selfHostedSite.isJetpackConnected());
        Assert.assertFalse(selfHostedSite.isWPCom());

        signOutWPCom();

        // Expect all WP.com sites to be removed
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(0, mSiteStore.getSitesAccessedViaWPComRestCount());

        // Site accessed via XMLRPC should not be removed
        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
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
            if (mNextEvent.equals(TestEvents.ERROR_DUPLICATE_SITE)) {
                Assert.assertEquals(SiteErrorType.DUPLICATE_SITE, event.error.type);
                mCountDownLatch.countDown();
                return;
            }
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertTrue(mSiteStore.hasSite());
        Assert.assertEquals(TestEvents.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(OnSiteRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        Assert.assertEquals(TestEvents.SITE_REMOVED, mNextEvent);
        mCountDownLatch.countDown();
    }

    private void authenticateWPComAndFetchSites(String username, String password) throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch account from REST API, and wait for OnAccountChanged event
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        Assert.assertTrue(mSiteStore.getSitesCount() > 0);
    }

    private void signOutWPCom() throws InterruptedException {
        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITE_REMOVED;
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction());

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSite(SiteModel site) throws InterruptedException {
        mNextEvent = TestEvents.SITE_CHANGED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(site));
        // Wait for a network response / onChanged event
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
