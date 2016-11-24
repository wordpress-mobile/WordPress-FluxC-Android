package org.wordpress.android.fluxc.release;

import org.apache.commons.lang.RandomStringUtils;
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

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("wordpress", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);

        String unavailableBlog = "docbrown" + RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        mNextEvent = TestEvents.IS_AVAILABLE_BLOG;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableBlogAction(unavailableBlog));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(unavailableBlog, mLastEvent.value);
        assertTrue(mLastEvent.isAvailable);
    }

    public void testIsAvailableBlogInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableBlogAction("notavalidname#"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("notavalidname#", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);
    }

    public void testIsAvailableDomain() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_DOMAIN;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableDomainAction("docbrown.com"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("docbrown.com", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);
        assertTrue(mLastEvent.suggestions.size() > 0);

        String unavailableDomain = "docbrown" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + ".com";
        mNextEvent = TestEvents.IS_AVAILABLE_DOMAIN;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableDomainAction(unavailableDomain));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(unavailableDomain, mLastEvent.value);
        assertTrue(mLastEvent.isAvailable);
        assertNull(mLastEvent.suggestions);
    }

    public void testIsAvailableDomainInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableDomainAction("notavaliddomain#"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("notavaliddomain#", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);
    }

    public void testIsAvailableEmail() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction("mobile@automattic.com"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("mobile@automattic.com", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);

        String unavailableEmail = "marty" + RandomStringUtils.randomAlphanumeric(8).toLowerCase() + "@themacflys.com";
        mNextEvent = TestEvents.IS_AVAILABLE_EMAIL;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(unavailableEmail));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(unavailableEmail, mLastEvent.value);
        assertTrue(mLastEvent.isAvailable);
    }

    public void testIsAvailableEmailInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction("notanemail"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("notanemail", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);
    }

    public void testIsAvailableUsername() throws InterruptedException {
        mNextEvent = TestEvents.IS_AVAILABLE_USERNAME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction("mobile"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("mobile", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);

        String unavailableUsername = "fluxc" + RandomStringUtils.randomAlphanumeric(8).toLowerCase();
        mNextEvent = TestEvents.IS_AVAILABLE_USERNAME;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction(unavailableUsername));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(unavailableUsername, mLastEvent.value);
        assertTrue(mLastEvent.isAvailable);
    }

    public void testIsAvailableUsernameInvalid() throws InterruptedException {
        mNextEvent = TestEvents.ERROR_INVALID;
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(AccountActionBuilder.newIsAvailableUsernameAction("invalidusername#"));

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertEquals("invalidusername#", mLastEvent.value);
        assertFalse(mLastEvent.isAvailable);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        mLastEvent = event;

        if (event.isError()) {
            AppLog.d(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
            if (mNextEvent.equals(TestEvents.ERROR_INVALID)) {
                assertEquals(IsAvailableErrorType.INVALID, event.error.type);
                mCountDownLatch.countDown();
            }
            return;
        }
        switch (event.type) {
            case BLOG:
                assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_BLOG);
                mCountDownLatch.countDown();
                break;
            case DOMAIN:
                assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_DOMAIN);
                mCountDownLatch.countDown();
                break;
            case EMAIL:
                assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_EMAIL);
                mCountDownLatch.countDown();
                break;
            case USERNAME:
                assertEquals(mNextEvent, TestEvents.IS_AVAILABLE_USERNAME);
                mCountDownLatch.countDown();
                break;
        }
    }
}
