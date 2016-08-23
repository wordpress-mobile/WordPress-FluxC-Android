package org.wordpress.android.fluxc.mocked;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;

public class MockedStack_Base extends InstrumentationTestCase {
    Context mAppContext;
    MockedNetworkAppComponent mMockedNetworkAppComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        mAppContext = getInstrumentation().getTargetContext().getApplicationContext();

        mMockedNetworkAppComponent =  DaggerMockedNetworkAppComponent.builder()
                .appContextModule(new AppContextModule(mAppContext))
                .build();
        WellSqlConfig config = new WellSqlConfig(mAppContext);
        WellSql.init(config);
        config.reset();
    }
}
