package org.wordpress.android.fluxc.mocked

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NoteIdSet
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.store.NotificationStore.DeviceRegistrationErrorType
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationHashesResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationSeenResponsePayload
import org.wordpress.android.fluxc.store.NotificationStore.NotificationAppKey
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDeviceResponsePayload
import org.wordpress.android.fluxc.store.SiteStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_NotificationTest : MockedStack_Base() {
    @Inject internal lateinit var notificationRestClient: NotificationRestClient
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var siteStore: SiteStore

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by Delegates.notNull()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        lastAction = null
    }

    @Test
    fun testRegistrationWordPress() {
        val responseJson = JsonObject().apply { addProperty("ID", 12345678) }

        interceptor.respondWith(responseJson)

        val gcmToken = "sample-token"
        val uuid = "sample-uuid"
        notificationRestClient.registerDeviceForPushNotifications(gcmToken, NotificationAppKey.WORDPRESS, uuid)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.REGISTERED_DEVICE, lastAction!!.type)

        val requestBodyMap = interceptor.lastRequestBody

        assertEquals(gcmToken, requestBodyMap["device_token"])
        assertEquals("org.wordpress.android", requestBodyMap["app_secret_key"])
        // No site was specified, so the site selection param should be omitted
        assertFalse(requestBodyMap.keys.contains("selected_blog_id"))

        val payload = lastAction!!.payload as RegisterDeviceResponsePayload

        assertEquals(responseJson.get("ID").asString, payload.deviceId)
    }

    @Test
    fun testRegistrationWooCommerce() {
        val responseJson = JsonObject().apply { addProperty("ID", 12345678) }

        interceptor.respondWith(responseJson)

        val gcmToken = "sample-token"
        val uuid = "sample-uuid"
        val site = SiteModel().apply { siteId = 123456 }
        notificationRestClient.registerDeviceForPushNotifications(gcmToken, NotificationAppKey.WOOCOMMERCE, uuid, site)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.REGISTERED_DEVICE, lastAction!!.type)

        val requestBodyMap = interceptor.lastRequestBody

        assertEquals(gcmToken, requestBodyMap["device_token"])
        assertEquals("com.woocommerce.android", requestBodyMap["app_secret_key"])
        assertEquals(site.siteId.toString(), requestBodyMap["selected_blog_id"])

        val payload = lastAction!!.payload as RegisterDeviceResponsePayload

        assertEquals(responseJson.get("ID").asString, payload.deviceId)
    }

    @Test
    fun testRegistrationResponseNull() {
        val errorJson = JsonObject().apply {
            addProperty("error", DeviceRegistrationErrorType.INVALID_RESPONSE.name)
            addProperty("message", "Response object is null")
        }

        interceptor.respondWithError(errorJson)

        val gcmToken = "sample-token"
        val uuid = "sample-uuid"
        val site = SiteModel().apply { siteId = 123456 }
        notificationRestClient.registerDeviceForPushNotifications(gcmToken, NotificationAppKey.WOOCOMMERCE, uuid, site)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.REGISTERED_DEVICE, lastAction!!.type)

        val payload = lastAction!!.payload as RegisterDeviceResponsePayload
        assertNotNull(payload.error)
        assertNull(payload.deviceId)
        assertEquals(DeviceRegistrationErrorType.INVALID_RESPONSE, payload.error.type)
    }

    @Test
    fun testUnregistration() {
        val responseJson = JsonObject().apply { addProperty("success", "true") }

        interceptor.respondWith(responseJson)

        notificationRestClient.unregisterDeviceForPushNotifications("12345678")

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.UNREGISTERED_DEVICE, lastAction!!.type)

        val requestBodyMap = interceptor.lastRequestBody

        assertTrue(requestBodyMap.isEmpty())
    }

    @Test
    fun testFetchNotificationsSuccess() {
        interceptor.respondWith("fetch-notifications-response-success.json")
        notificationRestClient.fetchNotifications()

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.FETCHED_NOTIFICATIONS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchNotificationsResponsePayload

        assertNotNull(payload)
        with(payload.notifs) {
            assertEquals(5, size)
        }
    }

    @Test
    fun testFetchNotificationHashesSuccess() {
        interceptor.respondWith("fetch-notification-hashes-response-success.json")
        notificationRestClient.fetchNotificationHashes()

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.FETCHED_NOTIFICATION_HASHES, lastAction!!.type)
        val payload = lastAction!!.payload as FetchNotificationHashesResponsePayload

        assertNotNull(payload)
        with(payload.hashesMap) {
            assertEquals(5, size)
        }
    }

    @Test
    fun testFetchNotificationSuccess() {
        val remoteNoteId = 3695324025L
        val remoteSiteId = 153482281L

        interceptor.respondWith("fetch-notification-response-success.json")
        notificationRestClient.fetchNotification(remoteNoteId)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.FETCHED_NOTIFICATION, lastAction!!.type)
        val payload = lastAction!!.payload as FetchNotificationResponsePayload

        assertNotNull(payload)
        assertNotNull(payload.notification)
        with(payload) {
            assertEquals(notification!!.remoteNoteId, remoteNoteId)
            assertEquals(notification!!.remoteSiteId, remoteSiteId)
        }
    }

    @Test
    fun testMarkNotificationSeenSuccess() {
        interceptor.respondWith("mark-notification-seen-response-success.json")
        notificationRestClient.markNotificationsSeen(1543265347)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(NotificationAction.MARKED_NOTIFICATIONS_SEEN, lastAction!!.type)
        val payload = lastAction!!.payload as MarkNotificationSeenResponsePayload

        assertNotNull(payload)
        assertEquals(payload.lastSeenTime, 1543265347L)
    }

    @Test
    fun testMarkSingleNotificationReadSuccess() = runBlocking {
        val testNoteIdSet = NoteIdSet(0, 22L, 2L)

        interceptor.respondWith("mark-notification-read-response-success.json")
        val result = notificationRestClient.markNotificationRead(
                listOf(NotificationModel(
                        remoteNoteId = testNoteIdSet.remoteNoteId,
                        remoteSiteId = testNoteIdSet.remoteSiteId)))

        assertNotNull(result)
        assertEquals(result.success, true)
        assertNotNull(result.notifications)
        assertEquals(1, result.notifications!!.size)
        with(result.notifications!![0]) {
            assertEquals(remoteNoteId, testNoteIdSet.remoteNoteId)
        }
    }

    @Test
    fun testMarkMultipleNotificationsReadSuccess() = runBlocking {
        val testNoteIdSet1 = NoteIdSet(0, 22L, 2L)
        val testNoteIdSet2 = NoteIdSet(0, 33L, 3L)
        val testNoteIdSet3 = NoteIdSet(0, 44L, 4L)

        interceptor.respondWith("mark-notification-read-response-success.json")
        val result = notificationRestClient.markNotificationRead(listOf(
                NotificationModel(
                        remoteNoteId = testNoteIdSet1.remoteNoteId,
                        remoteSiteId = testNoteIdSet1.remoteSiteId),
                NotificationModel(
                        remoteNoteId = testNoteIdSet2.remoteNoteId,
                        remoteSiteId = testNoteIdSet2.remoteSiteId),
                NotificationModel(
                        remoteNoteId = testNoteIdSet3.remoteNoteId,
                        remoteSiteId = testNoteIdSet3.remoteSiteId)))

        assertNotNull(result)
        assertEquals(result.success, true)
        assertNotNull(result.notifications)
        assertEquals(3, result.notifications!!.size)
        with(result.notifications!![0]) {
            assertEquals(remoteNoteId, testNoteIdSet1.remoteNoteId)
            assertEquals(remoteSiteId, testNoteIdSet1.remoteSiteId)
        }
        with(result.notifications!![1]) {
            assertEquals(remoteNoteId, testNoteIdSet2.remoteNoteId)
            assertEquals(remoteSiteId, testNoteIdSet2.remoteSiteId)
        }
        with(result.notifications!![2]) {
            assertEquals(remoteNoteId, testNoteIdSet3.remoteNoteId)
            assertEquals(remoteSiteId, testNoteIdSet3.remoteSiteId)
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
