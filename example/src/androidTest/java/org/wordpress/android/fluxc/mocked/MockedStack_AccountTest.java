package org.wordpress.android.fluxc.mocked;

import android.support.test.runner.AndroidJUnit4;

import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;

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
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

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
    }

    @After
    public void tearDown() throws Exception {
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
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
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

    private void signOut() throws InterruptedException {
        mIsError = false;
        mCountDownLatch = new CountDownLatch(2); // Wait for OnAuthenticationChanged and OnAccountChanged
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
