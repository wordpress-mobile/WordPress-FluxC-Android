package org.wordpress.android.fluxc.release;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

/**
 * NOTE:
 * Stores are not instantiated by default when running tests. If a test sub-class dispatches an
 * event or listens for an event from a Store, that test class MUST INJECT THE STORE even if the
 * Store is not explicitly used.
 *
 * For example:
 *  ReleaseStack_SiteTestWPCOM dispatches an authentication event that is handled by AccountStore.
 *  Therefore the test class must provide an injected AccountStore member, even though
 *  methods/properties from the AccountStore are never explicitly invoked.
 */
public class ReleaseStack_Base extends InstrumentationTestCase {
    @Inject Dispatcher mDispatcher;

    Context mAppContext;
    ReleaseStack_AppComponent mReleaseStackAppComponent;
    CountDownLatch mCountDownLatch;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        mAppContext = getInstrumentation().getTargetContext().getApplicationContext();

        mReleaseStackAppComponent = DaggerReleaseStack_AppComponent.builder()
                .appContextModule(new AppContextModule(mAppContext))
                .build();
        WellSqlConfig config = new WellSqlConfig(mAppContext);
        WellSql.init(config);
        config.reset();
    }

    protected void init() throws Exception {
        mDispatcher.register(this);
    }
}
