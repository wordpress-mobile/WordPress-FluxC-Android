package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.store.ThemeStore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_ThemeTestWPCom extends ReleaseStack_Base {
    enum TestEvents {
        NONE,
        FETCHED_THEMES,
    }

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

    public void testFetchWpComThemes() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        mNextEvent = TestEvents.FETCHED_THEMES;
        mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        if (mNextEvent == TestEvents.FETCHED_THEMES) {
            assertTrue(mThemeStore.getWpThemes().size() > 0);
            mCountDownLatch.countDown();
        }
    }
}
