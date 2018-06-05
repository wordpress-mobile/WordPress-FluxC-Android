package org.wordpress.android.fluxc.mocked

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.PluginActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.store.PluginStore
import org.wordpress.android.fluxc.store.PluginStore.OnSitePluginUpdated
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val UPDATE_PLUGIN_NAME = "UPDATE_PLUGIN_NAME"
private const val UPDATE_PLUGIN_SLUG = "UPDATE_PLUGIN_SLUG"

class MockedStack_PluginTest: MockedStack_Base() {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var pluginStore: PluginStore
    @Inject lateinit var interceptor: ResponseMockingInterceptor

    internal enum class TestEvents {
        NONE,
        SITE_PLUGIN_UPDATED
    }

    private lateinit var nextEvent: TestEvents
    private lateinit var countDownLatch: CountDownLatch

    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
    }

    @Test
    fun testUpdateSitePluginSuccess() {
        val site = SiteModel()
        site.setIsWPCom(true)
        site.setIsJetpackConnected(true)
        interceptor.respondWith("update-plugin-success-response.json")
        nextEvent = TestEvents.SITE_PLUGIN_UPDATED
        val payload = UpdateSitePluginPayload(site, UPDATE_PLUGIN_NAME, UPDATE_PLUGIN_SLUG)
        dispatcher.dispatch(PluginActionBuilder.newUpdateSitePluginAction(payload))
        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSitePluginUpdated(event: OnSitePluginUpdated) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.type)
        }
        assertEquals(nextEvent, TestEvents.SITE_PLUGIN_UPDATED)
        assertEquals(event.pluginName, UPDATE_PLUGIN_NAME)
        assertEquals(event.slug, UPDATE_PLUGIN_SLUG)
        countDownLatch.countDown()
    }
}
