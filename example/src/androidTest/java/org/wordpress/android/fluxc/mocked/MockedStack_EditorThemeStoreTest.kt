package org.wordpress.android.fluxc.mocked

import android.os.Bundle
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.EditorThemeActionBuilder
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor.InterceptorMode.STICKY
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
    private lateinit var payloadWithGSS: FetchEditorThemePayload
    private var editorTheme: EditorTheme? = null

    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        editorTheme = null

        site = SiteModel()
        site.setIsWPCom(true)
        site.softwareVersion = "5.8"
        payload = FetchEditorThemePayload(site)
        payloadWithGSS = FetchEditorThemePayload(site, true)
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
        val themeBundle = editorTheme!!.themeSupport.toBundle(site)
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
        val themeBundle = editorTheme!!.themeSupport.toBundle(site)
        assertEmpty(themeBundle)
    }

    @Test
    fun testFetchEditorThemeInvalidFormat() {
        interceptor.respondWith(JsonObject())
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertEmpty(editorTheme)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertEmpty(cachedTheme)
    }

    @Test
    fun testFetchEditorThemeEmptyResultFormat() {
        interceptor.respondWith(JsonArray())
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertEmpty(editorTheme)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertEmpty(cachedTheme)
    }

    @Test
    fun testInvalidFetchEditorThemeSuccess() {
        interceptor.respondWith("editor-theme-invalid-response.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertEmpty(editorTheme)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertEmpty(cachedTheme)

        // Validate Bundle
        val themeBundle = editorTheme!!.themeSupport.toBundle(site)
        assertEmpty(themeBundle)
    }

    @Test
    fun testGlobalStylesSettingsOffSuccess() {
        interceptor.respondWith("global-styles-off-success.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payloadWithGSS))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertNotEmpty(editorTheme)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertNotEmpty(cachedTheme)

        // Validate Bundle
        val themeBundle = editorTheme!!.themeSupport.toBundle(site)
        assertNotEmpty(themeBundle)
    }

    @Test
    fun testGlobalStylesSettingsFullSuccess() {
        interceptor.respondWith("global-styles-full-success.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payloadWithGSS))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // Validate Callback
        assertEmpty(editorTheme)
        Assert.assertNotNull(editorTheme?.themeSupport?.rawStyles)

        // Validate Cache
        val cachedTheme = editorThemeStore.getEditorThemeForSite(site)
        assertEmpty(cachedTheme)
        Assert.assertNotNull(cachedTheme?.themeSupport?.rawStyles)

        // Validate Bundle
        val themeBundle = editorTheme!!.themeSupport.toBundle(site)
        assertEmpty(themeBundle)
        val styles = themeBundle.getString("rawStyles")
        val features = themeBundle.getString("rawFeatures")
        Assert.assertNotNull(styles)
        Assert.assertNotNull(features)
    }

    @Test
    fun testEditorSettingsUrl() {
        val wordPressPayload = payloadWithGSS.apply {
            site.softwareVersion = "5.8"
            site.setIsWPCom(true)
        }
        interceptor.respondWith("global-styles-full-success.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(wordPressPayload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val id = payloadWithGSS.site.siteId
        val expectedUrl = "https://public-api.wordpress.com/wp-block-editor/v1/sites/$id/settings"
        interceptor.assertExpectedUrl(expectedUrl)
    }

    @Test
    fun testEditorSettingsOldUrl() {
        val wordPressPayload = payloadWithGSS.apply {
            site.softwareVersion = "5.7"
            site.setIsWPCom(true)
        }
        interceptor.respondWith("editor-theme-custom-elements-success-response.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(wordPressPayload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val id = payloadWithGSS.site.siteId
        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/$id/themes"
        interceptor.assertExpectedUrl(expectedUrl)
    }

    @Test
    fun testEditorSettingsRetryUrl() {
        val wordPressPayload = payloadWithGSS.apply {
            site.softwareVersion = "5.8"
            site.setIsWPCom(true)
        }
        interceptor.respondWithError(JsonObject(), 404)
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(wordPressPayload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // In case of failure we call the theme endpoint
        val id = payloadWithGSS.site.siteId
        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/$id/themes"
        interceptor.assertExpectedUrl(expectedUrl)
    }

    @Test
    @Ignore("Disabling as a part of effort to exclude flaky or failing tests." +
        "See https://github.com/wordpress-mobile/WordPress-FluxC-Android/pull/2665")
    fun testEditorSettingsOrgUrl() {
        val wordPressPayload = payloadWithGSS.apply {
            site.softwareVersion = "5.8"
            site.url = "https://test.com"
            site.setIsWPCom(false)
        }
        interceptor.respondWith("global-styles-full-success.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(wordPressPayload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))
        val expectedUrl = "https://test.com/wp-json/wp-block-editor/v1/settings"
        interceptor.assertExpectedUrl(expectedUrl)
    }

    @Test
    @Ignore("Disabling as a part of effort to exclude flaky or failing tests." +
        "See https://github.com/wordpress-mobile/WordPress-FluxC-Android/pull/2665")
    fun testEditorSettingsOldOrgUrl() {
        val wordPressPayload = payloadWithGSS.apply {
            site.softwareVersion = "5.7"
            site.url = "https://test.com"
            site.setIsWPCom(false)
        }
        interceptor.respondWith("editor-theme-custom-elements-success-response.json")
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(wordPressPayload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        val expectedUrl = "https://test.com/wp-json/wp/v2/themes"
        interceptor.assertExpectedUrl(expectedUrl)
    }

    @Test
    @Ignore("Disabling as a part of effort to exclude flaky or failing tests." +
        "See https://github.com/wordpress-mobile/WordPress-FluxC-Android/pull/2665")
    fun testEditorSettingsRetryOrgUrl() {
        val wordPressPayload = payloadWithGSS.apply {
            site.softwareVersion = "5.8"
            site.url = "https://test.com"
            site.setIsWPCom(false)
        }
        interceptor.respondWithError(JsonObject(), 404, STICKY)
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(wordPressPayload))

        // See onEditorThemeChanged for the latch's countdown to fire.
        Assert.assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), MILLISECONDS))

        // In case of failure we call the theme endpoint
        val expectedUrl = "https://test.com/wp-json/wp/v2/themes"
        interceptor.assertExpectedUrl(expectedUrl)
    }

    private fun ResponseMockingInterceptor.assertExpectedUrl(expectedUrl: String) =
            Assert.assertTrue(lastRequestUrl.startsWith(expectedUrl))

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
            countDownLatch.countDown()
        }

        editorTheme = event.editorTheme
        countDownLatch.countDown()
    }
}
