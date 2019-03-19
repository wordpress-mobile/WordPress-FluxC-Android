package org.wordpress.android.fluxc.mocked;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.yarolegovich.wellsql.WellSql;

import org.junit.After;
import org.junit.Before;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import javax.inject.Inject;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

public class MockedStack_Base {
    Context mAppContext;
    MockedNetworkAppComponent mMockedNetworkAppComponent;

    @Inject RequestQueue mRequestQueue;

    @Before
    public void setUp() throws Exception {
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        mAppContext = getInstrumentation().getTargetContext().getApplicationContext();

        mMockedNetworkAppComponent = DaggerMockedNetworkAppComponent.builder()
                .appContextModule(new AppContextModule(mAppContext))
                .build();
        WellSqlConfig config = new WellSqlConfig(mAppContext, WellSqlConfig.ADDON_WOOCOMMERCE);
        WellSql.init(config);
        config.reset();
    }

    @After
    public void tearDown() {
        // Spin down the queue to free up memory
        // Without this, running the full connected test suite will sometimes fail with 'out of memory' errors
        if (mRequestQueue != null) {
            mRequestQueue.stop();
        }
    }

    String getSampleImagePath() {
        return TestUtils.getSampleImagePath(getInstrumentation().getContext(), getInstrumentation().getTargetContext());
    }
}
