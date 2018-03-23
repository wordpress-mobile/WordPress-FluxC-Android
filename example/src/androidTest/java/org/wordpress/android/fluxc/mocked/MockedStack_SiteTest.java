package org.wordpress.android.fluxc.mocked;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.InitiateAutomatedTransferPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferInitiated;
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferStatusChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MockedStack_SiteTest extends MockedStack_Base {
    // from "initiate-automated-transfer-response-success.json"
    private static final int TEST_INITIATE_AUTOMATED_TRANSFER_ID = 197364;

    // Used in ResponseMockingInterceptor
    public static final int TEST_SITE_ID_TRANSFER_COMPLETE = 123;
    public static final int TEST_SITE_ID_TRANSFER_INCOMPLETE = 124;

    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TestEvents {
        NONE,
        ACCOUNT_CHANGED,
        AUTOMATED_TRANSFER_STATUS_COMPLETE,
        AUTOMATED_TRANSFER_STATUS_INCOMPLETE,
        ELIGIBLE_FOR_AUTOMATED_TRANSFER,
        INITIATE_AUTOMATED_TRANSFER,
        REMOVE_SITE,
        UPDATE_SITE
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
        SiteModel site = createAccountAndLocalTestSite(false);

        mNextEvent = TestEvents.ELIGIBLE_FOR_AUTOMATED_TRANSFER;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferEligibilityAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        SiteModel updatedSite = mSiteStore.getSiteBySiteId(site.getSiteId());
        assertTrue(updatedSite.isEligibleForAutomatedTransfer());

        removeSiteAndSignOut(updatedSite);
    }

    @Test
    public void testInitiateAutomatedTransferSuccessfully() throws InterruptedException {
        SiteModel site = createAccountAndLocalTestSite(true);

        mNextEvent = TestEvents.INITIATE_AUTOMATED_TRANSFER;
        InitiateAutomatedTransferPayload payload = new InitiateAutomatedTransferPayload(site, "react");
        mDispatcher.dispatch(SiteActionBuilder.newInitiateAutomatedTransferAction(payload));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        SiteModel updatedSite = mSiteStore.getSiteBySiteId(site.getSiteId());
        assertEquals(updatedSite.getAutomatedTransferId(), TEST_INITIATE_AUTOMATED_TRANSFER_ID);

        removeSiteAndSignOut(updatedSite);
    }

    @Test
    public void testAutomatedTransferStatusComplete() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setSiteId(TEST_SITE_ID_TRANSFER_COMPLETE);
        mNextEvent = TestEvents.AUTOMATED_TRANSFER_STATUS_COMPLETE;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAutomatedTransferStatusIncomplete() throws InterruptedException {
        SiteModel site = new SiteModel();
        site.setSiteId(TEST_SITE_ID_TRANSFER_INCOMPLETE);
        mNextEvent = TestEvents.AUTOMATED_TRANSFER_STATUS_INCOMPLETE;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferStatusAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAccountChanged(OnAccountChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.ACCOUNT_CHANGED);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAutomatedTransferEligibilityChecked(OnAutomatedTransferEligibilityChecked event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.ELIGIBLE_FOR_AUTOMATED_TRANSFER);
        assertNotNull(event.site);
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
        } else if (mNextEvent.equals(TestEvents.AUTOMATED_TRANSFER_STATUS_INCOMPLETE)) {
            assertFalse(event.isCompleted);
        } else {
            throw new AssertionError("Unexpected event occurred: " + mNextEvent);
        }
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteChanged(OnSiteChanged event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.UPDATE_SITE);
        assertEquals(event.rowsAffected, 1);
        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onSiteRemoved(OnSiteRemoved event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: " + event.error.type);
        }
        assertEquals(mNextEvent, TestEvents.REMOVE_SITE);
        assertEquals(event.mRowsAffected, 1);
        mCountDownLatch.countDown();
    }

    private @NonNull SiteModel createAccountAndLocalTestSite(boolean isEligible) throws InterruptedException {
        AccountModel account = new AccountModel();
        account.setUserId(478);
        mNextEvent = TestEvents.ACCOUNT_CHANGED;
        mDispatcher.dispatch(AccountActionBuilder.newUpdateAccountAction(account));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        SiteModel site = new SiteModel();
        site.setSiteId(322);
        site.setIsWPCom(true);
        site.setIsEligibleForAutomatedTransfer(isEligible);
        mNextEvent = TestEvents.UPDATE_SITE;
        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        SiteModel siteFromStore = mSiteStore.getSiteBySiteId(site.getSiteId());
        assertNotNull(siteFromStore);
        return siteFromStore;
    }

    private void removeSiteAndSignOut(SiteModel site) throws InterruptedException {
        mNextEvent = TestEvents.REMOVE_SITE;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(site));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(mSiteStore.getSites().size(), 0);

        mNextEvent = TestEvents.ACCOUNT_CHANGED;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newSignOutAction());
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }
}
