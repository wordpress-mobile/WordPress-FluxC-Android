package org.wordpress.android.fluxc.mocked;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MockedStack_SiteTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;

    enum TestEvents {
        NONE,
        ELIGIBLE_FOR_AUTOMATED_TRANSFER
    }

    private TestEvents mNextEvent;
    private CountDownLatch mCountDownLatch;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Inject
        mMockedNetworkAppComponent.inject(this);
        // Register
        mDispatcher.register(this);
        // Reset expected test event
        mNextEvent = TestEvents.NONE;
    }

    @Test
    public void testEligibleForAutomatedTransfer() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setSiteId(322);
        mNextEvent = TestEvents.ELIGIBLE_FOR_AUTOMATED_TRANSFER;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferEligibilityAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferEligibilityChecked(OnAutomatedTransferEligibilityChecked event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.ELIGIBLE_FOR_AUTOMATED_TRANSFER);
        assertNotNull(event.site);
        assertTrue(event.site.isEligibleForAutomatedTransfer());
        mCountDownLatch.countDown();
    }
}
