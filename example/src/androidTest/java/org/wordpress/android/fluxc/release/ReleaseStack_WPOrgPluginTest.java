package org.wordpress.android.fluxc.release;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.WPOrgPluginModel;
import org.wordpress.android.fluxc.store.PluginStore;
import org.wordpress.android.fluxc.store.PluginStore.OnWPOrgPluginFetched;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class ReleaseStack_WPOrgPluginTest extends ReleaseStack_Base {
    @Inject PluginStore mPluginStore;

    enum TestEvents {
        NONE,
        WPORG_PLUGIN_FETCHED,
        WPORG_PLUGIN_DOES_NOT_EXIST
    }

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

    public void testFetchWPOrgPlugin() throws InterruptedException {
        String slug = "akismet";
        mNextEvent = TestEvents.WPORG_PLUGIN_FETCHED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));


        WPOrgPluginModel wpOrgPlugin = mPluginStore.getWPOrgPluginBySlug(slug);
        assertNotNull(wpOrgPlugin);
    }

    public void testFetchWPOrgPluginDoesNotExistError() throws InterruptedException {
        String slug = "hello";
        mNextEvent = TestEvents.WPORG_PLUGIN_DOES_NOT_EXIST;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onWPOrgPluginFetched(OnWPOrgPluginFetched event) {
        if (event.isError()) {
            assertEquals(mNextEvent, TestEvents.WPORG_PLUGIN_DOES_NOT_EXIST);
            mCountDownLatch.countDown();
            return;
        }

        assertEquals(TestEvents.WPORG_PLUGIN_FETCHED, mNextEvent);
        mCountDownLatch.countDown();
    }
}
