package org.wordpress.android.fluxc.release;

import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.ThemeStore;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestWPCom extends ReleaseStack_Base {
    enum TestEvents {
        NONE,
    }

    @Inject SiteStore mSiteStore;
    @Inject ThemeStore mThemeStore;
    private TestEvents mNextEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        mCountDownLatch.countDown();
    }
}
