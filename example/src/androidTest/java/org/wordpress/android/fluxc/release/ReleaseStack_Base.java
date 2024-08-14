package org.wordpress.android.fluxc.release;

import android.content.Context;

import static androidx.test.InstrumentationRegistry.getInstrumentation;

import com.android.volley.RequestQueue;
import com.yarolegovich.wellsql.WellSql;

import org.junit.After;
import org.junit.Before;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;
import javax.inject.Named;

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
public class ReleaseStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject @Named("regular") RequestQueue mRequestQueueRegular;
    @Inject @Named("custom-ssl") RequestQueue mRequestQueueCustomSsl;
    @Inject WCAndroidDatabase mRoomDatabase;

    Context mAppContext;
    ReleaseStack_AppComponent mReleaseStackAppComponent;
    CountDownLatch mCountDownLatch;

    @Before
    public void setUp() throws Exception {
        setUp(true);
    }

    protected void setUp(boolean reset) throws Exception {
        mAppContext = getInstrumentation().getTargetContext().getApplicationContext();

        mReleaseStackAppComponent = DaggerReleaseStack_AppComponent.builder()
                                                                   .appContextModule(new AppContextModule(mAppContext))
                                                                   .build();
        WellSqlConfig config = new WellSqlConfig(mAppContext, WellSqlConfig.ADDON_WOOCOMMERCE);
        WellSql.init(config);

        // There are some suites that do not require the db to the be reset before each test
        if (reset) config.reset();
    }

    @After
    public void tearDown() {
        // Spin down the queue to free up memory
        // Without this, running the full connected test suite will sometimes fail with 'out of memory' errors
        if (mRequestQueueRegular != null) {
            mRequestQueueRegular.stop();
        }
        if (mRequestQueueCustomSsl != null) {
            mRequestQueueCustomSsl.stop();
        }

        mRoomDatabase.clearAllTables();
    }

    protected void init() throws Exception {
        mDispatcher.register(this);
    }

    String getSampleImagePath() {
        return TestUtils.getSampleImagePath(getInstrumentation().getContext(), getInstrumentation().getTargetContext());
    }

    String getSampleVideoPath() {
        return TestUtils.getSampleVideoPath(getInstrumentation().getContext(), getInstrumentation().getTargetContext());
    }
}
