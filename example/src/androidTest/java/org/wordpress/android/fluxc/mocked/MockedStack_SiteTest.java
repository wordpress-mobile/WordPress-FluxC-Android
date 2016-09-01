package org.wordpress.android.fluxc.mocked;

import org.wordpress.android.fluxc.Dispatcher;

import javax.inject.Inject;

public class MockedStack_SiteTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
    }
}
