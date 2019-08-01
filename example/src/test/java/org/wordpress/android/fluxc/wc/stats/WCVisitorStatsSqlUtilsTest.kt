package org.wordpress.android.fluxc.wc.stats

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCVisitorStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCVisitorStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCVisitorStatsSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCVisitorStatsModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testSimpleInsertionAndRetrievalOfVisitorStats() {
        // insert a visitor stats entry and verify that it is stored correctly
        val visitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel()
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(visitorStatsModel)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(1, size)
            assertEquals(visitorStatsModel.unit, first().unit)
            assertEquals(visitorStatsModel.startDate, first().startDate)
            assertEquals(visitorStatsModel.endDate, first().endDate)
        }

        // Create a second visitor stats entry for this site with same quantity but with different unit
        val visitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                        unit = OrderStatsApiUnit.MONTH.toString(), quantity = "12", data = "fake-data"
                )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(visitorStatsModel2)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals(visitorStatsModel.unit, first().unit)
            assertEquals(visitorStatsModel.startDate, first().startDate)
            assertEquals(visitorStatsModel.endDate, first().endDate)
            assertEquals(visitorStatsModel2.unit, get(1).unit)
            assertEquals(visitorStatsModel2.startDate, get(1).startDate)
            assertEquals(visitorStatsModel2.endDate, get(1).endDate)
        }

        // Create a third stats entry for this site with same interval but different start & end date
        // i.e. custom stats
        val visitorStatsModel3 =
                WCStatsTestUtils.generateSampleVisitorStatsModel(
                        data = "fake-data2", startDate = "2019-07-07", endDate = "2019-07-13"
                )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(visitorStatsModel3)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals(visitorStatsModel.unit, first().unit)
            assertEquals(visitorStatsModel.startDate, first().startDate)
            assertEquals(visitorStatsModel.endDate, first().endDate)
            assertEquals(visitorStatsModel2.unit, get(1).unit)
            assertEquals(visitorStatsModel2.startDate, get(1).startDate)
            assertEquals(visitorStatsModel2.endDate, get(1).endDate)
            assertEquals(visitorStatsModel3.unit, get(2).unit)
            assertEquals(visitorStatsModel3.startDate, get(2).startDate)
            assertEquals(visitorStatsModel3.endDate, get(2).endDate)
        }

        // Overwrite an existing entry and verify that update is happening correctly
        val visitorStatsModel4 = WCStatsTestUtils.generateSampleVisitorStatsModel()
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(visitorStatsModel4)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals(visitorStatsModel.unit, first().unit)
            assertEquals(visitorStatsModel.startDate, first().startDate)
            assertEquals(visitorStatsModel.endDate, first().endDate)
            assertEquals(visitorStatsModel2.unit, get(1).unit)
            assertEquals(visitorStatsModel2.startDate, get(1).startDate)
            assertEquals(visitorStatsModel2.endDate, get(1).endDate)
            assertEquals(visitorStatsModel3.unit, get(2).unit)
            assertEquals(visitorStatsModel3.startDate, get(2).startDate)
            assertEquals(visitorStatsModel3.endDate, get(2).endDate)
        }

        // Add another "day" entry, but for another site
        val visitorStatsModel5 = WCStatsTestUtils.generateSampleVisitorStatsModel(localSiteId = 8)
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(visitorStatsModel5)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(4, size)
            assertEquals(visitorStatsModel.unit, first().unit)
            assertEquals(visitorStatsModel.startDate, first().startDate)
            assertEquals(visitorStatsModel.endDate, first().endDate)
            assertEquals(visitorStatsModel2.unit, get(1).unit)
            assertEquals(visitorStatsModel2.startDate, get(1).startDate)
            assertEquals(visitorStatsModel2.endDate, get(1).endDate)
            assertEquals(visitorStatsModel3.unit, get(2).unit)
            assertEquals(visitorStatsModel3.startDate, get(2).startDate)
            assertEquals(visitorStatsModel3.endDate, get(2).endDate)
            assertEquals(visitorStatsModel5.unit, get(3).unit)
            assertEquals(visitorStatsModel5.localSiteId, get(3).localSiteId)
            assertEquals(visitorStatsModel5.startDate, get(3).startDate)
            assertEquals(visitorStatsModel5.endDate, get(3).endDate)
        }
    }

    @Test
    fun testGetRawVisitorStatsForSiteAndUnit() {
        val dayOrderStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel()
        val site = SiteModel().apply { id = dayOrderStatsModel.localSiteId }
        val monthOrderStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = "month", fields = "fake-data", data = "fake-data"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(dayOrderStatsModel)
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(monthOrderStatsModel)

        val site2 = SiteModel().apply { id = 8 }
        val altSiteOrderStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(localSiteId = site2.id)
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(altSiteOrderStatsModel)

        val dayOrderStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY
        )
        assertNotNull(dayOrderStats)
        with(dayOrderStats) {
            assertEquals("day", unit)
            assertEquals(false, isCustomField)
        }

        val dayOrderCustomStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(startDate = "2019-01-15",
                endDate = "2019-02-13")
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(dayOrderCustomStatsModel)
        val dayOrderCustomStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY, dayOrderCustomStatsModel.quantity,
                dayOrderCustomStatsModel.date, dayOrderCustomStatsModel.isCustomField
        )
        assertNotNull(dayOrderCustomStats)
        with(dayOrderCustomStats) {
            assertEquals("day", unit)
            assertEquals(true, isCustomField)
        }
        assertNotNull(dayOrderStats)

        val altSiteDayOrderStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site2, OrderStatsApiUnit.DAY
        )
        assertNotNull(altSiteDayOrderStats)

        val monthOrderStatus = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.MONTH
        )
        assertNotNull(monthOrderStatus)
        with(monthOrderStatus) {
            assertEquals("month", unit)
            assertEquals(false, isCustomField)
        }

        val nonExistentSite = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                SiteModel().apply { id = 88 }, OrderStatsApiUnit.DAY
        )
        assertNull(nonExistentSite)

        val missingData = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.YEAR
        )
        assertNull(missingData)
    }

    @Test
    fun testSimpleInsertionAndRetrievalOfCustomVisitorStats() {
        // Test Scenario - 1: Generate default stats with granularity - DAYS, quantity - 30, date - current date
        // and isCustomField - false
        // The total size of the local db table = 1
        val defaultDayVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel()
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultDayVisitorStatsModel)

        val site = SiteModel().apply { id = defaultDayVisitorStatsModel.localSiteId }

        val defaultDayVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY
        )

        assertEquals(defaultDayVisitorStatsModel.unit, defaultDayVisitorStats?.unit)
        assertEquals(defaultDayVisitorStatsModel.quantity, defaultDayVisitorStats?.quantity)
        assertEquals(defaultDayVisitorStatsModel.date, defaultDayVisitorStats?.date)
        assertEquals(defaultDayVisitorStatsModel.isCustomField, defaultDayVisitorStats?.isCustomField)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(1, size)
        }

        // Test Scenario - 2: Generate custom stats for same site with granularity - DAYS, quantity - 1,
        // date - 2019-01-01 and isCustomField - true
        // The total size of the local db table = 2
        val customDayVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2018-12-31"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customDayVisitorStatsModel)
        val customDayVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.date, customDayVisitorStatsModel.isCustomField
        )

        assertEquals(customDayVisitorStatsModel.unit, customDayVisitorStats?.unit)
        assertEquals(customDayVisitorStatsModel.quantity, customDayVisitorStats?.quantity)
        assertEquals(customDayVisitorStatsModel.endDate, customDayVisitorStats?.endDate)
        assertEquals(customDayVisitorStatsModel.startDate, customDayVisitorStats?.startDate)
        assertEquals(customDayVisitorStatsModel.date, customDayVisitorStats?.date)
        assertEquals(customDayVisitorStatsModel.isCustomField, customDayVisitorStats?.isCustomField)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        // Test Scenario - 3: Overwrite an existing default stats for same site, same unit, same quantity and same date,
        // The total size of the local db table = 2 (since no new data is inserted)
        val defaultDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel()
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultDayVisitorStatsModel2)
        val defaultDayVisitorStats2 = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY
        )

        assertEquals(defaultDayVisitorStatsModel2.unit, defaultDayVisitorStats2?.unit)
        assertEquals(defaultDayVisitorStatsModel2.quantity, defaultDayVisitorStats2?.quantity)
        assertEquals(defaultDayVisitorStatsModel2.date, defaultDayVisitorStats2?.date)
        assertEquals(defaultDayVisitorStatsModel2.isCustomField, defaultDayVisitorStats2?.isCustomField)
        assertEquals("", defaultDayVisitorStats2?.startDate)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        // Test Scenario - 4: Overwrite an existing custom stats for same site, same unit, same quantity and same date,
        // The total size of the local db table = 2 (since no new data is inserted)
        val customDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2018-12-31"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customDayVisitorStatsModel2)
        val customDayVisitorStats2 = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY, customDayVisitorStatsModel2.quantity,
                customDayVisitorStatsModel2.endDate, customDayVisitorStatsModel2.isCustomField
        )

        assertEquals(customDayVisitorStatsModel2.unit, customDayVisitorStats2?.unit)
        assertEquals(customDayVisitorStatsModel2.quantity, customDayVisitorStats2?.quantity)
        assertEquals(customDayVisitorStatsModel2.endDate, customDayVisitorStats2?.endDate)
        assertEquals(customDayVisitorStatsModel2.startDate, customDayVisitorStats2?.startDate)
        assertEquals(customDayVisitorStatsModel2.date, customDayVisitorStats2?.date)
        assertEquals(customDayVisitorStatsModel2.isCustomField, customDayVisitorStats2?.isCustomField)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        // Test Scenario - 5: Overwrite an existing custom stats for same site, unit, quantity but different date,
        // The total size of the local db table = 2 (since no old data was purged and new data was inserted)
        val customDayVisitorStatsModel3 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2018-12-31", startDate = "2018-12-31"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customDayVisitorStatsModel3)
        val customDayVisitorStats3 = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.DAY, customDayVisitorStatsModel3.quantity,
                customDayVisitorStatsModel3.endDate, customDayVisitorStatsModel3.isCustomField
        )

        assertEquals(customDayVisitorStatsModel3.unit, customDayVisitorStats3?.unit)
        assertEquals(customDayVisitorStatsModel3.quantity, customDayVisitorStats3?.quantity)
        assertEquals(customDayVisitorStatsModel3.endDate, customDayVisitorStats3?.endDate)
        assertEquals(customDayVisitorStatsModel3.startDate, customDayVisitorStats3?.startDate)
        assertEquals(customDayVisitorStatsModel3.date, customDayVisitorStats3?.date)
        assertEquals(customDayVisitorStatsModel3.isCustomField, customDayVisitorStats3?.isCustomField)

        /* expected size of local cache would still be 2 because there can only be one
         * custom stats row stored in local cache at any point of time. Before storing incoming data,
         * the existing data will be purged */
        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        // Test Scenario - 6: Generate default stats for same site with different unit,
        // The total size of the local db table = 3 (since stats with DAYS granularity would be stored already)
        val defaultWeekVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString(), quantity = "17"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultWeekVisitorStatsModel)
        val defaultWeekVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.WEEK
        )

        assertEquals(defaultWeekVisitorStatsModel.unit, defaultWeekVisitorStats?.unit)
        assertEquals(defaultWeekVisitorStatsModel.quantity, defaultWeekVisitorStats?.quantity)
        assertEquals(defaultWeekVisitorStatsModel.endDate, defaultWeekVisitorStats?.endDate)
        assertEquals(defaultWeekVisitorStatsModel.date, defaultWeekVisitorStats?.date)
        assertEquals(defaultWeekVisitorStatsModel.isCustomField, defaultWeekVisitorStats?.isCustomField)
        assertEquals("", defaultWeekVisitorStats?.startDate)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(3, size)
        }

        // Test Scenario - 7: Generate custom stats for same site with different unit:
        // The total size of the local db table = 3 (since stats with DAYS granularity would be stored already)
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString(), quantity = "2",
                endDate = "2019-01-28", startDate = "2019-01-25"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customWeekVisitorStatsModel)
        val customWeekVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site, OrderStatsApiUnit.WEEK, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.date, customWeekVisitorStatsModel.isCustomField
        )

        assertEquals(customWeekVisitorStatsModel.unit, customWeekVisitorStats?.unit)
        assertEquals(customWeekVisitorStatsModel.quantity, customWeekVisitorStats?.quantity)
        assertEquals(customWeekVisitorStatsModel.endDate, customWeekVisitorStats?.endDate)
        assertEquals(customWeekVisitorStatsModel.startDate, customWeekVisitorStats?.startDate)
        assertEquals(customWeekVisitorStatsModel.date, customWeekVisitorStats?.date)
        assertEquals(customWeekVisitorStatsModel.isCustomField, customWeekVisitorStats?.isCustomField)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(3, size)
        }

        // Test Scenario - 8: Generate default stats for different site with different unit:
        // The total size of the local db table = 5 (since stats with DAYS and WEEKS data would be stored already)
        val site2 = SiteModel().apply { id = 8 }
        val defaultMonthVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                        localSiteId = site2.id, unit = OrderStatsApiUnit.MONTH.toString(), quantity = "12"
                )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultMonthVisitorStatsModel)
        val defaultMonthVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site2, OrderStatsApiUnit.MONTH
        )

        assertEquals(defaultMonthVisitorStatsModel.unit, defaultMonthVisitorStats?.unit)
        assertEquals(defaultMonthVisitorStatsModel.quantity, defaultMonthVisitorStats?.quantity)
        assertEquals(defaultMonthVisitorStatsModel.date, defaultMonthVisitorStats?.date)
        assertEquals(defaultMonthVisitorStatsModel.isCustomField, defaultMonthVisitorStats?.isCustomField)
        assertEquals("", defaultMonthVisitorStats?.startDate)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(4, size)
        }

        // Test Scenario - 9: Generate custom stats for different site with different unit and different date
        // The total size of the local db table = 5 (since 3 default stats for another site would be stored already
        // and 1 stats for site 8 would be stored). No purging of data would take place
        val customMonthVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                localSiteId = site2.id, unit = OrderStatsApiUnit.MONTH.toString(), quantity = "2",
                endDate = "2019-01-28", startDate = "2018-12-31"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customMonthVisitorStatsModel)
        val customMonthVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site2, OrderStatsApiUnit.MONTH, customMonthVisitorStatsModel.quantity,
                customMonthVisitorStatsModel.date, customMonthVisitorStatsModel.isCustomField
        )

        assertEquals(customMonthVisitorStatsModel.unit, customMonthVisitorStats?.unit)
        assertEquals(customMonthVisitorStatsModel.quantity, customMonthVisitorStats?.quantity)
        assertEquals(customMonthVisitorStatsModel.endDate, customMonthVisitorStats?.endDate)
        assertEquals(customMonthVisitorStatsModel.startDate, customMonthVisitorStats?.startDate)
        assertEquals(customMonthVisitorStatsModel.date, customMonthVisitorStats?.date)
        assertEquals(customMonthVisitorStatsModel.isCustomField, customMonthVisitorStats?.isCustomField)

        with(WellSql.select(WCVisitorStatsModel::class.java).asModel) {
            assertEquals(5, size)
        }

        // Test Scenario - 10: Check for missing stats data. Query should return null
        val missingData = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site2, OrderStatsApiUnit.YEAR, "1", "2019-01-01"
        )
        assertNull(missingData)

        // Test Scenario - 11: Fetch data with only site(8) and granularity (MONTHS)
        val defaultVisitorStats = WCVisitorStatsSqlUtils.getRawVisitorStatsForSiteUnitQuantityAndDate(
                site2, OrderStatsApiUnit.MONTH
        )
        assertNotNull(defaultVisitorStats)
        assertEquals(OrderStatsApiUnit.MONTH.toString(), defaultVisitorStats.unit)
    }
}
