package org.wordpress.android.fluxc.release

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import java.lang.AssertionError
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class ReleaseStack_NotificationTest : ReleaseStack_WPComBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_NOTES,
        MARKED_NOTES_SEEN
    }

    @Inject internal lateinit var notificationStore: NotificationStore

    private var nextEvent: TestEvent = TestEvent.NONE
    private var lastEvent: OnNotificationChanged? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)

        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchNotifications() {
        nextEvent = TestEvent.FETCHED_NOTES
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        Assert.assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val fetchedNotifs = notificationStore.getNotifications().size
        assertTrue(fetchedNotifs > 0 && fetchedNotifs <= NotificationRestClient.NOTIFICATION_DEFAULT_NUMBER)
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        event.error?.let {
            throw AssertionError("OnNotificationChanged as error: ${it.type}")
        }

        lastEvent = event
        when (event.causeOfChange) {
            NotificationAction.FETCH_NOTIFICATIONS -> {
                assertEquals(TestEvent.FETCHED_NOTES, nextEvent)
                mCountDownLatch.countDown()
            }
            NotificationAction.MARK_NOTIFICATIONS_SEEN -> {
                assertEquals(TestEvent.MARKED_NOTES_SEEN, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: ${event.causeOfChange}")
        }
    }
}
