package org.wordpress.android.fluxc.mocked

import com.yarolegovich.wellsql.WellSql
import org.greenrobot.eventbus.Subscribe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils.WCSettingsBuilder
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.util.Locale
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import kotlin.properties.Delegates.notNull

/**
 * Tests using a Mocked Network app component. Test the network client itself and not the underlying
 * network component(s).
 */
class MockedStack_WCBaseStoreTest : MockedStack_Base() {
    @Inject internal lateinit var wcRestClient: WooCommerceRestClient
    @Inject internal lateinit var wooCommerceStore: WooCommerceStore
    @Inject internal lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var interceptor: ResponseMockingInterceptor

    private var lastAction: Action<*>? = null
    private var countDownLatch: CountDownLatch by notNull()

    private val siteModel = SiteModel().apply {
        origin = SiteModel.ORIGIN_WPCOM_REST
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

    // This is a connected test instead of a unit test because some of the internals of java.util.Currency seem to be
    // stubbed in a unit test environment, giving results inconsistent with a normal running app
    @Test
    fun testGetLocalizedCurrencySymbolForCode() {
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

    @Test
    fun testGetSiteCurrency() {
        // Override device locale and use en_US so currency symbols can be predicted
        TestUtils.updateLocale(mAppContext, Locale("en", "US"))

        // -- Site using CAD
        val cadSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                couponsEnabled = true)
        WCSettingsSqlUtils.insertOrUpdateSettings(cadSettings)

        with(wooCommerceStore) {
            val formattedCurrencySymbol = getSiteCurrency(siteModel, "CAD")
            assertEquals("CA$", formattedCurrencySymbol)

            val formattedCurrencyUseSiteCurrency = getSiteCurrency(siteModel, null)
            assertEquals("CA$", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = getSiteCurrency(siteModel, "EUR")
            assertEquals("€", formattedCurrencyDifferentCurrency)

            val formattedCurrencyEmptyCodeUseSite = getSiteCurrency(siteModel, "")
            assertEquals("CA$", formattedCurrencyEmptyCodeUseSite)
        }

        // -- Site using EUR
        val eurSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "EUR",
                currencyPosition = CurrencyPosition.RIGHT_SPACE,
                currencyThousandSeparator = ".",
                currencyDecimalSeparator = ",",
                currencyDecimalNumber = 2,
                couponsEnabled = true)
        WCSettingsSqlUtils.insertOrUpdateSettings(eurSettings)

        with(wooCommerceStore) {
            val formattedCurrencyDouble = getSiteCurrency(siteModel, "EUR")
            assertEquals("€", formattedCurrencyDouble)

            val formattedCurrencyUseSiteCurrency = getSiteCurrency(siteModel, null)
            assertEquals("€", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = getSiteCurrency(siteModel, "USD")
            assertEquals("$", formattedCurrencyDifferentCurrency)
        }

        // -- Site using JPY
        val jpySettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "JPY",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = "",
                currencyDecimalSeparator = "",
                currencyDecimalNumber = 0,
                couponsEnabled = true)
        WCSettingsSqlUtils.insertOrUpdateSettings(jpySettings)

        with(wooCommerceStore) {
            val formattedCurrencyDouble = getSiteCurrency(siteModel, "JPY")
            assertEquals("¥", formattedCurrencyDouble)

            val formattedCurrencyUseSiteCurrency = getSiteCurrency(siteModel, null)
            assertEquals("¥", formattedCurrencyUseSiteCurrency)
        }

        // -- No site settings stored
        WellSql.delete(WCSettingsBuilder::class.java).execute()

        with(wooCommerceStore) {
            val formattedCurrencyDouble = getSiteCurrency(siteModel, "CAD")
            assertEquals("CA$", formattedCurrencyDouble)

            val formattedCurrencyUseSiteCurrency = getSiteCurrency(siteModel, null)
            assertEquals("", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = getSiteCurrency(siteModel, "INR")
            assertEquals("₹", formattedCurrencyDifferentCurrency)
        }
    }

    @Test
    fun testFormatCurrencyForDisplay() {
        // Override device locale and use en_US so currency symbols can be predicted
        TestUtils.updateLocale(mAppContext, Locale("en", "US"))

        // -- Site using CAD
        val cadSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                couponsEnabled = true)
        WCSettingsSqlUtils.insertOrUpdateSettings(cadSettings)

        with(wooCommerceStore) {
            val formattedCurrencyDouble = formatCurrencyForDisplay(1234.12, siteModel, "CAD", true)
            assertEquals("CA$1,234.12", formattedCurrencyDouble)

            val formattedCurrencyString = formatCurrencyForDisplay("1234.12", siteModel, "CAD", true)
            assertEquals("CA$1,234.12", formattedCurrencyString)

            val formattedCurrencyPretty = formatCurrencyForDisplay("1.2k", siteModel, "CAD", false)
            assertEquals("CA$1.2k", formattedCurrencyPretty)

            val formattedCurrencyNegative = formatCurrencyForDisplay(-1234.12, siteModel, "CAD", true)
            assertEquals("-CA$1,234.12", formattedCurrencyNegative)

            val formattedCurrencyUseSiteCurrency = formatCurrencyForDisplay(1234.12, siteModel, null, true)
            assertEquals("CA$1,234.12", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = formatCurrencyForDisplay(1234.12, siteModel, "EUR", true)
            assertEquals("€1,234.12", formattedCurrencyDifferentCurrency)

            val formattedCurrencyEmptyCodeUseSite = formatCurrencyForDisplay(1234.12, siteModel, "", true)
            assertEquals("CA$1,234.12", formattedCurrencyUseSiteCurrency)
        }

        // -- Site using EUR
        val eurSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "EUR",
                currencyPosition = CurrencyPosition.RIGHT_SPACE,
                currencyThousandSeparator = ".",
                currencyDecimalSeparator = ",",
                currencyDecimalNumber = 2,
                couponsEnabled = true)
        WCSettingsSqlUtils.insertOrUpdateSettings(eurSettings)

        with(wooCommerceStore) {
            val formattedCurrencyDouble = formatCurrencyForDisplay(1234.12, siteModel, "EUR", true)
            assertEquals("1.234,12 €", formattedCurrencyDouble)

            val formattedCurrencyString = formatCurrencyForDisplay("1234.12", siteModel, "EUR", true)
            assertEquals("1.234,12 €", formattedCurrencyString)

            val formattedCurrencyPretty = formatCurrencyForDisplay("1.2k", siteModel, "EUR", false)
            assertEquals("1.2k €", formattedCurrencyPretty)

            val formattedCurrencyNegative = formatCurrencyForDisplay(-1234.12, siteModel, "EUR", true)
            assertEquals("-1.234,12 €", formattedCurrencyNegative)

            val formattedCurrencyUseSiteCurrency = formatCurrencyForDisplay(1234.12, siteModel, null, true)
            assertEquals("1.234,12 €", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = formatCurrencyForDisplay(1234.12, siteModel, "USD", true)
            assertEquals("1.234,12 \$", formattedCurrencyDifferentCurrency)
        }

        // -- Site using JPY
        val jpySettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "JPY",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = "",
                currencyDecimalSeparator = "",
                currencyDecimalNumber = 0,
                couponsEnabled = true)
        WCSettingsSqlUtils.insertOrUpdateSettings(jpySettings)

        with(wooCommerceStore) {
            val formattedCurrencyDouble = formatCurrencyForDisplay(1234.0, siteModel, "JPY", true)
            assertEquals("¥1234", formattedCurrencyDouble)

            val formattedCurrencyString = formatCurrencyForDisplay("1234", siteModel, "JPY", true)
            assertEquals("¥1234", formattedCurrencyString)

            val formattedCurrencyPretty = formatCurrencyForDisplay("1.2k", siteModel, "JPY", false)
            assertEquals("¥1.2k", formattedCurrencyPretty)

            val formattedCurrencyNegative = formatCurrencyForDisplay(-1234.12, siteModel, "JPY", true)
            assertEquals("-¥1234", formattedCurrencyNegative)

            val formattedCurrencyUseSiteCurrency = formatCurrencyForDisplay(1234.0, siteModel, null, true)
            assertEquals("¥1234", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = formatCurrencyForDisplay(1234.0, siteModel, "USD", true)
            assertEquals("$1234", formattedCurrencyDifferentCurrency)
        }

        // -- No site settings stored
        WellSql.delete(WCSettingsBuilder::class.java).execute()

        with(wooCommerceStore) {
            val formattedCurrencyDouble = formatCurrencyForDisplay(1234.12, siteModel, "CAD", true)
            assertEquals("CA$1234.12", formattedCurrencyDouble)

            val formattedCurrencyString = formatCurrencyForDisplay("1234.12", siteModel, "CAD", true)
            assertEquals("CA$1234.12", formattedCurrencyString)

            val formattedCurrencyPretty = formatCurrencyForDisplay("1.2k", siteModel, "CAD", false)
            assertEquals("CA$1.2k", formattedCurrencyPretty)

            val formattedCurrencyNegative = formatCurrencyForDisplay(-1234.12, siteModel, "CAD", true)
            assertEquals("-CA$1234.12", formattedCurrencyNegative)

            val formattedCurrencyUseSiteCurrency = formatCurrencyForDisplay(1234.12, siteModel, null, true)
            assertEquals("1234.12", formattedCurrencyUseSiteCurrency)

            val formattedCurrencyDifferentCurrency = formatCurrencyForDisplay(1234.12, siteModel, "EUR", true)
            assertEquals("€1234.12", formattedCurrencyDifferentCurrency)
        }
    }

    @Test
    fun testGetStoreCountryBySite() {
        // -- Site using CAD
        val cadSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                countryCode = "CA:QC",
                couponsEnabled = true
        )
        WCSettingsSqlUtils.insertOrUpdateSettings(cadSettings)

        with(wooCommerceStore) {
            assertEquals("CA:QC", getStoreCountryCode(siteModel))
        }

        // -- No site settings stored
        WellSql.delete(WCSettingsBuilder::class.java).execute()
        with(wooCommerceStore) {
            assertNull(getStoreCountryCode(siteModel))
        }

        // -- no country code in settings
        val siteSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                couponsEnabled = true
        )
        WCSettingsSqlUtils.insertOrUpdateSettings(siteSettings)

        with(wooCommerceStore) {
            assertEquals("", getStoreCountryCode(siteModel))
        }

        // -- Site without city i.e. "US"
        val usSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                countryCode = "US",
                couponsEnabled = true
        )
        WCSettingsSqlUtils.insertOrUpdateSettings(usSettings)

        with(wooCommerceStore) {
            assertEquals("US", getStoreCountryCode(siteModel))
        }

        // -- Site using IND
        val indSettings = WCSettingsModel(
                localSiteId = siteModel.id,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                countryCode = "IN:TN",
                couponsEnabled = true
        )
        WCSettingsSqlUtils.insertOrUpdateSettings(indSettings)

        with(wooCommerceStore) {
            assertEquals("IN:TN", getStoreCountryCode(siteModel))
        }
    }

    @Suppress("unused")
    @Subscribe
    fun onAction(action: Action<*>) {
        lastAction = action
        countDownLatch.countDown()
    }
}
