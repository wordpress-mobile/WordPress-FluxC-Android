package org.wordpress.android.fluxc.release;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailErrorType;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadFlow;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadSource;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.FetchUsernameSuggestionsPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnDomainContactFetched;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameSuggestionsFetched;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Tests with real credentials on real servers using the full release stack (no mock)
 */
public class ReleaseStack_AccountTest extends ReleaseStack_Base {
    @Inject AccountStore mAccountStore;

    private enum TestEvents {
        NONE,
        AUTHENTICATE,
        INCORRECT_USERNAME_OR_PASSWORD_ERROR,
        AUTHENTICATE_2FA_ERROR,
        FETCHED,
        POSTED,
        FETCH_ERROR,
        SENT_AUTH_EMAIL,
        AUTH_EMAIL_ERROR_INVALID,
        AUTH_EMAIL_ERROR_NO_SUCH_USER,
        AUTH_EMAIL_ERROR_USER_EXISTS,
        CHANGE_USERNAME_ERROR_INVALID_INPUT,
        FETCH_USERNAME_SUGGESTIONS_ERROR_NO_NAME,
        FETCH_USERNAME_SUGGESTIONS_SUCCESS,
        FETCH_DOMAIN_CONTACT
    }

    private TestEvents mNextEvent;
    private boolean mExpectAccountInfosChanged;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register
        init();
        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testWPComAuthenticationOK() throws InterruptedException {
        if (mAccountStore.hasAccessToken()) {
            signOut();
        }

        mNextEvent = TestEvents.AUTHENTICATE;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
    }

