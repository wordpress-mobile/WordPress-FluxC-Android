package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.example.BuildConfig;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AccountUsernameActionType;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailErrorType;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.fluxc.store.AccountStore.FetchUsernameSuggestionsPayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthEmailSent;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnUsernameSuggestionsFetched;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.AccountStore.PushUsernamePayload;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

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
        FETCH_USERNAME_SUGGESTIONS_SUCCESS
    }

    private TestEvents mNextEvent;
    private boolean mExpectAccountInfosChanged;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register
        init();
        mNextEvent = TestEvents.NONE;
    }

    public void testWPComAuthenticationOK() throws InterruptedException {
        mNextEvent = TestEvents.AUTHENTICATE;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
    }

    public void testWPComAuthenticationIncorrectUsernameOrPassword() throws InterruptedException {
        mNextEvent = TestEvents.INCORRECT_USERNAME_OR_PASSWORD_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_BAD_PASSWORD);
    }

    public void testWPCom2faAuthentication() throws InterruptedException {
        mNextEvent = TestEvents.AUTHENTICATE_2FA_ERROR;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_2FA, BuildConfig.TEST_WPCOM_PASSWORD_2FA);
    }

    public void testWPComFetch() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
        mNextEvent = TestEvents.FETCHED;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        mCountDownLatch = new CountDownLatch(2);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPComPost() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }
        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = String.valueOf(System.currentTimeMillis());
        mExpectAccountInfosChanged = true;
        payload.params = new HashMap<>();
        payload.params.put("description", newValue);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(newValue, mAccountStore.getAccount().getAboutMe());
    }

    public void testWPComPostNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mNextEvent = TestEvents.FETCHED;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        mCountDownLatch = new CountDownLatch(2);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = mAccountStore.getAccount().getAboutMe();
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("description", newValue);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(newValue, mAccountStore.getAccount().getAboutMe());
    }

    public void testWPComPostPrimarySiteIdNoChange() throws InterruptedException {
        if (!mAccountStore.hasAccessToken()) {
            mNextEvent = TestEvents.AUTHENTICATE;
            authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);
        }

        // First, fetch account settings
        mNextEvent = TestEvents.FETCHED;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        mCountDownLatch = new CountDownLatch(2);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mNextEvent = TestEvents.POSTED;
        PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
        String newValue = String.valueOf(mAccountStore.getAccount().getPrimarySiteId());
        mExpectAccountInfosChanged = false;
        payload.params = new HashMap<>();
        payload.params.put("primary_site_ID", newValue);
        mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(newValue, String.valueOf(mAccountStore.getAccount().getPrimarySiteId()));
    }

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
        mDispatcher.dispatch(AccountActionBuilder.newPushUsernameAction(payload));

        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(username, String.valueOf(mAccountStore.getAccount().getUserName()));
        Assert.assertEquals(address, String.valueOf(mAccountStore.getAccount().getWebAddress()));
    }

    public void testFetchWPComUsernameSuggestionsNoNameError() throws InterruptedException {
        mNextEvent = TestEvents.FETCH_USERNAME_SUGGESTIONS_ERROR_NO_NAME;

        FetchUsernameSuggestionsPayload payload = new FetchUsernameSuggestionsPayload("");
        mDispatcher.dispatch(AccountActionBuilder.newFetchUsernameSuggestionsAction(payload));

        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testFetchWPComUsernameSuggestionsSuccess() throws InterruptedException {
        mNextEvent = TestEvents.FETCH_USERNAME_SUGGESTIONS_SUCCESS;

        FetchUsernameSuggestionsPayload payload = new FetchUsernameSuggestionsPayload("username");
        mDispatcher.dispatch(AccountActionBuilder.newFetchUsernameSuggestionsAction(payload));

        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testWPComSignOut() throws InterruptedException {
        mNextEvent = TestEvents.AUTHENTICATE;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        mCountDownLatch = new CountDownLatch(2); // Wait for OnAuthenticationChanged and OnAccountChanged
        mNextEvent = TestEvents.AUTHENTICATE;
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertFalse(mAccountStore.hasAccessToken());
        Assert.assertEquals(0, mAccountStore.getAccount().getUserId());
    }

    public void testWPComSignOutCollision() throws InterruptedException {
        mNextEvent = TestEvents.AUTHENTICATE;
        authenticate(BuildConfig.TEST_WPCOM_USERNAME_TEST1, BuildConfig.TEST_WPCOM_PASSWORD_TEST1);

        mCountDownLatch = new CountDownLatch(2); // Wait for OnAuthenticationChanged and OnAccountChanged
        mNextEvent = TestEvents.AUTHENTICATE;
        mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
        Thread.sleep(100);
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        mCountDownLatch = new CountDownLatch(1); // Wait for FETCH_ACCOUNT result
        mNextEvent = TestEvents.FETCH_ERROR;
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertFalse(mAccountStore.hasAccessToken());
        Assert.assertEquals(0, mAccountStore.getAccount().getUserId());
    }

    public void testSendAuthEmail() throws InterruptedException {
        mNextEvent = TestEvents.SENT_AUTH_EMAIL;
        AuthEmailPayload payload = new AuthEmailPayload(BuildConfig.TEST_WPCOM_EMAIL_TEST1, false);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailViaUsername() throws InterruptedException {
        mNextEvent = TestEvents.SENT_AUTH_EMAIL;
        AuthEmailPayload payload = new AuthEmailPayload(BuildConfig.TEST_WPCOM_USERNAME_TEST1, false);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailInvalid() throws InterruptedException {
        // even for an invalid email address, the v1.3 /auth/send-login-email endpoint returns "User does not exist"
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_NO_SUCH_USER;
        AuthEmailPayload payload = new AuthEmailPayload("email@domain", false);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailNoSuchUser() throws InterruptedException {
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_NO_SUCH_USER;
        String unknownEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        AuthEmailPayload payload = new AuthEmailPayload(unknownEmail, false);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailSignup() throws InterruptedException {
        mNextEvent = TestEvents.SENT_AUTH_EMAIL;
        String unknownEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        AuthEmailPayload payload = new AuthEmailPayload(unknownEmail, true);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailSignupInvalid() throws InterruptedException {
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_INVALID;
        AuthEmailPayload payload = new AuthEmailPayload("email@domain", true);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    public void testSendAuthEmailSignupUserExists() throws InterruptedException {
        mNextEvent = TestEvents.AUTH_EMAIL_ERROR_USER_EXISTS;
        AuthEmailPayload payload = new AuthEmailPayload(BuildConfig.TEST_WPCOM_EMAIL_TEST1, true);
        mDispatcher.dispatch(AuthenticationActionBuilder.newSendAuthEmailAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        AppLog.i(AppLog.T.API, "Received OnAuthenticationChanged");
        if (event.isError()) {
            switch (mNextEvent) {
                case AUTHENTICATE_2FA_ERROR:
                    Assert.assertEquals(event.error.type, AuthenticationErrorType.NEEDS_2FA);
                    break;
                case INCORRECT_USERNAME_OR_PASSWORD_ERROR:
                    Assert.assertEquals(event.error.type, AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD);
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            Assert.assertEquals(mNextEvent, TestEvents.AUTHENTICATE);
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
                    Assert.assertEquals(mNextEvent, TestEvents.FETCH_ERROR);
                    mCountDownLatch.countDown();
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
            return;
        }
        if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
            Assert.assertEquals(mNextEvent, TestEvents.FETCHED);
            Assert.assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            Assert.assertEquals(mNextEvent, TestEvents.FETCHED);
            Assert.assertEquals(BuildConfig.TEST_WPCOM_USERNAME_TEST1, mAccountStore.getAccount().getUserName());
        } else if (event.causeOfChange == AccountAction.PUSH_SETTINGS) {
            Assert.assertEquals(mNextEvent, TestEvents.POSTED);
            Assert.assertEquals(mExpectAccountInfosChanged, event.accountInfosChanged);
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
                    Assert.assertTrue(mNextEvent == TestEvents.AUTH_EMAIL_ERROR_INVALID);
                } else {
                    Assert.assertTrue(mNextEvent == TestEvents.AUTH_EMAIL_ERROR_NO_SUCH_USER);
                }
                mCountDownLatch.countDown();
            } else if (event.error.type == AuthEmailErrorType.USER_EXISTS) {
                Assert.assertEquals(mNextEvent, TestEvents.AUTH_EMAIL_ERROR_USER_EXISTS);
                mCountDownLatch.countDown();
            } else {
                throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else {
            Assert.assertEquals(mNextEvent, TestEvents.SENT_AUTH_EMAIL);
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
                    Assert.assertEquals(mNextEvent, TestEvents.CHANGE_USERNAME_ERROR_INVALID_INPUT);
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
                    Assert.assertEquals(mNextEvent, TestEvents.FETCH_USERNAME_SUGGESTIONS_ERROR_NO_NAME);
                    mCountDownLatch.countDown();
                    break;
                default:
                    throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
            }
        } else if (event.suggestions.size() != 0) {
            Assert.assertEquals(mNextEvent, TestEvents.FETCH_USERNAME_SUGGESTIONS_SUCCESS);
            mCountDownLatch.countDown();
        }
    }

    private void authenticate(String username, String password) throws InterruptedException {
        AuthenticatePayload payload = new AuthenticatePayload(username, password);
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
