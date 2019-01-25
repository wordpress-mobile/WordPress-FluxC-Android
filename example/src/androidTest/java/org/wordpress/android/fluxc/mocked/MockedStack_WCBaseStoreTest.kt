package org.wordpress.android.fluxc.mocked

import android.os.Build
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.action.WCCoreAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchApiVersionResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCSiteSettingsResponsePayload
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_WCBaseStoreTest : MockedStack_Base() {
    @Inject internal lateinit var wcRestClient: WooCommerceRestClient
    @Inject internal lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by notNull()

    private val siteModel = SiteModel().apply {
        id = 5
        siteId = 567
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mMockedNetworkAppComponent.inject(this)
        dispatcher.register(this)
        lastAction = null
    }

    @Test
    fun testApiVersionFetch() {
        interceptor.respondWith("jetpack-tunnel-root-response-success.json")
        wcRestClient.getSupportedWooApiVersion(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCCoreAction.FETCHED_SITE_API_VERSION, lastAction!!.type)
        val payload = lastAction!!.payload as FetchApiVersionResponsePayload
        assertNull(payload.error)
        assertEquals(WooCommerceStore.WOO_API_NAMESPACE_V3, payload.version)
    }

    @Test
    fun testWCSiteSettingsGeneralFetch() {
        interceptor.respondWith("wc-site-settings-general-response-success.json")
        wcRestClient.getSiteSettingsGeneral(siteModel)

        countDownLatch = CountDownLatch(1)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(WCCoreAction.FETCHED_SITE_SETTINGS, lastAction!!.type)
        val payload = lastAction!!.payload as FetchWCSiteSettingsResponsePayload
        assertNull(payload.error)
        with(payload.settings!!) {
            assertEquals(siteModel.id, localSiteId)
            assertEquals("CAD", currencyCode)
            assertEquals(CurrencyPosition.LEFT, currencyPosition)
            assertEquals(",", currencyThousandSeparator)
            assertEquals(".", currencyDecimalSeparator)
            assertEquals(2, currencyDecimalNumber)
        }
    }

    // This is a connected test instead of a unit test because some of the internals of java.util.Currency seem to be
    // stubbed in a unit test environment, giving results inconsistent with a normal running app
    @Test
    fun testGetLocalizedCurrencySymbolForCode() {
        assumeTrue(
                "Requires API 23 or higher due to localized currency values differing on older versions",
                Build.VERSION.SDK_INT >= 23
        )

        Locale("en", "US").let { localeEnUS ->
            assertEquals("$", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("USD", localeEnUS))
            assertEquals("CA$", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("CAD", localeEnUS))
            assertEquals("€", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("EUR", localeEnUS))
            assertEquals("¥", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("JPY", localeEnUS))
        }

        Locale("en", "CA").let { localeEnCA ->
            assertEquals("US$", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("USD", localeEnCA))
            assertEquals("$", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("CAD", localeEnCA))
            assertEquals("€", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("EUR", localeEnCA))
            assertEquals("JP¥", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("JPY", localeEnCA))
        }

        Locale("fr", "FR").let { localeFrFR ->
            assertEquals("\$US", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("USD", localeFrFR))
            assertEquals("\$CA", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("CAD", localeFrFR))
            assertEquals("€", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("EUR", localeFrFR))
            assertEquals("JPY", WCCurrencyUtils.getLocalizedCurrencySymbolForCode("JPY", localeFrFR))
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
