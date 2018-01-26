package org.wordpress.android.fluxc.release;

import junit.framework.Assert;

import org.apache.commons.lang3.RandomStringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.IsAvailableErrorType;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Tests on real servers using the full release stack (no mock)
 */
public class ReleaseStack_AccountAvailabilityTest extends ReleaseStack_Base {
    @Inject AccountStore mAccountStore;

    private enum TestEvents {
        NONE,
        IS_AVAILABLE_BLOG,
        IS_AVAILABLE_DOMAIN,
        IS_AVAILABLE_EMAIL,
        IS_AVAILABLE_USERNAME,
        ERROR_INVALID
    }

    private TestEvents mNextEvent;
    private OnAvailabilityChecked mLastEvent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);

        // Register
        init();
        mNextEvent = TestEvents.NONE;
    }

    public void testIsAvailableBlog() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_BLOG;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableBlogAction("wordpress"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("wordpress", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);

        String unavailableBlog = "docbrown" + RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        mNextEvent = TestEvents.IS_AVAILABLE_BLOG;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableBlogAction(unavailableBlog));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(unavailableBlog, mLastEvent.value);
        Assert.assertTrue(mLastEvent.isAvailable);
    }

    public void testIsAvailableBlogInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableBlogAction("notavalidname#"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("notavalidname#", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);
    }

    public void testIsAvailableDomain() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_DOMAIN;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableDomainAction("docbrown.com"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("docbrown.com", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);
        Assert.assertTrue(mLastEvent.suggestions.size() > 0);

        String unavailableDomain = "docbrown" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + ".com";
        mNextEvent = TestEvents.IS_AVAILABLE_DOMAIN;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableDomainAction(unavailableDomain));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(unavailableDomain, mLastEvent.value);
        Assert.assertTrue(mLastEvent.isAvailable);
        assertNull(mLastEvent.suggestions);
    }

    public void testIsAvailableDomainInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableDomainAction("notavaliddomain#"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("notavaliddomain#", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);
    }

    public void testIsAvailableEmail() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction("mobile@automattic.com"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("mobile@automattic.com", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);

        String unavailableEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        mNextEvent = TestEvents.IS_AVAILABLE_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(unavailableEmail));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(unavailableEmail, mLastEvent.value);
        Assert.assertTrue(mLastEvent.isAvailable);
    }

    public void testIsAvailableEmailInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction("notanemail"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("notanemail", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);
    }

    public void testIsAvailableUsername() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_USERNAME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction("mobile"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("mobile", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);

        String unavailableUsername = "fluxc" + RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        mNextEvent = TestEvents.IS_AVAILABLE_USERNAME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction(unavailableUsername));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals(unavailableUsername, mLastEvent.value);
        Assert.assertTrue(mLastEvent.isAvailable);
    }

    public void testIsAvailableUsernameInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction("invalidusername#"));

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        Assert.assertEquals("invalidusername#", mLastEvent.value);
        Assert.assertFalse(mLastEvent.isAvailable);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        mLastEvent = event;

        if (event.isError()) {
            AppLog.d(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.ERROR_INVALID)) {
                Assert.assertEquals(IsAvailableErrorType.INVALID, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        switch (event.type) {
            case BLOG:
                Assert.assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_BLOG);
                mCountDownLatch.countDown();
                break;
            case DOMAIN:
                Assert.assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_DOMAIN);
                mCountDownLatch.countDown();
                break;
            case EMAIL:
                Assert.assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_EMAIL);
                mCountDownLatch.countDown();
                break;
            case USERNAME:
                Assert.assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_USERNAME);
                mCountDownLatch.countDown();
                break;
        }
    }
}
