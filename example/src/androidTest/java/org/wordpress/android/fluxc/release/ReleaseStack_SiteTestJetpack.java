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
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;
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
        SITE_REMOVED
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
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_SINGLE_JETPACK_ONLY,
                BuildConfig.TEST_WPCOM_PASSWORD_SINGLE_JETPACK_ONLY);

        assertEquals(1, mSiteStore.getSitesCount());
        assertTrue(mSiteStore.hasWPComSite());
        assertTrue(mSiteStore.hasJetpackSite());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testWPComSingleJetpackSiteFetch() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_ONE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_ONE_JETPACK);

        List<SiteModel> sites = mSiteStore.getSites();

        assertEquals(2, sites.size());

        assertTrue(sites.get(0).isWPCom());
        assertTrue(sites.get(1).isWPCom());

        // Only one of the two sites is expected to be a Jetpack site
        assertTrue(sites.get(0).isJetpack() != sites.get(1).isJetpack());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testWPComMultipleJetpackSiteFetch() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_MULTIPLE_JETPACK,
                BuildConfig.TEST_WPCOM_PASSWORD_MULTIPLE_JETPACK);

        List<SiteModel> sites = mSiteStore.getSites();
        List<SiteModel> jetpackSites = new ArrayList<>(2);
        List<SiteModel> nonJetpackSites = new ArrayList<>(1);

        for (SiteModel site : sites) {
            assertTrue(site.isWPCom());
            if (site.isJetpack()) {
                jetpackSites.add(site);
            } else {
                nonJetpackSites.add(site);
            }
        }

        assertEquals(2, jetpackSites.size());
        assertEquals(1, nonJetpackSites.size());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
    }

    public void testWPComJetpackMultisiteSiteFetch() throws InterruptedException {
        authenticateAndFetchSites(BuildConfig.TEST_WPCOM_USERNAME_JETPACK_MULTISITE,
                BuildConfig.TEST_WPCOM_PASSWORD_JETPACK_MULTISITE);

        List<SiteModel> sites = mSiteStore.getSites();
        List<SiteModel> nonJetpackSites = new ArrayList<>(1);

        for (SiteModel site : sites) {
            assertTrue(site.isWPCom());
            if (!site.isJetpack()) {
                nonJetpackSites.add(site);
            }
        }

        // Only one non-Jetpack site exists, all the other fetched sites should be Jetpack sites
        assertEquals(1, nonJetpackSites.size());

        signOutWPCom();

        assertFalse(mSiteStore.hasSite());
        assertFalse(mSiteStore.hasWPComSite());
        assertFalse(mSiteStore.hasJetpackSite());
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

    private void authenticateAndFetchSites(String username, String password) throws InterruptedException {
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
}
