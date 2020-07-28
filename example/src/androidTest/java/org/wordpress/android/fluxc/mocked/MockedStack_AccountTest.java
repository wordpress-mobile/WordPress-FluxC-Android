package org.wordpress.android.fluxc.mocked;

import androidx.test.runner.AndroidJUnit4;

import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.FetchAuthOptionsPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthOptionsFetched;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameChanged;
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 */
@RunWith(AndroidJUnit4.class)
public class MockedStack_AccountTest extends MockedStack_Base {
    private static final String TEST_USERNAME = "TEST_USERNAME";
    private static final String TEST_UNVERIFIED_EMAIL = "test_unverified_email@example.org";
    private static final String TEST_SUSPICIOUS_EMAIL = "test_suspicious_email@example.org";

    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    @Inject ResponseMockingInterceptor mInterceptor;

    enum TestEvents {
        NONE,
        CHANGE_USERNAME_SUCCESSFUL,
        FETCH_AUTH_OPTIONS_UNVERIFIED_EMAIL,
        FETCH_AUTH_OPTIONS_ERROR_EMAIL_LOGIN_NOT_ALLOWED
    }

    private TestEvents mNextEvent;
    private boolean mIsError;
    private CountDownLatch mCountDownLatch;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    @After
    public void tearDown() {
        if (mAccountStore.hasAccessToken()) {
            throw new AssertionError("Mock account tests should clear the AccountStore!");
        }
    }

    @Test
    public void testAuthenticationOK() throws InterruptedException {
        if (mAccountStore.hasAccessToken()) {
            signOut();
        }

        AuthenticatePayload payload = new AuthenticatePayload("test", "test");
        mIsError = false;
        // Correct user we should get an OnAuthenticationChanged message
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        mInterceptor.respondWith("authentication-success-response.json");
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Log out and clear stored dummy account
        signOut();
    }

    @Test
    public void testAuthenticationKO() throws InterruptedException {
        if (mAccountStore.hasAccessToken()) {
            signOut();
        }
        AuthenticatePayload payload = new AuthenticatePayload("error", "error");
        mIsError = true;
        // Correct user we should get an OnAuthenticationChanged message
        mCountDownLatch = new CountDownLatch(1);
        mInterceptor.respondWith("authentication-error-response.json");
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testChangeWPComUsername() throws InterruptedException {
        if (mAccountStore.hasAccessToken()) {
            signOut();
        }
        mNextEvent = TestEvents.CHANGE_USERNAME_SUCCESSFUL;
        PushUsernamePayload payload = new PushUsernamePayload(TEST_USERNAME,
                AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS);
        mCountDownLatch = new CountDownLatch(1);
        mInterceptor.respondWith("change-username-success.json");
        mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchAuthOptionsForUserWithUnverifiedEmail() throws InterruptedException {
        mNextEvent = TestEvents.FETCH_AUTH_OPTIONS_UNVERIFIED_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        mInterceptor.respondWith("fetch-auth-options-response-success-unverified-email.json");
        FetchAuthOptionsPayload payload = new FetchAuthOptionsPayload(TEST_UNVERIFIED_EMAIL);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAuthOptionsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchAuthOptionsForUserWithSuspiciousEmail() throws InterruptedException {
        mNextEvent = TestEvents.FETCH_AUTH_OPTIONS_ERROR_EMAIL_LOGIN_NOT_ALLOWED;
        mCountDownLatch = new CountDownLatch(1);
        mInterceptor.respondWithError("fetch-auth-options-response-failure-email-login-not-allowed.json");
        FetchAuthOptionsPayload payload = new FetchAuthOptionsPayload(TEST_SUSPICIOUS_EMAIL);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAuthOptionsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        assertEquals(mIsError, event.isError());
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            throw new AssertionError("Got unexpected error in OnAccountChanged: " + event.error.type);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUsernameChanged(OnUsernameChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.CHANGE_USERNAME_SUCCESSFUL);
        assertEquals(TEST_USERNAME, event.username);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthOptionsFetched(OnAuthOptionsFetched event) {
        if (event.isError()) {
            switch (event.error.type) {
                case GENERIC_ERROR:
                    throw new AssertionError("Error should not be tested: " + event.error.type);
                case EMAIL_LOGIN_NOT_ALLOWED:
                    assertEquals(mNextEvent, TestEvents.FETCH_AUTH_OPTIONS_ERROR_EMAIL_LOGIN_NOT_ALLOWED);
                    mCountDownLatch.countDown();
                    break;
                case UNKNOWN_USER:
                    // We handle this error in the release stack.
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else if (!event.isEmailVerified) {
            assertEquals(mNextEvent, TestEvents.FETCH_AUTH_OPTIONS_UNVERIFIED_EMAIL);
            mCountDownLatch.countDown();
        } else {
            throw new AssertionError("Unexpected verified email in the mocked stack");
        }
    }

    private void signOut() throws InterruptedException {
        mIsError = false;
        mCountDownLatch = new CountDownLatch(2); // Wait for OnAuthenticationChanged and OnAccountChanged
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
