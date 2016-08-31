package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_SiteTestWPCOM extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    CountDownLatch mCountDownLatch;

    enum TEST_EVENTS {
        NONE,
        SITE_CHANGED,
        POST_FORMATS_CHANGED,
        SITE_REMOVED
    }
    private TEST_EVENTS mExpectedEvent;

    private int mExpectedRowsAffected;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mExpectedEvent = TEST_EVENTS.NONE;
        mExpectedRowsAffected = 0;
    }

    public void testWPCOMSiteFetchAndLogout() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                        BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Clear WP.com sites, and wait for OnSiteRemoved event
        mCountDownLatch = new CountDownLatch(1);
        mExpectedEvent = TEST_EVENTS.SITE_REMOVED;
        mExpectedRowsAffected = mSiteStore.getSitesCount();
        mDispatcher.dispatch(SiteActionBuilder.newRemoveWpcomSitesAction());

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchPostFormats() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                        BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mExpectedEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Get the first site
        SiteModel firstSite = mSiteStore.getSites().get(0);

        // Fetch post formats
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(firstSite));
        mExpectedEvent = TEST_EVENTS.POST_FORMATS_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Test fetched Post Formats
        List<PostFormatModel> postFormats = mSiteStore.getPostFormats(firstSite);
        assertNotSame(0, postFormats.size());
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertEquals(false, event.isError());
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            return;
        }
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasDotComSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mExpectedEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void OnSiteRemoved(SiteStore.OnSiteRemoved event) {
        AppLog.e(T.TESTS, "site count " + mSiteStore.getSitesCount());
        assertEquals(mExpectedRowsAffected, event.mRowsAffected);
        assertEquals(false, mSiteStore.hasSite());
        assertEquals(false, mSiteStore.hasDotComSite());
        assertEquals(TEST_EVENTS.SITE_REMOVED, mExpectedEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onPostFormatsChanged(OnPostFormatsChanged event) {
        assertEquals(TEST_EVENTS.POST_FORMATS_CHANGED, mExpectedEvent);
        mCountDownLatch.countDown();
    }
}
