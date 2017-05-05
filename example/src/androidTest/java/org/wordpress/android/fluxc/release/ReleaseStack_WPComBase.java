package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.annotations.action.NextableAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_WPComBase extends ReleaseStack_Base {
    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    static SiteModel sSite;

    private enum TestEvents {
        NONE,
        ACCOUNT_FETCHED,
        SITES_FETCHED
    }

    private TestEvents mNextEvent;

    @Override
    protected void init() throws Exception {
        super.init();
        mNextEvent = TestEvents.NONE;

        if (!mAccountStore.getAccessToken().isEmpty() && sSite != null) {
            // We have all we need, move on (the AccountStore is probably empty, but we don't need it)
            return;
        }

        if (mAccountStore.getAccessToken().isEmpty() || mAccountStore.getAccount().getUserId() == 0) {
            authenticate();
        }

        if (sSite == null) {
            fetchSites();
            sSite = mSiteStore.getSites().get(0);
        }
    }

    private void authenticate() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        // Fetch account and sites, and wait for OnSiteChanged event
        AuthenticatePayload payload = new AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        NextableAction authAction = AuthenticationActionBuilder.newAuthenticateAction(payload);
        authAction.doNextOnSuccess(AccountActionBuilder.newFetchAccountAction());

        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACCOUNT_FETCHED;
        mDispatcher.dispatch(authAction);

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void fetchSites() throws InterruptedException {
        // Fetch sites from REST API, and wait for onSiteChanged event
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITES_FETCHED;
        mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction());

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        assertFalse(event.isError());
        assertEquals(TestEvents.ACCOUNT_FETCHED, mNextEvent);
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
        assertTrue(mSiteStore.hasWPComSite());
        assertEquals(TestEvents.SITES_FETCHED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
