package org.wordpress.android.fluxc.wc.stats

import com.nhaarman.mockito_kotlin.mock
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCStatsStore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCStatsStoreTest {
    private val wcStatsStore = WCStatsStore(Dispatcher(), mock())

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = SingleStoreWellSqlConfigForTests(appContext, WCOrderStatsModel::class.java,
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        val orderStatsModel = WCStatsTestUtils.generateSampleStatsModel()

        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel)

        with (WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(1, size)
            assertEquals("day", first().unit)
        }

        // Create a second stats entry for this site
        val orderStatsModel2 =
                WCStatsTestUtils.generateSampleStatsModel(unit = "month", fields = "fake-data", data = "fake-data")
        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel2)

        with (WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals("day", first().unit)
            assertEquals("month", get(1).unit)
        }

        // Overwrite an existing entry
        val orderStatsModel3 = WCStatsTestUtils.generateSampleStatsModel()
        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel3)

        with (WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals("day", first().unit)
            assertEquals("month", get(1).unit)
        }

        // Add another "day" entry, but for another site
        val orderStatsModel4 = WCStatsTestUtils.generateSampleStatsModel(localSiteId = 8)
        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel4)

        with (WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals("day", first().unit)
            assertEquals("month", get(1).unit)
            assertEquals("day", get(2).unit)
        }
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
        with (dayOrderStats!!) {
            assertEquals("day", unit)
        }

        val altSiteDayOrderStats = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site2, OrderStatsApiUnit.DAY)
        assertNotNull(altSiteDayOrderStats)

        val monthOrderStatus = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, OrderStatsApiUnit.MONTH)
        assertNotNull(monthOrderStatus)
        with (monthOrderStatus!!) {
            assertEquals("month", unit)
        }

        val nonExistentSite = WCStatsSqlUtils.getRawStatsForSiteAndUnit(
                SiteModel().apply { id = 88 }, OrderStatsApiUnit.DAY)
        assertNull(nonExistentSite)

        val missingData = WCStatsSqlUtils.getRawStatsForSiteAndUnit(site, OrderStatsApiUnit.YEAR)
        assertNull(missingData)
    }

    @Test
    fun testGetRevenueStatsForCurrentMonth() {
        val orderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        val site = SiteModel().apply { id = orderStatsModel.localSiteId }

        WCStatsSqlUtils.insertOrUpdateStats(orderStatsModel)

        val monthStats = wcStatsStore.getRevenueStatsForCurrentMonth(site)

        assertTrue(monthStats.isNotEmpty())
    }
}
