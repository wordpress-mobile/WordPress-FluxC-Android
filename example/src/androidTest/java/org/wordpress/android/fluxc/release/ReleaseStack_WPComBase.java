package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_WPComBase extends ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    protected CountDownLatch mCountDownLatch;
    protected SiteModel mSite;

    private enum TEST_EVENTS {
        NONE,
        AUTHENTICATED,
        SITE_CHANGED,
    }
    private TEST_EVENTS mNextEvent;


    protected void init()  throws Exception {
        // Register
        mNextEvent = TEST_EVENTS.NONE;

        mDispatcher.register(this);

        if (mAccountStore.getAccessToken().isEmpty()) {
            authenticate();
        }

        if (mSite == null) {
            fetchSites();
            mSite = mSiteStore.getSites().get(0);
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
        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSites() throws InterruptedException {
        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TEST_EVENTS.SITE_CHANGED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertEquals(true, mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Subscribe
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        assertEquals(false, event.isError());
        assertEquals(TEST_EVENTS.AUTHENTICATED, mNextEvent);
        mCountDownLatch.countDown();
    }

    @Subscribe
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        AppLog.i(T.TESTS, "site count " + mSiteStore.getSitesCount());
        if (event.isError()) {
            AppLog.i(T.TESTS, "event error type: " + event.error.type);
            return;
        }
        assertEquals(true, mSiteStore.hasSite());
        assertEquals(true, mSiteStore.hasWPComSite());
        assertEquals(TEST_EVENTS.SITE_CHANGED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
