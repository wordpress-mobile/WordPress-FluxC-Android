package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.OnPluginInfoChanged;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_PluginInfoTest extends ReleaseStack_Base {
    @Inject PluginStore mPluginStore;

    enum TestEvents {
        NONE,
        PLUGIN_INFO_FETCHED,
    }

    private TestEvents mNextEvent;
    private final String mSlug = "akismet";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        // Register
        init();
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    public void testFetchPlugins() throws InterruptedException {
        mNextEvent = TestEvents.PLUGIN_INFO_FETCHED;
        mCountDownLatch = new CountDownLatch(1);

        mDispatcher.dispatch(PluginActionBuilder.newFetchPluginInfoAction(mSlug));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPluginInfoChanged(OnPluginInfoChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }

        assertEquals(TestEvents.PLUGIN_INFO_FETCHED, mNextEvent);
        PluginInfoModel pluginInfo = mPluginStore.getPluginInfoBySlug(mSlug);
        assertNotNull(pluginInfo);
        mCountDownLatch.countDown();
    }
}
