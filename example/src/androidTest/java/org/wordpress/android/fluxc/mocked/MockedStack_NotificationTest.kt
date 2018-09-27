package org.wordpress.android.fluxc.mocked

import com.google.gson.JsonObject
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.notifications.NotificationRestClient
import org.wordpress.android.fluxc.store.NotificationStore.NotificationAppKey
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDeviceResponsePayload
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

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
