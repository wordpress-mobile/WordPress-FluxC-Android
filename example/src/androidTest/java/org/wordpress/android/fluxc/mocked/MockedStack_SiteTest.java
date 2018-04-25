package org.wordpress.android.fluxc.mocked;

import android.text.TextUtils;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferInitiated;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferStatusChecked;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MockedStack_SiteTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    @Inject ResponseMockingInterceptor mInterceptor;

    enum TestEvents {
        NONE,
        AUTOMATED_TRANSFER_STATUS_COMPLETE,
        AUTOMATED_TRANSFER_STATUS_INCOMPLETE,
        ELIGIBLE_FOR_AUTOMATED_TRANSFER,
        INITIATE_AUTOMATED_TRANSFER
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
        site.setSiteId(123); // does not matter
        mInterceptor.respondWith("eligible-for-automated-transfer-response-success.json");
        mNextEvent = TestEvents.ELIGIBLE_FOR_AUTOMATED_TRANSFER;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferEligibilityAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testInitiateAutomatedTransferSuccessfully() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setSiteId(123); // does not matter
        mInterceptor.respondWith("initiate-automated-transfer-response-success.json");
        mNextEvent = TestEvents.INITIATE_AUTOMATED_TRANSFER;
        InitiateAutomatedTransferPayload payload = new InitiateAutomatedTransferPayload(site, "react");
        mDispatcher.dispatch(SiteActionBuilder.newInitiateAutomatedTransferAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAutomatedTransferStatusComplete() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setSiteId(123); // does not matter
        mInterceptor.respondWith("automated-transfer-status-complete-response-success.json");
        mNextEvent = TestEvents.AUTOMATED_TRANSFER_STATUS_COMPLETE;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAutomatedTransferStatusIncomplete() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setSiteId(123); // does not matter
        mInterceptor.respondWith("automated-transfer-status-incomplete-response-success.json");
        mNextEvent = TestEvents.AUTOMATED_TRANSFER_STATUS_INCOMPLETE;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(site));
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
        assertTrue(event.isEligible);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferInitiated(OnAutomatedTransferInitiated event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.INITIATE_AUTOMATED_TRANSFER);
        assertNotNull(event.site);
        assertFalse(TextUtils.isEmpty(event.pluginSlugToInstall));
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferStatusChecked(OnAutomatedTransferStatusChecked event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertNotNull(event.site);
        if (mNextEvent.equals(TestEvents.AUTOMATED_TRANSFER_STATUS_COMPLETE)) {
            assertTrue(event.isCompleted);
            // It's not guaranteed that currentStep will be equal to totalSteps
            assertTrue(event.currentStep <= event.totalSteps);
        } else if (mNextEvent.equals(TestEvents.AUTOMATED_TRANSFER_STATUS_INCOMPLETE)) {
            assertFalse(event.isCompleted);
            assertTrue(event.currentStep < event.totalSteps);
        } else {
            throw new AssertionError("Unexpected event occurred: " + mNextEvent);
        }
        mCountDownLatch.countDown();
    }
}
