package org.wordpress.android.fluxc.wc.stats

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.yarolegovich.wellsql.WellSql
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.SiteUtils.getCurrentDateTimeForSite
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.hamcrest.CoreMatchers.`is` as isEqual

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCStatsStoreTest {
    private val mockOrderStatsRestClient = mock<OrderStatsRestClient>()
    private val wcStatsStore = WCStatsStore(Dispatcher(), mockOrderStatsRestClient)

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = SingleStoreWellSqlConfigForTests(
                appContext, WCOrderStatsModel::class.java,
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        val orderStatsModel = WCStatsTestUtils.generateSampleStatsModel()

        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(1, size)
            assertEquals("day", first().unit)
        }

        // Create a second stats entry for this site
        val orderStatsModel2 =
                WCStatsTestUtils.generateSampleStatsModel(unit = "month", fields = "fake-data", data = "fake-data")
        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel2)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals("day", first().unit)
            assertEquals("month", get(1).unit)
        }

        // Overwrite an existing entry
        val orderStatsModel3 = WCStatsTestUtils.generateSampleStatsModel()
        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel3)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals("day", first().unit)
            assertEquals("month", get(1).unit)
        }

        // Add another "day" entry, but for another site
        val orderStatsModel4 = WCStatsTestUtils.generateSampleStatsModel(localSiteId = 8)
        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel4)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals("day", first().unit)
            assertEquals("month", get(1).unit)
            assertEquals("day", get(2).unit)
        }
    }

    @Test
    fun testFetchDayOrderStatsDate() {
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        // The two test sites are 24 hours apart, so we are guaranteed to have one site date match the local date,
        // and the other not match it
        val localDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
        assertThat(localDate, anyOf(isEqual(plus12SiteDate), isEqual(minus12SiteDate)))
        assertThat(localDate, anyOf(not(plus12SiteDate), not(minus12SiteDate)))
    }

    @Test
    fun testGetRawStatsForSiteAndUnit() {
        val dayOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        val site = SiteModel().apply { id = dayOrderStatsModel.localSiteId }
        val monthOrderStatsModel =
                WCStatsTestUtils.generateSampleStatsModel(unit = "month", fields = "fake-data", data = "fake-data")
        WCStatsSqlUtils.insertOrUpdateStats(dayOrderStatsModel)
        WCStatsSqlUtils.insertOrUpdateStats(monthOrderStatsModel)

        val site2 = SiteModel().apply { id = 8 }
        val altSiteOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel(localSiteId = site2.id)
        WCStatsSqlUtils.insertOrUpdateStats(altSiteOrderStatsModel)

        val dayOrderStats = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, OrderStatsApiUnit.DAY)
        assertNotNull(dayOrderStats)
        with(dayOrderStats!!) {
            assertEquals("day", unit)
        }

        val altSiteDayOrderStats = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site2, OrderStatsApiUnit.DAY)
        assertNotNull(altSiteDayOrderStats)

        val monthOrderStatus = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, OrderStatsApiUnit.MONTH)
        assertNotNull(monthOrderStatus)
        with(monthOrderStatus!!) {
            assertEquals("month", unit)
        }

        val nonExistentSite = WCStatsSqlUtils.getRawStatsForSiteAndUnit(
                SiteModel().apply { id = 88 }, OrderStatsApiUnit.DAY
        )
        assertNull(nonExistentSite)

        val missingData = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, OrderStatsApiUnit.YEAR)
        assertNull(missingData)
    }

    @Test
    fun testGetStatsForDaysGranularity() {
        val orderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        val site = SiteModel().apply { id = orderStatsModel.localSiteId }

        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel)

        val revenueStats = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS)
        val orderStats = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS)

        assertTrue(revenueStats.isNotEmpty())
        assertTrue(orderStats.isNotEmpty())
        assertEquals(revenueStats.size, orderStats.size)
        assertEquals(revenueStats.keys, orderStats.keys)
    }

    @Test
    fun testGetStatsCurrencyForSite() {
        val orderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        val site = SiteModel().apply { id = orderStatsModel.localSiteId }

        assertNull(wcStatsStore.getStatsCurrencyForSite(site))

        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel)

        assertEquals("USD", wcStatsStore.getStatsCurrencyForSite(site))
    }
}
