package org.wordpress.android.fluxc.mocked;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;

import javax.inject.Inject;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 */
public class AccountStoreTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    public boolean mIsError;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
    }

    public void testAuthenticationOK() {
        AuthenticatePayload payload = new AuthenticatePayload("test", "test");
        mIsError = false;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatchAsk(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    public void testAuthenticationKO() {
        AuthenticatePayload payload = new AuthenticatePayload("error", "error");
        mIsError = true;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatchAsk(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        assertEquals(mIsError, event.isError());
    }
}
