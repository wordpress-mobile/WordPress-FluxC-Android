package org.wordpress.android.fluxc.mocked;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.yarolegovich.wellsql.WellSql;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload;
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged;

import javax.inject.Inject;

/**
 * Tests using a Mocked Network app component. Test the Store itself and not the underlying network component(s).
 */
public class AccountStoreTest extends InstrumentationTestCase {
    @Inject Dispatcher mDispatcher;
    @Inject AccountStore mAccountStore;

    public boolean mIsError;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());

        Context appContext = getInstrumentation().getTargetContext().getApplicationContext();

        MockedNetworkAppComponent testComponent =  DaggerMockedNetworkAppComponent.builder()
                .appContextModule(new AppContextModule(appContext))
                .build();
        testComponent.inject(this);
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();

        // Register
        mDispatcher.register(this);
    }

    public void testAuthenticationOK() {
        AuthenticatePayload payload = new AuthenticatePayload("test", "test");
        mIsError = false;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    public void testAuthenticationKO() {
        AuthenticatePayload payload = new AuthenticatePayload("error", "error");
        mIsError = true;
        // Correct user we should get an OnAuthenticationChanged message
        mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
    }

    @Subscribe
    public void onAuthenticationChanged(OnAuthenticationChanged event) {
        assertEquals(mIsError, event.isError());
    }
}
