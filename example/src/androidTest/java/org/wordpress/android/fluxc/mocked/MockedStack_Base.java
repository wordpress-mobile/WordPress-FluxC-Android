package org.wordpress.android.fluxc.mocked;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

public class MockedStack_Base {
    Context mAppContext;
    MockedNetworkAppComponent mMockedNetworkAppComponent;

    @Before
    public void setUp() throws Exception {
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        mAppContext = getInstrumentation().getTargetContext().getApplicationContext();

        mMockedNetworkAppComponent = DaggerMockedNetworkAppComponent.builder()
                .appContextModule(new AppContextModule(mAppContext))
                .build();
        WellSqlConfig config = new WellSqlConfig(mAppContext);
        WellSql.init(config);
        config.reset();
    }

    String getSampleImagePath() {
        return TestUtils.getSampleImagePath(getInstrumentation().getContext(), getInstrumentation().getTargetContext());
    }
}
