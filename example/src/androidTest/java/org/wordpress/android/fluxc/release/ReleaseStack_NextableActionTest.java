package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.annotations.action.NextableAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_NextableActionTest extends ReleaseStack_Base {
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TestEvents {
        NONE,
        AUTHENTICATED,
        ACCOUNT_FETCHED,
        SETTINGS_FETCHED,
        SITES_FETCHED,
        AUTH_ERROR
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

    public void testWPComSiteChainedFetch() throws InterruptedException {
        // Authenticate a test user (actual credentials declared in gradle.properties)
        // Fetch account and sites, and wait for OnSiteChanged event
        AuthenticatePayload payload = new AuthenticatePayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1,
                BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        NextableAction authAction = AuthenticationActionBuilder.newAuthenticateAction(payload);
        authAction.doNextOnSuccess(AccountActionBuilder.newFetchAccountAction())
                .doNextOnSuccess(AccountActionBuilder.newFetchSettingsAction())
                .doNextOnSuccess(SiteActionBuilder.newFetchSitesAction());

        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.AUTHENTICATED;
        mDispatcher.dispatch(authAction);

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.ACCOUNT_FETCHED;

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SETTINGS_FETCHED;

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.SITES_FETCHED;

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mSiteStore.getSitesCount() > 0);
    }

    public void testWPComSiteChainedFetchAuthFailure() throws InterruptedException {
        // First, make sure we're completely signed out, with no token
        mCountDownLatch = new CountDownLatch(2); // Two events: OnAuthenticationChanged and OnAccountChanged
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Attempt to authenticate with invalid credentials
        // Fetch account and sites, and wait for OnSiteChanged event
        AuthenticatePayload payload = new AuthenticatePayload("notarealusername14562", "secret");
        NextableAction authAction = AuthenticationActionBuilder.newAuthenticateAction(payload);
        authAction.doNextOnSuccess(AccountActionBuilder.newFetchAccountAction())
                .doNextOnSuccess(AccountActionBuilder.newFetchSettingsAction())
                .doNextOnSuccess(SiteActionBuilder.newFetchSitesAction());

        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.AUTH_ERROR;
        mDispatcher.dispatch(authAction);

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        mCountDownLatch = new CountDownLatch(1);

        // Auth failed, so FETCH_ACCOUNT and the rest of the actions shouldn't get dispatched at all
        assertFalse(mCountDownLatch.await(5, TimeUnit.SECONDS));
        assertFalse(mAccountStore.hasAccessToken());
        assertEquals(0, mAccountStore.getAccount().getUserId());
        assertFalse(mSiteStore.hasSite());
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        AppLog.i(T.API, "Received OnAuthenticationChanged");
        if (event.isError()) {
            assertEquals(mNextEvent, TestEvents.AUTH_ERROR);
        } else {
            if (mAccountStore.hasAccessToken()) {
                assertEquals(mNextEvent, TestEvents.AUTHENTICATED);
            }
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        AppLog.i(T.API, "Received OnAccountChanged");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        } else {
            if (event.causeOfChange != null) {
                switch (event.causeOfChange) {
                    case FETCH_ACCOUNT:
                        assertEquals(mNextEvent, TestEvents.ACCOUNT_FETCHED);
                        assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
                        break;
                    case FETCH_SETTINGS:
                        assertEquals(mNextEvent, TestEvents.SETTINGS_FETCHED);
                        assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
                        break;
                }
            }
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
        if (mSiteStore.getSitesCount() > 0) {
            assertTrue(mSiteStore.hasWPComSite());
        }
        assertEquals(TestEvents.SITES_FETCHED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
