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
import org.wordpress.android.fluxc.store.SiteStore.OnAutomatedTransferEligibilityChecked;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MockedStack_SiteTest extends MockedStack_Base {
    @Inject Dispatcher mDispatcher;
    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    enum TestEvents {
        NONE,
        ACCOUNT_CHANGED,
        ELIGIBLE_FOR_AUTOMATED_TRANSFER,
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
        // Create dummy site
        SiteModel site = createAccountAndLocalTestSite();

        mNextEvent = TestEvents.ELIGIBLE_FOR_AUTOMATED_TRANSFER;
        mDispatcher.dispatch(SiteActionBuilder.newCheckAutomatedTransferEligibilityAction(site));
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
        assertTrue(event.site.isEligibleForAutomatedTransfer());
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

    private @NonNull SiteModel createAccountAndLocalTestSite() throws InterruptedException {
        AccountModel account = new AccountModel();
        account.setUserId(478);
        mNextEvent = TestEvents.ACCOUNT_CHANGED;
        mDispatcher.dispatch(AccountActionBuilder.newUpdateAccountAction(account));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        SiteModel site = new SiteModel();
        site.setSiteId(322);
        site.setIsWPCom(true);
        mNextEvent = TestEvents.UPDATE_SITE;
        mDispatcher.dispatch(SiteActionBuilder.newUpdateSiteAction(site));
        mCountDownLatch = new CountDownLatch(1);
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        SiteModel siteFromStore = mSiteStore.getSiteBySiteId(site.getSiteId());
        assertNotNull(siteFromStore);
        return siteFromStore;
    }
}
