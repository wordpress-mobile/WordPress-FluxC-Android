package org.wordpress.android.fluxc.mocked;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;

import javax.inject.Inject;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 */
@RunWith(AndroidJUnit4.class)
public class MockedStack_AccountTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    public boolean mIsError;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
    }

    @Test
    public void testAuthenticationOK() {
        AuthenticatePayload payload = new AuthenticatePayload("test", "test");
        mIsError = false;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    @Test
    public void testAuthenticationKO() {
        AuthenticatePayload payload = new AuthenticatePayload("error", "error");
        mIsError = true;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        Assert.assertEquals(mIsError, event.isError());
    }
}