    @Test
    public void testWPComAuthenticationIncorrectUsernameOrPassword() throws InterruptedException {
        mNextEvent = TestEvents.INCORRECT_USERNAME_OR_PASSWORD_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, "afakepassword19551105");
    }

    @Test
    public void testWPCom2faAuthentication() throws InterruptedException {
        mNextEvent = TestEvents.AUTHENTICATE_2FA_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_2FA, BuildConfig.TEST_WPCOM_PASSWORD_2FA);
    }

    @Test
    public void testWPComFetch() throws InterruptedException {
        // Ensure we're logged in as the primary test user
        // This is because we're validating the response based on what we know about this test account
        logInToPrimaryTestAccount();

        mNextEvent = TestEvents.FETCHED;
        mCountDownLatch = new CountDownLatch(2);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWPComPost() throws InterruptedException {
        // Ensure we're logged in as the primary test user
        // This is to avoid surprise changes to the description of non-test accounts
        logInToPrimaryTestAccount();

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = String.valueOf(System.currentTimeMillis());
        mExpectAccountInfosChanged = true;
        payload.params = new HashMap<>();
        payload.params.put("description", newValue);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, mAccountStore.getAccount().getAboutMe());
    }

    @Test
    public void testWPComPostNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mNextEvent = TestEvents.FETCHED;
        mCountDownLatch = new CountDownLatch(2);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = mAccountStore.getAccount().getAboutMe();
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("description", newValue);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, mAccountStore.getAccount().getAboutMe());
    }

    @Test
    public void testWPComPostPrimarySiteIdNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mNextEvent = TestEvents.FETCHED;
        mCountDownLatch = new CountDownLatch(2);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = String.valueOf(mAccountStore.getAccount().getPrimarySiteId());
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("primary_site_ID", newValue);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, String.valueOf(mAccountStore.getAccount().getPrimarySiteId()));
    }

    @Test
    public void testWPComPostTracksOptOutNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mNextEvent = TestEvents.FETCHED;
        mCountDownLatch = new CountDownLatch(2);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        Boolean newValue = mAccountStore.getAccount().getTracksOptOut();
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("tracks_opt_out", newValue);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, mAccountStore.getAccount().getTracksOptOut());
    }

    @Test
    public void testWPComPostTracksOptOut() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        } else if (!mAccountStore.getAccount().getUserName().equals(BuildConfig.TEST_WPCOM_USERNAME_TEST1)) {
            // If we're logged in as any user other than the test user, switch accounts
            // This is to avoid surprise changes to the description of non-test accounts
            signOut();
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mNextEvent = TestEvents.FETCHED;
        mCountDownLatch = new CountDownLatch(2);
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        Boolean newValue = !mAccountStore.getAccount().getTracksOptOut();
        mExpectAccountInfosChanged = true;
        payload.params = new HashMap<>();
        payload.params.put("tracks_opt_out", newValue);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(newValue, mAccountStore.getAccount().getTracksOptOut());
    }

    @Test
    public void testChangeWPComUsernameInvalidInputError() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        mNextEvent = TestEvents.CHANGE_USERNAME_ERROR_INVALID_INPUT;
        String username = mAccountStore.getAccount().getUserName();
        String address = mAccountStore.getAccount().getWebAddress();

        PushUsernamePayload payload = new PushUsernamePayload(username,
                AccountUsernameActionType.KEEP_OLD_SITE_AND_ADDRESS);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(username, String.valueOf(mAccountStore.getAccount().getUserName()));
        assertEquals(address, String.valueOf(mAccountStore.getAccount().getWebAddress()));
    }

    @Test
    public void testFetchWPComUsernameSuggestionsNoNameError() throws InterruptedException {
        mNextEvent = TestEvents.FETCH_USERNAME_SUGGESTIONS_ERROR_NO_NAME;

        FetchUsernameSuggestionsPayload payload = new FetchUsernameSuggestionsPayload("");
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchUsernameSuggestionsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchWPComUsernameSuggestionsSuccess() throws InterruptedException {
        mNextEvent = TestEvents.FETCH_USERNAME_SUGGESTIONS_SUCCESS;

        FetchUsernameSuggestionsPayload payload = new FetchUsernameSuggestionsPayload("username");
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchUsernameSuggestionsAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testWPComSignOut() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        signOut();

        assertFalse(mAccountStore.hasAccessToken());
        assertEquals(0, mAccountStore.getAccount().getUserId());
    }

    @Test
    public void testWPComSignOutCollision() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        mCountDownLatch = new CountDownLatch(2); // Wait for OnAuthenticationChanged and OnAccountChanged
        mNextEvent = TestEvents.AUTHENTICATE;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        Thread.sleep(100);
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mCountDownLatch = new CountDownLatch(1); // Wait for FETCH_ACCOUNT result
        mNextEvent = TestEvents.FETCH_ERROR;
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertFalse(mAccountStore.hasAccessToken());
        assertEquals(0, mAccountStore.getAccount().getUserId());
    }

    @Test
    public void testSendAuthEmail() throws InterruptedException {
        mNextEvent = TestEvents.SENT_AUTH_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload(BuildConfig.TEST_WPCOM_EMAIL_TEST1, false,
                null, null);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendAuthEmailViaUsername() throws InterruptedException {
        mNextEvent = TestEvents.SENT_AUTH_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1, false,
                AuthEmailPayloadFlow.JETPACK, null);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendAuthEmailInvalid() throws InterruptedException {
        // even for an invalid email address, the v1.3 /auth/send-login-email endpoint returns "User does not exist"
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_NO_SUCH_USER;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload("email@domain", false,
                null, AuthEmailPayloadSource.NOTIFICATIONS);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendAuthEmailNoSuchUser() throws InterruptedException {
        String unknownEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_NO_SUCH_USER;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload(unknownEmail, false,
                AuthEmailPayloadFlow.JETPACK, AuthEmailPayloadSource.STATS);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendAuthEmailSignup() throws InterruptedException {
        String unknownEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        mNextEvent = TestEvents.SENT_AUTH_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload(unknownEmail, true, null, null);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendAuthEmailSignupInvalid() throws InterruptedException {
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload("email@domain", true, AuthEmailPayloadFlow.JETPACK, null);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendAuthEmailSignupUserExists() throws InterruptedException {
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_USER_EXISTS;
        mCountDownLatch = new CountDownLatch(1);
        AuthEmailPayload payload = new AuthEmailPayload(BuildConfig.TEST_WPCOM_EMAIL_TEST1, true,
                null, AuthEmailPayloadSource.NOTIFICATIONS);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFetchDomainContact() throws InterruptedException {
        logInToPrimaryTestAccount();
        mNextEvent = TestEvents.FETCH_DOMAIN_CONTACT;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newFetchDomainContactAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onDomainContactFetched(OnDomainContactFetched event) {
        AppLog.i(AppLog.T.API, "Received onDomainContactFetched");
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(TestEvents.FETCH_DOMAIN_CONTACT, mNextEvent);
        assertNotNull(event.contactModel);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        AppLog.i(AppLog.T.API, "Received OnAuthenticationChanged");
        if (event.isError()) {
            switch (mNextEvent) {
                case AUTHENTICATE_2FA_ERROR:
                    assertEquals(event.error.type, AuthenticationErrorType.NEEDS_2FA);
                    break;
                case INCORRECT_USERNAME_OR_PASSWORD_ERROR:
                    assertEquals(event.error.type, AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD);
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            assertEquals(mNextEvent, TestEvents.AUTHENTICATE);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        AppLog.i(AppLog.T.API, "Received OnAccountChanged");
        if (event.isError()) {
            switch (event.error.type) {
                case ACCOUNT_FETCH_ERROR:
                    assertEquals(mNextEvent, TestEvents.FETCH_ERROR);
                    mCountDownLatch.countDown();
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            return;
        }
        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            assertEquals(mNextEvent, TestEvents.FETCHED);
            assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            assertEquals(mNextEvent, TestEvents.FETCHED);
            assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.PUSH_SETTINGS) {
            assertEquals(mNextEvent, TestEvents.POSTED);
            assertEquals(mExpectAccountInfosChanged, event.accountInfosChanged);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthEmailSent(OnAuthEmailSent event) {
        AppLog.i(AppLog.T.API, "Received OnAuthEmailSent");
        if (event.isError()) {
            AppLog.i(AppLog.T.API, "OnAuthEmailSent has error: " + event.error.type + " - " + event.error.message);
            if (event.error.type == AuthEmailErrorType.INVALID_EMAIL) {
                if (event.isSignup) {
                    assertTrue(mNextEvent == TestEvents.AUTH_EMAIL_ERROR_INVALID);
                } else {
                    assertTrue(mNextEvent == TestEvents.AUTH_EMAIL_ERROR_NO_SUCH_USER);
                }
                mCountDownLatch.countDown();
            } else if (event.error.type == AuthEmailErrorType.USER_EXISTS) {
                assertEquals(mNextEvent, TestEvents.AUTH_EMAIL_ERROR_USER_EXISTS);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            assertEquals(mNextEvent, TestEvents.SENT_AUTH_EMAIL);
            mCountDownLatch.countDown();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onUsernameChanged(OnUsernameChanged event) {
        AppLog.i(AppLog.T.API, "Received OnUsernameChanged");

        if (event.isError()) {
            AppLog.i(AppLog.T.API, "OnUsernameChanged has error: " + event.error.type + " - " + event.error.message);

            switch (event.error.type) {
                case GENERIC_ERROR:
                    throw new AssertionError("Error should not be tested: " + event.error.type);
                case INVALID_ACTION:
                    throw new AssertionError("Error should not occur with action type enum: " + event.type);
                case INVALID_INPUT:
                    assertEquals(mNextEvent, TestEvents.CHANGE_USERNAME_ERROR_INVALID_INPUT);
                    mCountDownLatch.countDown();
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        }
    }


    @SuppressWarnings("unused")
    @Subscribe
    public void onUsernameSuggestionsFetched(OnUsernameSuggestionsFetched event) {
        AppLog.i(AppLog.T.API, "Received OnUsernameSuggestionsFetched");

        if (event.isError()) {
            AppLog.i(AppLog.T.API, "OnUsernameSuggestionsFetched: " + event.error.type + " - " + event.error.message);

            switch (event.error.type) {
                case GENERIC_ERROR:
                    throw new AssertionError("Error should not be tested: " + event.error.type);
                case REST_MISSING_CALLBACK_PARAM:
                    throw new AssertionError("Error should not occur with name parameter");
                case REST_NO_NAME:
                    assertEquals(mNextEvent, TestEvents.FETCH_USERNAME_SUGGESTIONS_ERROR_NO_NAME);
                    mCountDownLatch.countDown();
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else if (event.suggestions.size() != 0) {
            assertEquals(mNextEvent, TestEvents.FETCH_USERNAME_SUGGESTIONS_SUCCESS);
            mCountDownLatch.countDown();
        }
    }

    private void logInToPrimaryTestAccount() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        } else if (!mAccountStore.getAccount().getUserName().equals(BuildConfig.TEST_WPCOM_USERNAME_TEST1)) {
            // If we're logged in as any user other than the test user, switch accounts
            // This is to avoid surprise changes to the description of non-test accounts
            signOut();
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
    }
    private void authenticate(String username, String password) throws InterruptedException {
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void signOut() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(2); // Wait for OnAuthenticationChanged and OnAccountChanged
        mNextEvent = TestEvents.AUTHENTICATE;
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
