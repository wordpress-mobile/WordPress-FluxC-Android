package org.wordpress.android.fluxc.mocked

import android.os.Bundle
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.EditorThemeActionBuilder
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.EditorThemeStore.FetchEditorThemePayload
import org.wordpress.android.fluxc.store.EditorThemeStore.OnEditorThemeChanged
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class MockedStack_EditorThemeStoreTest : MockedStack_Base() {
    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var interceptor: ResponseMockingInterceptor

    @Inject lateinit var editorThemeStore: EditorThemeStore
    private lateinit var countDownLatch: CountDownLatch
    private lateinit var site: SiteModel
    private lateinit var payload: FetchEditorThemePayload
    private var editorTheme: EditorTheme? = null

    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        editorTheme = null

        site = SiteModel()
        site.setIsWPCom(true)
        payload = FetchEditorThemePayload(site)
        countDownLatch = CountDownLatch(1)
    }

    @Test
    fun testFetchEditorThemeSuccess() {
        interceptor.respondWith("editor-theme-custom-elements-success-response.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertNotEmpty(editorTheme)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertNotEmpty(cachedTheme)

        // Validate Bundle
        val themeBundle = editorTheme!!.themeSupport.toBundle()
        assertNotEmpty(themeBundle)
    }

    @Test
    fun testUnsupportedFetchEditorThemeSuccess() {
        interceptor.respondWith("editor-theme-unsupported-response.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertEmpty(editorTheme)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertEmpty(cachedTheme)

        // Validate Bundle
        val themeBundle = editorTheme!!.themeSupport.toBundle()
        assertEmpty(themeBundle)
    }

    private fun assertNotEmpty(theme: EditorTheme?) {
        Assert.assertFalse(theme?.themeSupport?.colors.isNullOrEmpty())
        Assert.assertFalse(theme?.themeSupport?.gradients.isNullOrEmpty())
    }

    private fun assertEmpty(theme: EditorTheme?) {
        Assert.assertTrue(theme?.themeSupport?.colors == null)
        Assert.assertTrue(theme?.themeSupport?.gradients == null)
    }

    private fun assertEmpty(theme: Bundle) {
        val colors = theme.getSerializable("colors")
        val gradients = theme.getSerializable("gradients")
        Assert.assertTrue(colors == null)
        Assert.assertTrue(gradients == null)
    }

    private fun assertNotEmpty(theme: Bundle) {
        val colors = theme.getSerializable("colors") as ArrayList<*>
        val gradients = theme.getSerializable("gradients") as ArrayList<*>
        Assert.assertFalse(colors.isNullOrEmpty())
        Assert.assertFalse(gradients.isNullOrEmpty())
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onEditorThemeChanged(event: OnEditorThemeChanged) {
        if (event.isError) {
            throw AssertionError("Unexpected error occurred with type: " + event.error.message)
        }

        editorTheme = event.editorTheme
        countDownLatch.countDown()
    }
}
