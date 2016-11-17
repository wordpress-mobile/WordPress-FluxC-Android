package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_WPComBase extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    CountDownLatch mCountDownLatch;
    static SiteModel sSite;

    private enum TEST_EVENTS {
        NONE,
        AUTHENTICATED,
        SITE_CHANGED,
    }
    private TEST_EVENTS mNextEvent;

    protected void init() throws Exception {
        // Register
        mNextEvent = TEST_EVENTS.NONE;

        mDispatcher.register(this);

        if (mAccountStore.getAccessToken().isEmpty()) {
            authenticate();
        }

        if (sSite == null) {
            fetchSites();
            sSite = mSiteStore.getSites().get(0);
        }
    }

    private void authenticate() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        AccountStore.AuthenticatePayload payload =
                new AccountStore.AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                        BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        mCountDownLatch = new CountDownLatch(1);

        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        // Wait for a network response / onChanged event
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSites() throws InterruptedException {
        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        assertFalse(event.isError());
        assertEquals(TEST_EVENTS.AUTHENTICATED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            throw new AssertionError("event error type: " + event.error.type);
        }
        assertTrue(mSiteStore.hasSite());
        assertTrue(mSiteStore.hasWPComSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
