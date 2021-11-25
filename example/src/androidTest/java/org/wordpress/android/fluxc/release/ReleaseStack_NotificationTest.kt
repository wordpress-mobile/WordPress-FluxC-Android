package org.wordpress.android.fluxc.release

import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationHashesResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsSeenPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import kotlin.properties.Delegates

class ReleaseStack_NotificationTest : ReleaseStack_WPComBase() {
    companion object {
        const val BOGUS_HASH = 1L
    }
    internal enum class TestEvent {
        NONE,
        FETCHED_NOTIFS,
        MARKED_NOTIFS_SEEN,
    }

    @Inject internal lateinit var notificationStore: NotificationStore
    @Inject internal lateinit var notificationSqlUtils: NotificationSqlUtils

    private var nextEvent: TestEvent = TestEvent.NONE
    private var lastEvent: OnNotificationChanged? = null
    private var lastAction: Action<*>? = null
    private var actionCountdownLatch: CountDownLatch by Delegates.notNull()
    private var nextAction: NotificationAction? = null
    private val incomingActions: MutableList<Action<*>> = mutableListOf()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)

        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
        this.incomingActions.clear()
        nextAction = null
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchNotifications_basic() {
        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val fetchedNotifs = notificationStore.getNotifications().size
        assertTrue(fetchedNotifs > 0 && fetchedNotifs <= NotificationRestClient.NOTIFICATION_DEFAULT_NUMBER)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchNotifications_noExistingNotifsInDB() {
        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)
        nextAction = NotificationAction.FETCHED_NOTIFICATIONS
        actionCountdownLatch = CountDownLatch(2)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        // Verify expected dispatch actions
        assertTrue(actionCountdownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        assertEquals(lastAction!!.type, nextAction)
        assertEquals(incomingActions[0].type, NotificationAction.FETCH_NOTIFICATIONS)
        assertEquals(incomingActions[1].type, NotificationAction.FETCHED_NOTIFICATIONS)

        val payload = lastAction!!.payload as FetchNotificationsResponsePayload
        assertNotNull(payload)
        assertFalse(payload.isError)
        assertTrue(payload.notifs.isNotEmpty())

        // Now wait for full event completion
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val fetchedNotifs = notificationStore.getNotifications().size
        assertTrue(fetchedNotifs > 0 && fetchedNotifs <= NotificationRestClient.NOTIFICATION_DEFAULT_NUMBER)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchNotifications_synchronizeCachedNotifs() {
        //
        // First, fetch notifications and save to db
        //

        nextAction = null
        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val totalInitialNotifs = notificationStore.getNotifications().size
        assertTrue(totalInitialNotifs > 0 && totalInitialNotifs <= NotificationRestClient.NOTIFICATION_DEFAULT_NUMBER)

        //
        // Second, Split the existing notifications into thirds, forcing an update, insert and ignore when new
        // notifications are sync'd
        //

        // Determine how many notifications can be changed for each of the 3 types to test
        val totalInDb = notificationSqlUtils.getNotificationsCount()
        val chunkLimit = Math.round(totalInDb / 3.0).toInt()

        // Build a list of notification ids expected to need updating
        val updateList = notificationSqlUtils.getNotifications()
                .take(chunkLimit).map {
                    // Change the hash to force an update
                    it.noteHash = BOGUS_HASH

                    // Save to database
                    notificationSqlUtils.insertOrUpdateNotification(it)
                    it.remoteNoteId }

        // Build a list of notification ids expected to be inserted as new
        val newList = notificationSqlUtils.getNotifications()
                .filter { !updateList.contains(it.remoteNoteId) }
                .take(chunkLimit).map {
                    // delete from db
                    notificationSqlUtils.deleteNotificationByRemoteId(it.remoteNoteId)
                    it.remoteNoteId }

        //
        // Third, attempt to fetch notifications again, this time it should route through the
        // syncing with hashes logic
        //

        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)
        nextAction = NotificationAction.FETCHED_NOTIFICATION_HASHES
        actionCountdownLatch = CountDownLatch(2)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        // Wait for our expected next action
        assertTrue(actionCountdownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Verify fetch hashes action and payload
        assertEquals(lastAction!!.type, nextAction)
        assertEquals(incomingActions[0].type, NotificationAction.FETCH_NOTIFICATIONS)
        assertEquals(incomingActions[1].type, NotificationAction.FETCHED_NOTIFICATION_HASHES)
        val payload = lastAction!!.payload as FetchNotificationHashesResponsePayload
        assertNotNull(payload)
        assertFalse(payload.isError)
        assertTrue(payload.hashesMap.isNotEmpty())

        //
        // Finally, wait for full fetch notifications event completion, and verify results
        //

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        assertNotNull(lastEvent)
        assertEquals(lastEvent!!.causeOfChange, NotificationAction.FETCH_NOTIFICATIONS)

        // Fetch fresh list of cached notifications from the db
        val cachedNotifs = notificationStore.getNotifications()
        assertTrue(cachedNotifs.isNotEmpty() && cachedNotifs.size <= NotificationRestClient.NOTIFICATION_DEFAULT_NUMBER)

        // Verify only the notifications we changed or deleted were fetched and inserted into the database.
        assertEquals(newList.size + updateList.size, lastEvent!!.rowsAffected)

        // Verify the initial size of the table is the same after second fetch
        assertEquals(cachedNotifs.size, totalInitialNotifs)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testMarkNotificationsSeen() {
        nextEvent = TestEvent.MARKED_NOTIFS_SEEN
        mCountDownLatch = CountDownLatch(1)

        val lastSeenTime = Date().time
        mDispatcher.dispatch(NotificationActionBuilder
                .newMarkNotificationsSeenAction(MarkNotificationsSeenPayload(lastSeenTime)))

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        assertNotNull(lastEvent?.lastSeenTime)
    }

    @Throws(InterruptedException::class)
    @Test
    fun testMarkNotificationsRead() = runBlocking {
        // First, fetch notifications and store in database.
        nextEvent = TestEvent.FETCHED_NOTIFS
        mCountDownLatch = CountDownLatch(1)

        mDispatcher.dispatch(NotificationActionBuilder
                .newFetchNotificationsAction(FetchNotificationsPayload()))

        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val fetchedNotifs = notificationStore.getNotifications()

        // Second, request up to 3 notifications from the db to set to read
        if (fetchedNotifs.isNotEmpty()) {
            val requestList = fetchedNotifs.take(3)
            val requestListSize = requestList.size
            val result = notificationStore.markNotificationsRead(MarkNotificationsReadPayload(requestList))

            // Verify
            assertNotNull(result)
            assertTrue(result.success)
            assertEquals(result.changedNotificationLocalIds.size, requestListSize)
            with(result.changedNotificationLocalIds) {
                requestList.forEach { assertTrue(contains(it.noteId)) }
            }
        } else {
            throw AssertionError("No notifications fetched to run test with!")
        }
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
                assertEquals(TestEvent.FETCHED_NOTIFS, nextEvent)
                mCountDownLatch.countDown()
            }
            NotificationAction.MARK_NOTIFICATIONS_SEEN -> {
                assertEquals(TestEvent.MARKED_NOTIFS_SEEN, nextEvent)
                mCountDownLatch.countDown()
            }
            else -> throw AssertionError("Unexpected cause of change: ${event.causeOfChange}")
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        if (nextAction != null && action.type is NotificationAction) {
            lastAction = action
            incomingActions.add(action)
            actionCountdownLatch.countDown()
        }
    }
}
