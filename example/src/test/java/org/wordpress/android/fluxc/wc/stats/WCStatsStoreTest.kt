package org.wordpress.android.fluxc.wc.stats

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.runBlocking
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
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCVisitorStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WCVisitorStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsResponsePayload
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
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
    private val appContext = RuntimeEnvironment.application.applicationContext
    private val wcStatsStore = WCStatsStore(Dispatcher(), appContext, mockOrderStatsRestClient, initCoroutineEngine())

    @Before
    fun setUp() {
        val config = SingleStoreWellSqlConfigForTests(
                appContext, listOf(WCOrderStatsModel::class.java,
                WCRevenueStatsModel::class.java,
                WCVisitorStatsModel::class.java,
                WCNewVisitorStatsModel::class.java),
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
            verify(mockOrderStatsRestClient).fetchStats(any(), any(),
                    dateArgument.capture(), any(), any(), anyOrNull(), anyOrNull())
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
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(),
                    any(), anyOrNull(), anyOrNull())
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

        val dayOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site, OrderStatsApiUnit.DAY)
        assertNotNull(dayOrderStats)
        with(dayOrderStats) {
            assertEquals("day", unit)
            assertEquals(false, isCustomField)
        }

        val dayOrderCustomStatsModel = WCStatsTestUtils.generateSampleStatsModel(startDate = "2019-01-15",
                endDate = "2019-02-13")
        WCStatsSqlUtils.insertOrUpdateStats(dayOrderCustomStatsModel)
        val dayOrderCustomStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.DAY, dayOrderCustomStatsModel.quantity, dayOrderCustomStatsModel.date,
                dayOrderCustomStatsModel.isCustomField)
        assertNotNull(dayOrderCustomStats)
        with(dayOrderCustomStats) {
            assertEquals("day", unit)
            assertEquals(true, isCustomField)
        }
        assertNotNull(dayOrderStats)

        val altSiteDayOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site2, OrderStatsApiUnit.DAY)
        assertNotNull(altSiteDayOrderStats)

        val monthOrderStatus = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site, OrderStatsApiUnit.MONTH)
        assertNotNull(monthOrderStatus)
        with(monthOrderStatus) {
            assertEquals("month", unit)
            assertEquals(false, isCustomField)
        }

        val nonExistentSite = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(
                SiteModel().apply { id = 88 }, OrderStatsApiUnit.DAY
        )
        assertNull(nonExistentSite)

        val missingData = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site, OrderStatsApiUnit.YEAR)
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

    @Test
    fun testGetQuantityForDays() {
        val quantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-25", "2018-01-28",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(4, quantity1)

        val quantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2018-01-01",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(1, quantity2)

        val quantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2018-01-31",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(31, quantity3)

        val quantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-28", "2018-01-25",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(4, quantity4)

        val quantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2018-01-01",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(1, quantity5)

        val quantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-31", "2018-01-01",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(31, quantity6)

        val defaultQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("", "",
                OrderStatsApiUnit.DAY, 30)
        assertEquals(30, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit(null, null,
                OrderStatsApiUnit.DAY, 30)
        assertEquals(30, defaultQuantity2)
    }

    @Test
    fun testGetQuantityForWeeks() {
        val quantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-10-22", "2018-10-23",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(1, quantity1)

        val quantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-01-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(53, quantity2)

        val quantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2019-01-20", "2019-01-13",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(2, quantity3)

        val quantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-03-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(61, quantity4)

        val quantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2018-01-31",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(5, quantity5)

        val quantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-12-01", "2018-12-31",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(6, quantity6)

        val quantity7 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-11-01", "2018-11-30",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(5, quantity7)

        val inverseQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-10-23", "2018-10-22",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(1, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2017-01-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(53, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2019-01-13", "2019-01-20",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(2, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-03-01", "2017-01-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(61, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-31", "2018-01-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(5, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-12-31", "2018-12-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(6, inverseQuantity6)

        val inverseQuantity7 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-11-30", "2018-11-01",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(5, inverseQuantity7)

        val defaultQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("", "",
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(17, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit(null, null,
                OrderStatsApiUnit.WEEK, 17)
        assertEquals(17, defaultQuantity2)
    }

    @Test
    fun testGetQuantityForMonths() {
        val quantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-10-22", "2018-10-23",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(1, quantity1)

        val quantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-01-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(13, quantity2)

        val quantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2018-01-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(1, quantity3)

        val quantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-03-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(15, quantity4)

        val quantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-01-31",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(13, quantity5)

        val quantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-12-31", "2019-01-01",
                OrderStatsApiUnit.MONTH, 1)
        assertEquals(2, quantity6)

        val inverseQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-10-23", "2018-10-22",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(1, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2017-01-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(13, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2018-01-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(1, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-03-01", "2017-01-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(15, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-31", "2017-01-01",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(13, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2019-01-01", "2018-12-31",
                OrderStatsApiUnit.MONTH, 1)
        assertEquals(2, inverseQuantity6)

        val defaultQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("", "",
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(12, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit(null, null,
                OrderStatsApiUnit.MONTH, 12)
        assertEquals(12, defaultQuantity2)
    }

    @Test
    fun testGetQuantityForYears() {
        val quantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, quantity1)

        val quantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-03-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, quantity2)

        val quantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2018-01-05",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, quantity3)

        val quantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2019-03-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(3, quantity4)

        val quantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2015-03-05", "2017-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(3, quantity5)

        val quantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-12-31", "2019-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, quantity6)

        val quantity7 = wcStatsStore.getQuantityByOrderStatsApiUnit("2019-01-25", "2019-01-25",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(1, quantity7)

        val inverseQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-01", "2017-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-03-01", "2017-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getQuantityByOrderStatsApiUnit("2018-01-05", "2017-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getQuantityByOrderStatsApiUnit("2019-03-01", "2017-01-01",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(3, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getQuantityByOrderStatsApiUnit("2017-01-01", "2015-03-05",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(3, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getQuantityByOrderStatsApiUnit("2019-01-01", "2018-12-31",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(2, inverseQuantity6)

        val defaultQuantity1 = wcStatsStore.getQuantityByOrderStatsApiUnit("", "",
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(1, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByOrderStatsApiUnit(null, null,
                OrderStatsApiUnit.YEAR, 1)
        assertEquals(1, defaultQuantity2)
    }

    @Test
    fun testFetchOrderStatsForDaysDate() {
        val startDate = "2019-01-01"
        val endDate = "2019-01-01"
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy-MM-dd", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy-MM-dd", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = DateUtils.formatDate("YYYY-MM-dd", DateUtils.getDateFromString(endDate))
        assertThat(localDate, anyOf(isEqual(plus12SiteDate), isEqual(minus12SiteDate)))
        assertThat(localDate, anyOf(not(plus12SiteDate), not(minus12SiteDate)))
    }

    @Test
    fun testFetchOrderStatsForWeeksDate() {
        val startDate = "2019-01-25"
        val endDate = "2019-01-28"
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.WEEKS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy-'W'ww", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.WEEKS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy-'W'ww", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = DateUtils.formatDate("yyyy-'W'ww", DateUtils.getDateFromString(endDate))

        assertThat(localDate, isEqual(plus12SiteDate))
        assertThat(localDate, isEqual(minus12SiteDate))
    }

    @Test
    fun testFetchOrderStatsForMonthsDate() {
        val startDate = "2019-01-25"
        val endDate = "2019-01-28"

        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.MONTHS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy-MM", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.MONTHS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy-MM", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = DateUtils.formatDate("yyyy-MM", DateUtils.getDateFromString(endDate))

        assertThat(localDate, isEqual(plus12SiteDate))
        assertThat(localDate, isEqual(minus12SiteDate))
    }

    @Test
    fun testFetchOrderStatsForYearsDate() {
        val startDate = "2018-12-25"
        val endDate = "2019-01-28"

        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.YEARS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.YEARS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = DateUtils.getDateTimeForSite(it, "yyyy", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(),
                    any(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = DateUtils.formatDate("yyyy", DateUtils.getDateFromString(endDate))

        assertThat(localDate, isEqual(plus12SiteDate))
        assertThat(localDate, isEqual(minus12SiteDate))
    }

    @Test
    fun testFetchOrderStatsForDaysQuantity() {
        val startDate = "2019-01-01"
        val endDate = "2019-01-01"

        val siteModel = SiteModel()
        val payload = FetchOrderStatsPayload(siteModel, StatsGranularity.DAYS, startDate, endDate)
        wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

        val quantity: Long =
                wcStatsStore.getQuantityByOrderStatsApiUnit(startDate, endDate, OrderStatsApiUnit.DAY, 30)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient)
                .fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any(), any(), any())

        val calculatedQuantity: Long = quantityArgument.firstValue
        assertEquals(quantity, calculatedQuantity)
    }

    @Test
    fun testFetchOrderStatsForWeeksQuantity() {
        val startDate = "2019-01-25"
        val endDate = "2019-01-28"

        val siteModel = SiteModel()
        val payload = FetchOrderStatsPayload(siteModel, StatsGranularity.WEEKS, startDate, endDate)
        wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

        val quantity: Long =
                wcStatsStore.getQuantityByOrderStatsApiUnit(startDate, endDate, OrderStatsApiUnit.WEEK, 17)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient)
                .fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any(), any(), any())

        val calculatedQuantity: Long = quantityArgument.firstValue
        assertEquals(quantity, calculatedQuantity)
    }

    @Test
    fun testFetchOrderStatsForMonthsQuantity() {
        val startDate = "2018-12-25"
        val endDate = "2019-01-28"

        val siteModel = SiteModel()
        val payload = FetchOrderStatsPayload(siteModel, StatsGranularity.MONTHS, startDate, endDate)
        wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

        val quantity: Long =
                wcStatsStore.getQuantityByOrderStatsApiUnit(startDate, endDate, OrderStatsApiUnit.MONTH, 12)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient)
                .fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any(), any(), any())

        val calculatedQuantity: Long = quantityArgument.firstValue
        assertEquals(quantity, calculatedQuantity)
    }

    @Test
    fun testFetchOrderStatsForYearsQuantity() {
        val startDate = "2018-12-25"
        val endDate = "2019-01-28"

        val siteModel = SiteModel()
        val payload = FetchOrderStatsPayload(siteModel, StatsGranularity.YEARS, startDate, endDate)
        wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

        val quantity: Long =
                wcStatsStore.getQuantityByOrderStatsApiUnit(startDate, endDate, OrderStatsApiUnit.YEAR, 12)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient)
                .fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any(), any(), any())

        val calculatedQuantity: Long = quantityArgument.firstValue
        assertEquals(quantity, calculatedQuantity)
    }

    @Test
    fun testInsertionAndRetrievalForCustomStats() {
        /*
         * Test Scenario - I
         * Generate default stats with
         * granularity - DAYS
         * quantity - 30
         * date - current date
         * isCustomField - false
         *
         * 1. This generated data to be inserted to local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 1
         * */
        val defaultDayOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        WCStatsSqlUtils.insertOrUpdateStats(defaultDayOrderStatsModel)

        val site = SiteModel().apply { id = defaultDayOrderStatsModel.localSiteId }

        val defaultDayOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site, OrderStatsApiUnit.DAY)

        assertEquals(defaultDayOrderStatsModel.unit, defaultDayOrderStats?.unit)
        assertEquals(defaultDayOrderStatsModel.quantity, defaultDayOrderStats?.quantity)
        assertEquals(defaultDayOrderStatsModel.endDate, defaultDayOrderStats?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(1, size)
        }

        /*
         * Test Scenario - II
         * Generate custom stats for same site:
         * granularity - DAYS
         * quantity - 1
         * date - 2019-01-01
         * isCustomField - true
         *
         * 1. This generated data to be inserted to local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 2
         * */
        val customDayOrderStatsModel =
                WCStatsTestUtils.generateSampleStatsModel(quantity = "1",
                        endDate = "2019-01-01", startDate = "2018-12-31")

        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel)
        val customDayOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.DAY, customDayOrderStatsModel.quantity, customDayOrderStatsModel.endDate,
                customDayOrderStatsModel.isCustomField)

        assertEquals(customDayOrderStatsModel.unit, customDayOrderStats?.unit)
        assertEquals(customDayOrderStatsModel.quantity, customDayOrderStats?.quantity)
        assertEquals(customDayOrderStatsModel.endDate, customDayOrderStats?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        /*
         * Test Scenario - III
         * Overwrite an existing default stats for same site, same unit, same quantity and same date:
         * granularity - DAYS
         * quantity - 30
         * date - current date
         * isCustomField - false
         *
         * 1. This generated data would be updated to the local db (not inserted)
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 2 (since no new data is inserted)
         * */
        val defaultDayOrderStatsModel2 = WCStatsTestUtils.generateSampleStatsModel()
        WCStatsSqlUtils.insertOrUpdateStats(defaultDayOrderStatsModel2)
        val defaultDayOrderStats2 = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.DAY)

        assertEquals(defaultDayOrderStatsModel2.unit, defaultDayOrderStats2?.unit)
        assertEquals(defaultDayOrderStatsModel2.quantity, defaultDayOrderStats2?.quantity)
        assertEquals(defaultDayOrderStatsModel2.endDate, defaultDayOrderStats2?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        /*
         * Test Scenario - IV
         * Overwrite an existing custom stats for same site, same unit, same quantity and same date:
         * granularity - DAYS
         * quantity - 1
         * date - 2019-01-01
         * isCustomField - true
         *
         * 1. This generated data would be updated to the local db (not inserted)
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 2 (since no new data is inserted)
         * */
        val customDayOrderStatsModel2 =
                WCStatsTestUtils.generateSampleStatsModel(quantity = "1",
                        endDate = "2019-01-01", startDate = "2018-12-31")

        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel2)
        val customDayOrderStats2 = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.DAY, customDayOrderStatsModel2.quantity, customDayOrderStatsModel2.endDate,
                customDayOrderStatsModel2.isCustomField)

        assertEquals(customDayOrderStatsModel2.unit, customDayOrderStats2?.unit)
        assertEquals(customDayOrderStatsModel2.quantity, customDayOrderStats2?.quantity)
        assertEquals(customDayOrderStatsModel2.endDate, customDayOrderStats2?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        /*
         * Test Scenario - V
         * Overwrite an existing custom stats for same site, same unit, same quantity but different date:
         * granularity - DAYS
         * quantity - 1
         * date - 2018-12-31
         * isCustomField - true
         *
         * 1. The data already stored with isCustomField would be purged.
         * 2. This generated data would be inserted to the local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 2 (since no old data was purged and new data was inserted)
         * */
        val customDayOrderStatsModel3 =
                WCStatsTestUtils.generateSampleStatsModel(quantity = "1",
                        endDate = "2018-12-31", startDate = "2018-12-31")

        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel3)
        val customDayOrderStats3 = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.DAY, customDayOrderStatsModel3.quantity, customDayOrderStatsModel3.endDate,
                customDayOrderStatsModel3.isCustomField)

        assertEquals(customDayOrderStatsModel3.unit, customDayOrderStats3?.unit)
        assertEquals(customDayOrderStatsModel3.quantity, customDayOrderStats3?.quantity)
        assertEquals(customDayOrderStatsModel3.endDate, customDayOrderStats3?.endDate)

        /* expected size of local cache would still be 2 because there can only be one
         * custom stats row stored in local cache at any point of time. Before storing incoming data,
         * the existing data will be purged */
        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(2, size)
        }

        /*
         * Test Scenario - VI
         * Generate default stats for same site with different unit:
         * granularity - WEEKS
         * quantity - 17
         * date - current date
         * isCustomField - false
         *
         * 1. This generated data to be inserted to local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 3 (since stats with DAYS granularity would be stored already)
         * */
        val defaultWeekOrderStatsModel =
                WCStatsTestUtils.generateSampleStatsModel(unit = OrderStatsApiUnit.WEEK.toString(), quantity = "17")

        WCStatsSqlUtils.insertOrUpdateStats(defaultWeekOrderStatsModel)
        val defaultWeekOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.WEEK)

        assertEquals(defaultWeekOrderStatsModel.unit, defaultWeekOrderStats?.unit)
        assertEquals(defaultWeekOrderStatsModel.quantity, defaultWeekOrderStats?.quantity)
        assertEquals(defaultWeekOrderStatsModel.endDate, defaultWeekOrderStats?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(3, size)
        }

        /*
         * Test Scenario - VII
         * Generate custom stats for same site with different unit:
         * granularity - WEEKS
         * quantity - 2
         * date - 2019-01-28
         * isCustomField - true
         *
         * 1. This generated data to be inserted to local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 3 (since 2 default stats would be stored
         *    already and previously stored custom stats would be purged)
         * */
        val customWeekOrderStatsModel =
                WCStatsTestUtils.generateSampleStatsModel(unit = OrderStatsApiUnit.WEEK.toString(),
                        quantity = "2", endDate = "2019-01-28", startDate = "2019-01-25")

        WCStatsSqlUtils.insertOrUpdateStats(customWeekOrderStatsModel)
        val customWeekOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site,
                OrderStatsApiUnit.WEEK, customWeekOrderStatsModel.quantity, customWeekOrderStatsModel.endDate,
                customWeekOrderStatsModel.isCustomField)

        assertEquals(customWeekOrderStatsModel.unit, customWeekOrderStats?.unit)
        assertEquals(customWeekOrderStatsModel.quantity, customWeekOrderStats?.quantity)
        assertEquals(customWeekOrderStatsModel.endDate, customWeekOrderStats?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(3, size)
        }

        /*
         * Test Scenario - VIII
         * Generate default stats for different site with different unit:
         * siteId - 8
         * granularity - MONTHS
         * quantity - 12
         * date - current date
         * isCustomField - false
         *
         * 1. This generated data to be inserted to local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 5 (since stats with DAYS and WEEKS granularity
         *    would be stored already)
         * */
        val site2 = SiteModel().apply { id = 8 }
        val defaultMonthOrderStatsModel =
                WCStatsTestUtils.generateSampleStatsModel(localSiteId = site2.id,
                        unit = OrderStatsApiUnit.MONTH.toString(), quantity = "12")

        WCStatsSqlUtils.insertOrUpdateStats(defaultMonthOrderStatsModel)
        val defaultMonthOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site2,
                OrderStatsApiUnit.MONTH)

        assertEquals(defaultMonthOrderStatsModel.unit, defaultMonthOrderStats?.unit)
        assertEquals(defaultMonthOrderStatsModel.quantity, defaultMonthOrderStats?.quantity)
        assertEquals(defaultMonthOrderStatsModel.endDate, defaultMonthOrderStats?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(4, size)
        }

        /*
         * Test Scenario - IX
         * Generate custom stats for different site with different unit and different date:
         * siteId - 8
         * granularity - MONTHS
         * quantity - 2
         * date - 2019-01-28
         * isCustomField - true
         *
         * 1. This generated data to be inserted to local db
         * 2. The data stored to be queried again to check against this generated data
         * 3. The total size of the local db table = 5 (since 3 default stats for another site would be stored
         *    already and 1 stats for site 8 would be stored). No purging of data would take place
         * */
        val customMonthOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel(localSiteId = site2.id,
                unit = OrderStatsApiUnit.MONTH.toString(), quantity = "2",
                endDate = "2019-01-28", startDate = "2018-12-31")

        WCStatsSqlUtils.insertOrUpdateStats(customMonthOrderStatsModel)
        val customMonthOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site2,
                OrderStatsApiUnit.MONTH, customMonthOrderStatsModel.quantity, customMonthOrderStatsModel.endDate,
                customMonthOrderStatsModel.isCustomField)

        assertEquals(customMonthOrderStatsModel.unit, customMonthOrderStats?.unit)
        assertEquals(customMonthOrderStatsModel.quantity, customMonthOrderStats?.quantity)
        assertEquals(customMonthOrderStatsModel.endDate, customMonthOrderStats?.endDate)

        with(WellSql.select(WCOrderStatsModel::class.java).asModel) {
            assertEquals(5, size)
        }

        /*
         * Test Scenario - X
         * Check if the below query returns null
         * */
        val missingData = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site2,
                OrderStatsApiUnit.YEAR, "1", "2019-01-01")

        assertNull(missingData)

        /*
         * Test Scenario - XI
         * Fetch data with only site and granularity:
         * siteId - 8
         * granularity - MONTHS
         *
         * */
        val defaultOrderStats = WCStatsSqlUtils.getRawStatsForSiteUnitQuantityAndDate(site2, OrderStatsApiUnit.MONTH)
        assertNotNull(defaultOrderStats)
        assertEquals(OrderStatsApiUnit.MONTH.toString(), defaultOrderStats.unit)
    }

    @Test
    fun testGetCustomStatsForDaysGranularity() {
        /*
         * Test Scenario - I
         * Generate default stats with
         * granularity - DAYS
         * quantity - 30
         * date - current date
         * isCustomField - false
         *
         * 1. This generated data to be inserted to local db
         * 2. Get Revenue and Order Stats of the same site and granularity
         * 3. Assert Not Null
         * */
        val defaultDayOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        WCStatsSqlUtils.insertOrUpdateStats(defaultDayOrderStatsModel)

        val site = SiteModel().apply { id = defaultDayOrderStatsModel.localSiteId }

        val defaultDayOrderRevenueStats = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS)
        val defaultDayOrderStats = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayOrderRevenueStats.isNotEmpty())
        assertTrue(defaultDayOrderStats.isNotEmpty())

        /*
         * Test Scenario - II
         * Generate default stats with a different date in the future
         */
        val defaultDayOrderStatsModel2 = WCStatsTestUtils.generateSampleStatsModel(endDate = "2019-03-20")
        WCStatsSqlUtils.insertOrUpdateStats(defaultDayOrderStatsModel2)

        WCStatsSqlUtils.getFirstRawStatsForSite(site)?.let {
            assertEquals(defaultDayOrderStatsModel2.date, it.date)
        }

        val defaultDayOrderRevenueStats2 = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS)
        val defaultDayOrderStats2 = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS)

        assertTrue(defaultDayOrderRevenueStats2.isNotEmpty())
        assertTrue(defaultDayOrderStats2.isNotEmpty())

        /*
         * Test Scenario - III
         * Generate custom stats for same site:
         * granularity - DAYS
         * quantity - 1
         * date - 2019-01-01
         * isCustomField - true
         *
         * 1. This generated data to be inserted to local db
         * 2. Get Revenue and Order Stats of the same site and granularity
         * 3. Assert Not Null
         * */
        val customDayOrderStatsModel =
                WCStatsTestUtils.generateSampleStatsModel(quantity = "1", endDate = "2019-01-01",
                        startDate = "2019-01-01")
        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel)

        val customDayOrderRevenueStats = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel.quantity, customDayOrderStatsModel.endDate,
                customDayOrderStatsModel.isCustomField)

        val customDayOrderStats = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel.quantity, customDayOrderStatsModel.endDate,
                customDayOrderStatsModel.isCustomField)

        assertTrue(customDayOrderRevenueStats.isNotEmpty())
        assertTrue(customDayOrderStats.isNotEmpty())

        /*
         * Test Scenario - IV
         * Query for custom stats that is not present in local cache: for same site, same quantity, different date
         * granularity - DAYS
         * quantity - 1
         * date - 2018-12-01
         * isCustomField - true
         *
         * 1. Get Revenue and Order Stats of the same site and granularity
         * 3. Assert Null
         * */
        val customDayOrderStatsModel2 = WCStatsTestUtils.generateSampleStatsModel(quantity = "1",
                endDate = "2018-12-01", startDate = "2018-12-01")
        val customDayOrderRevenueStats2 = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel2.quantity, customDayOrderStatsModel2.endDate,
                customDayOrderStatsModel2.isCustomField)

        val customDayOrderStats2 = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel2.quantity, customDayOrderStatsModel2.endDate,
                customDayOrderStatsModel2.isCustomField)

        assertTrue(customDayOrderRevenueStats2.isEmpty())
        assertTrue(customDayOrderStats2.isEmpty())

        /*
         * Test Scenario - V
         * Query for custom stats that is not present in local cache:
         * for same site, different quantity, different date
         * granularity - DAYS
         * quantity - 30
         * date - 2018-12-01
         * isCustomField - true
         *
         * 1. Get Revenue and Order Stats of the same site and granularity but different quantity, different date
         * 3. Assert Null
         * */
        val customDayOrderStatsModel3 = WCStatsTestUtils.generateSampleStatsModel(quantity = "1",
                endDate = "2018-12-01", startDate = "2018-12-01")

        val customDayOrderRevenueStats3 = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel3.quantity, customDayOrderStatsModel3.endDate,
                customDayOrderStatsModel3.isCustomField)

        val customDayOrderStats3 = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel3.quantity, customDayOrderStatsModel3.endDate,
                customDayOrderStatsModel3.isCustomField)

        assertTrue(customDayOrderRevenueStats3.isEmpty())
        assertTrue(customDayOrderStats3.isEmpty())

        /*
         * Test Scenario - VI
         * Generate custom stats for same site with different granularity, same date, same quantity
         * granularity - WEEKS
         * quantity - 1
         * date - 2019-01-01
         * isCustomField - true
         *
         * 1. This generated data to be inserted to local db
         * 2. Get Revenue and Order Stats of the same site and granularity
         * 3. Assert Not Null
         * 4. Now if another query ran for granularity - DAYS, with same date and same quantity:
         *    Assert Null
         * */
        val customWeekOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel(quantity = "1",
                endDate = "2019-01-01", startDate = "2019-01-01",
                unit = OrderStatsApiUnit.fromStatsGranularity(WEEKS).toString())

        WCStatsSqlUtils.insertOrUpdateStats(customWeekOrderStatsModel)

        val customWeekOrderRevenueStats = wcStatsStore.getRevenueStats(site, StatsGranularity.WEEKS,
                customWeekOrderStatsModel.quantity, customWeekOrderStatsModel.endDate,
                customWeekOrderStatsModel.isCustomField)

        val customWeekOrderStats = wcStatsStore.getOrderStats(site, StatsGranularity.WEEKS,
                customWeekOrderStatsModel.quantity, customWeekOrderStatsModel.endDate,
                customWeekOrderStatsModel.isCustomField)

        assertTrue(customWeekOrderRevenueStats.isNotEmpty())
        assertTrue(customWeekOrderStats.isNotEmpty())

        val customDayOrderRevenueStats4 = wcStatsStore.getRevenueStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel.quantity, customDayOrderStatsModel.endDate,
                customDayOrderStatsModel.isCustomField)

        val customDayOrderStats4 = wcStatsStore.getOrderStats(site, StatsGranularity.DAYS,
                customDayOrderStatsModel.quantity, customDayOrderStatsModel.endDate,
                customDayOrderStatsModel.isCustomField)

        assertTrue(customDayOrderRevenueStats4.isEmpty())
        assertTrue(customDayOrderStats4.isEmpty())

        /*
         * Test Scenario - VII
         * Generate custom stats for different site with same granularity, same date, same quantity
         * site - 8
         * granularity - WEEKS
         * quantity - 1
         * date - 2019-01-01
         * isCustomField - true
         *
         * 1. This generated data to be inserted to local db
         * 2. Get Revenue and Order Stats of the same site and granularity
         * 3. Assert Not Null
         * 4. Now if scenario IV is run again it assert NOT NULL, since the stats is for different sites
         * */
        val customWeekOrderStatsModel2 = WCStatsTestUtils.generateSampleStatsModel(localSiteId = 8,
                unit = OrderStatsApiUnit.fromStatsGranularity(WEEKS).toString(), quantity = "1",
                endDate = "2019-01-01", startDate = "2019-01-01")

        WCStatsSqlUtils.insertOrUpdateStats(customWeekOrderStatsModel2)

        val customWeekOrderRevenueStats2 = wcStatsStore.getRevenueStats(site, StatsGranularity.WEEKS,
                customWeekOrderStatsModel2.quantity, customWeekOrderStatsModel2.endDate,
                customWeekOrderStatsModel2.isCustomField)

        val customWeekOrderStats2 = wcStatsStore.getOrderStats(site, StatsGranularity.WEEKS,
                customWeekOrderStatsModel2.quantity, customWeekOrderStatsModel2.endDate,
                customWeekOrderStatsModel2.isCustomField)

        assertTrue(customWeekOrderRevenueStats2.isNotEmpty())
        assertTrue(customWeekOrderStats2.isNotEmpty())

        /* Now if scenario IV is run again it assert NOT NULL, since the stats is for different sites */
        assertTrue(customWeekOrderRevenueStats.isNotEmpty())
        assertTrue(customWeekOrderStats.isNotEmpty())
    }

    @Test
    fun testGetCustomStatsForSite() {
        val defaultDayOrderStatsModel = WCStatsTestUtils.generateSampleStatsModel()
        WCStatsSqlUtils.insertOrUpdateStats(defaultDayOrderStatsModel)

        val site = SiteModel().apply { id = defaultDayOrderStatsModel.localSiteId }
        val customStats = wcStatsStore.getCustomStatsForSite(site)
        assertNull(customStats)

        /*
         * For same site, but for custom stats
         * */
        val customDayOrderStatsModel = WCStatsTestUtils
                .generateSampleStatsModel(unit = StatsGranularity.MONTHS.toString(),
                        quantity = "2", endDate = "2019-01-01", startDate = "2018-12-01")
        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel)
        val customStats1 = wcStatsStore.getCustomStatsForSite(site)
        assertEquals(StatsGranularity.MONTHS.toString(), customStats1?.unit)
        assertEquals(customDayOrderStatsModel.startDate, customStats1?.startDate)
        assertEquals(customDayOrderStatsModel.endDate, customStats1?.endDate)

        /*
         * For same site, and same stats but different unit
         * */
        val customDayOrderStatsModel2 = WCStatsTestUtils
                .generateSampleStatsModel(unit = StatsGranularity.YEARS.toString(),
                        quantity = "2", endDate = "2019-01-01", startDate = "2018-12-01")
        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel2)
        val customStats2 = wcStatsStore.getCustomStatsForSite(site)
        assertEquals(StatsGranularity.YEARS.toString(), customStats2?.unit)
        assertEquals(customDayOrderStatsModel2.startDate, customStats2?.startDate)
        assertEquals(customDayOrderStatsModel2.endDate, customStats2?.endDate)

        /*
         * For same site, and same unit but different stats
         * */
        val customDayOrderStatsModel3 = WCStatsTestUtils
                .generateSampleStatsModel(unit = StatsGranularity.YEARS.toString(),
                        quantity = "2", endDate = "2019-01-02", startDate = "2018-01-01")
        WCStatsSqlUtils.insertOrUpdateStats(customDayOrderStatsModel3)
        val customStats3 = wcStatsStore.getCustomStatsForSite(site)
        assertEquals(StatsGranularity.YEARS.toString(), customStats3?.unit)
        assertEquals(customDayOrderStatsModel3.startDate, customStats3?.startDate)
        assertEquals(customDayOrderStatsModel3.endDate, customStats3?.endDate)

        /*
         * For different site, but for custom stats
         * */
        val site2 = SiteModel().apply { id = 8 }
        val customStats4 = wcStatsStore.getCustomStatsForSite(site2)
        assertNull(customStats4)
    }

    @Test
    fun testFetchCurrentDayRevenueStatsDate() = runBlocking {
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any())
            ).thenReturn(FetchRevenueStatsResponsePayload(it, DAYS, WCRevenueStatsModel()))
            val startDate = DateUtils.formatDate("yyyy-MM-dd", Date())
            val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, startDate)
            wcStatsStore.fetchRevenueStats(payload)

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd'T'00:00:00")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchRevenueStats(any(), any(),
                    dateArgument.capture(), any(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any())
            ).thenReturn(FetchRevenueStatsResponsePayload(it, DAYS, WCRevenueStatsModel()))
            val startDate = DateUtils.formatDate("yyyy-MM-dd", Date())
            val payload = FetchRevenueStatsPayload(it, StatsGranularity.DAYS, startDate)
            wcStatsStore.fetchRevenueStats(payload)

            val timeOnSite = getCurrentDateTimeForSite(it, "yyyy-MM-dd'T'00:00:00")

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchRevenueStats(any(), any(), dateArgument.capture(), any(),
                    any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        // The two test sites are 24 hours apart, so we are guaranteed to have one site date match the local date,
        // and the other not match it
        val localDate = SimpleDateFormat("yyyy-MM-dd'T'00:00:00").format(Date())
        assertThat(localDate, anyOf(isEqual(plus12SiteDate), isEqual(minus12SiteDate)))
        assertThat(localDate, anyOf(not(plus12SiteDate), not(minus12SiteDate)))
    }

    @Test
    fun testGetRevenueAndOrderStatsForSite() = runBlocking {
        // revenue stats model for current day
        val currentDayStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel()
        val site = SiteModel().apply { id = currentDayStatsModel.localSiteId }
        val currentDayGranularity = StatsGranularity.valueOf(currentDayStatsModel.interval.toUpperCase())
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(
                        FetchRevenueStatsResponsePayload(
                                site,
                                currentDayGranularity,
                                currentDayStatsModel
                        )
                )
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        currentDayGranularity,
                        currentDayStatsModel.startDate,
                        currentDayStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val currentDayRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(currentDayStatsModel.interval.toUpperCase()),
                currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )
        val currentDayOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(currentDayStatsModel.interval.toUpperCase()),
                currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )

        assertTrue(currentDayRevenueStats.isNotEmpty())
        assertTrue(currentDayOrderStats.isNotEmpty())

        // revenue stats model for this week
        val currentWeekStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.WEEKS.toString(), startDate = "2019-07-07", endDate = "2019-07-09"
                )
        val curretnWeekGranularity = StatsGranularity.valueOf(currentWeekStatsModel.interval.toUpperCase())
        val currentWeekPayload = FetchRevenueStatsResponsePayload(
                site, curretnWeekGranularity, currentWeekStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(currentWeekPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        curretnWeekGranularity,
                        currentWeekStatsModel.startDate,
                        currentWeekStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val currentWeekRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(currentWeekStatsModel.interval.toUpperCase()),
                currentWeekStatsModel.startDate, currentWeekStatsModel.endDate
        )
        val currentWeekOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(currentWeekStatsModel.interval.toUpperCase()),
                currentWeekStatsModel.startDate, currentWeekStatsModel.endDate
        )

        assertTrue(currentWeekRevenueStats.isNotEmpty())
        assertTrue(currentWeekOrderStats.isNotEmpty())

        // revenue stats model for this month
        val currentMonthStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.MONTHS.toString(), startDate = "2019-07-01", endDate = "2019-07-09"
                )
        val currentMonthGranularity = StatsGranularity.valueOf(currentMonthStatsModel.interval.toUpperCase())
        val currentMonthPayload = FetchRevenueStatsResponsePayload(
                site, currentMonthGranularity, currentMonthStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(currentMonthPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        currentMonthGranularity,
                        currentMonthStatsModel.startDate,
                        currentMonthStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val currentMonthRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(currentMonthStatsModel.interval.toUpperCase()),
                currentMonthStatsModel.startDate, currentMonthStatsModel.endDate
        )
        val currentMonthOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(currentMonthStatsModel.interval.toUpperCase()),
                currentMonthStatsModel.startDate, currentMonthStatsModel.endDate
        )

        assertTrue(currentMonthRevenueStats.isNotEmpty())
        assertTrue(currentMonthOrderStats.isNotEmpty())

        // current day stats for alternate site
        val site2 = SiteModel().apply { id = 8 }
        val altSiteOrderStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel(
                localSiteId = site2.id, interval = StatsGranularity.DAYS.toString()
        )
        val allSiteCurrentDayGranularity = StatsGranularity.valueOf(altSiteOrderStatsModel.interval.toUpperCase())
        val allSiteCurrentDayPayload = FetchRevenueStatsResponsePayload(
                site, allSiteCurrentDayGranularity, altSiteOrderStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(allSiteCurrentDayPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        allSiteCurrentDayGranularity,
                        altSiteOrderStatsModel.startDate,
                        altSiteOrderStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is not empty
        val altSiteCurrentDayRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.valueOf(altSiteOrderStatsModel.interval.toUpperCase()),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )
        val altSiteCurrentDayOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.valueOf(altSiteOrderStatsModel.interval.toUpperCase()),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )

        assertTrue(altSiteCurrentDayRevenueStats.isNotEmpty())
        assertTrue(altSiteCurrentDayOrderStats.isNotEmpty())

        // non existentSite
        val nonExistentSite = SiteModel().apply { id = 88 }
        val nonExistentSiteGranularity = StatsGranularity.valueOf(altSiteOrderStatsModel.interval)
        val nonExistentPayload = FetchRevenueStatsResponsePayload(
                nonExistentSite, nonExistentSiteGranularity, altSiteOrderStatsModel
        )
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(nonExistentPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        nonExistentSiteGranularity,
                        altSiteOrderStatsModel.startDate,
                        altSiteOrderStatsModel.endDate
                )
        )

        // verify that the revenue stats & order count is empty
        val nonExistentRevenueStats = wcStatsStore.getGrossRevenueStats(
                nonExistentSite, StatsGranularity.valueOf(altSiteOrderStatsModel.interval.toUpperCase()),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )
        val nonExistentOrderStats = wcStatsStore.getOrderCountStats(
                nonExistentSite, StatsGranularity.valueOf(altSiteOrderStatsModel.interval.toUpperCase()),
                altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )

        assertTrue(nonExistentRevenueStats.isEmpty())
        assertTrue(nonExistentOrderStats.isEmpty())

        // missing data
        val missingDataPayload = FetchRevenueStatsResponsePayload(site, StatsGranularity.YEARS, null)
        whenever(mockOrderStatsRestClient.fetchRevenueStats(any(), any(), any(), any(), any(), any()))
                .thenReturn(nonExistentPayload)
        wcStatsStore.fetchRevenueStats(
                FetchRevenueStatsPayload(
                        site,
                        StatsGranularity.YEARS
                )
        )

        // verify that the revenue stats & order count is empty
        val missingRevenueStats = wcStatsStore.getGrossRevenueStats(
                site, StatsGranularity.YEARS, "2019-01-01", "2019-01-07"
        )
        val missingOrderStats = wcStatsStore.getOrderCountStats(
                site, StatsGranularity.YEARS, "2019-01-01", "2019-01-07"
        )
        assertTrue(missingRevenueStats.isEmpty())
        assertTrue(missingOrderStats.isEmpty())
    }

    @Test
    fun testGetVisitorStatsForDaysGranularity() {
        // Test Scenario - 1: Generate default visitor stats i.e. isCustomField - false
        // Get visitor Stats of the same site and granularity and assert not null
        val defaultDayVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel()
        val site = SiteModel().apply { id = defaultDayVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultDayVisitorStatsModel)

        val defaultDayVisitorStats = wcStatsStore.getVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats.isNotEmpty())

        // Test Scenario - 2: Generate default visitor stats with a different date
        // Get visitor of the same site and granularity and assert not null
        val defaultDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                endDate = "2019-03-20"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultDayVisitorStatsModel2)
        val defaultDayVisitorStats2 = wcStatsStore.getVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats2.isNotEmpty())

        // Test Scenario - 3: Generate custom stats for same site i.e. isCustomField - true
        // Get visitor Stats of the same site and granularity and assert not null
        val customDayVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customDayVisitorStatsModel)

        val customDayVisitorStats = wcStatsStore.getVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.endDate, customDayVisitorStatsModel.isCustomField
        )
        assertTrue(customDayVisitorStats.isNotEmpty())

        // Test Scenario - 4: Query for custom visitor stats that is not present in local cache:
        // for same site, same quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2018-12-01", startDate = "2018-12-01"
        )
        val customDayVisitorStats2 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel2.quantity,
                customDayVisitorStatsModel2.endDate, customDayVisitorStatsModel2.isCustomField
        )
        assertTrue(customDayVisitorStats2.isEmpty())

        // Test Scenario - 5: Query for custom visitor stats that is not present in local cache:
        // for same site, different quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customDayVisitorStatsModel3 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2018-12-01", startDate = "2018-12-01"
        )

        val customDayVisitorStats3 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel3.quantity,
                customDayVisitorStatsModel3.endDate, customDayVisitorStatsModel3.isCustomField
        )
        assertTrue(customDayVisitorStats3.isEmpty())

        // Test Scenario - 6: Generate custom visitor stats for same site with different granularity (WEEKS),
        // same date(2019-01-01), same quantity (1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if another query ran for granularity - DAYS, with same date and same quantity: assert null
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(quantity = "1",
                endDate = "2019-01-01", startDate = "2019-01-01",
                unit = OrderStatsApiUnit.fromStatsGranularity(WEEKS).toString())

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customWeekVisitorStatsModel)

        val customWeekVisitorStats = wcStatsStore.getVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats.isNotEmpty())

        val customDayVisitorStats4 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.endDate, customDayVisitorStatsModel.isCustomField
        )
        assertTrue(customDayVisitorStats4.isEmpty())

        // Test Scenario - 7: Generate custom stats for different site(8) with same granularity(WEEKS),
        // same date(2019-01-01), same quantity(1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if scenario 4 is run again it should assert NOT NULL, since the stats is for different sites
        val customWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                localSiteId = 8, unit = OrderStatsApiUnit.fromStatsGranularity(WEEKS).toString(),
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customWeekVisitorStatsModel2)

        val customWeekVisitorStats2 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel2.quantity,
                customWeekVisitorStatsModel2.endDate, customWeekVisitorStatsModel2.isCustomField
        )
        assertTrue(customWeekVisitorStats2.isNotEmpty())
        assertTrue(customWeekVisitorStats.isNotEmpty())
    }

    @Test
    fun testGetVisitorStatsForWeeksGranularity() {
        // Test Scenario - 1: Generate default visitor stats i.e. isCustomField - false
        // Get visitor Stats of the same site and granularity and assert not null
        val defaultWeekVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString()
        )
        val site = SiteModel().apply { id = defaultWeekVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultWeekVisitorStatsModel)

        val defaultWeekVisitorStats = wcStatsStore.getVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats.isNotEmpty())

        // query for days granularity. the visitor stats should be empty
        val defaultDayVisitorStats = wcStatsStore.getVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats.isEmpty())

        // Test Scenario - 2: Generate default visitor stats with a different date
        // Get visitor of the same site and granularity and assert not null
        val defaultWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString(), endDate = "2019-03-20"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(defaultWeekVisitorStatsModel2)
        val defaultWeekVisitorStats2 = wcStatsStore.getVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats2.isNotEmpty())

        // Test Scenario - 3: Generate custom stats for same site i.e. isCustomField - true
        // Get visitor Stats of the same site and granularity and assert not null
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString(), quantity = "1",
                endDate = "2019-01-01", startDate = "2019-01-01"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customWeekVisitorStatsModel)

        val customWeekVisitorStats = wcStatsStore.getVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats.isNotEmpty())

        // Test Scenario - 4: Query for custom visitor stats that is not present in local cache:
        // for same site, same quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString(), quantity = "1",
                endDate = "2018-12-01", startDate = "2018-12-01"
        )
        val customWeekVisitorStats2 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel2.quantity,
                customWeekVisitorStatsModel2.endDate, customWeekVisitorStatsModel2.isCustomField
        )
        assertTrue(customWeekVisitorStats2.isEmpty())

        // Test Scenario - 5: Query for custom visitor stats that is not present in local cache:
        // for same site, different quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customWeekVisitorStatsModel3 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                unit = OrderStatsApiUnit.WEEK.toString(), quantity = "1",
                endDate = "2018-12-01", startDate = "2018-12-01"
        )

        val customWeekVisitorStats3 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel3.quantity,
                customWeekVisitorStatsModel3.endDate, customWeekVisitorStatsModel3.isCustomField
        )
        assertTrue(customWeekVisitorStats3.isEmpty())

        // Test Scenario - 6: Generate custom visitor stats for same site with different granularity (MONTHS),
        // same date(2019-01-01), same quantity (1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if another query ran for granularity - WEEKS, with same date and same quantity: assert null
        val customMonthVisitorStatsModel = WCStatsTestUtils.generateSampleVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01",
                unit = OrderStatsApiUnit.fromStatsGranularity(MONTHS).toString())

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customMonthVisitorStatsModel)

        val customMonthVisitorStats = wcStatsStore.getVisitorStats(
                site, StatsGranularity.MONTHS, customMonthVisitorStatsModel.quantity,
                customMonthVisitorStatsModel.endDate, customMonthVisitorStatsModel.isCustomField
        )
        assertTrue(customMonthVisitorStats.isNotEmpty())

        val customWeekVisitorStats4 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats4.isEmpty())

        // Test Scenario - 7: Generate custom stats for different site(8) with same granularity(MONTHS),
        // same date(2019-01-01), same quantity(1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if scenario 4 is run again it should assert NOT NULL, since the stats is for different sites
        val customMonthVisitorStatsModel2 = WCStatsTestUtils.generateSampleVisitorStatsModel(
                localSiteId = 8, unit = OrderStatsApiUnit.fromStatsGranularity(MONTHS).toString(),
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateVisitorStats(customMonthVisitorStatsModel2)

        val customMonthVisitorStats2 = wcStatsStore.getVisitorStats(
                site, StatsGranularity.MONTHS, customMonthVisitorStatsModel2.quantity,
                customMonthVisitorStatsModel2.endDate, customMonthVisitorStatsModel2.isCustomField
        )
        assertTrue(customMonthVisitorStats2.isNotEmpty())
        assertTrue(customMonthVisitorStats.isNotEmpty())
    }

    @Test
    fun testGetVisitorStatsForCurrentDayGranularity() {
        // Test Scenario - 1: Generate default visitor stats i.e. isCustomField - false
        // Get visitor Stats of the same site and granularity and assert not null
        val defaultDayVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel()
        val site = SiteModel().apply { id = defaultDayVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultDayVisitorStatsModel)

        val defaultDayVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats.isNotEmpty())

        // Test Scenario - 2: Generate default visitor stats with a different date
        // Get visitor of the same site and granularity and assert not null
        val defaultDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                endDate = "2019-08-02"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultDayVisitorStatsModel2)
        val defaultDayVisitorStats2 = wcStatsStore.getNewVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats2.isNotEmpty())

        // Test Scenario - 3: Generate custom stats for same site i.e. isCustomField - true
        // Get visitor Stats of the same site and granularity and assert not null
        val customDayVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-08-06", startDate = "2019-08-06"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customDayVisitorStatsModel)

        val customDayVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.endDate, customDayVisitorStatsModel.isCustomField
        )
        assertTrue(customDayVisitorStats.isNotEmpty())

        // Test Scenario - 4: Query for custom visitor stats that is not present in local cache:
        // for same site, same quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customDayVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )
        val customDayVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel2.quantity,
                customDayVisitorStatsModel2.endDate, customDayVisitorStatsModel2.isCustomField
        )
        assertTrue(customDayVisitorStats2.isEmpty())

        // Test Scenario - 5: Query for custom visitor stats that is not present in local cache:
        // for same site, different quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customDayVisitorStatsModel3 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-01-01", startDate = "2019-01-01"
        )

        val customDayVisitorStats3 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel3.quantity,
                customDayVisitorStatsModel3.endDate, customDayVisitorStatsModel3.isCustomField
        )
        assertTrue(customDayVisitorStats3.isEmpty())

        // Test Scenario - 6: Generate custom visitor stats for same site with different granularity (WEEKS),
        // same date(2019-01-01), same quantity (1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if another query ran for granularity - DAYS, with same date and same quantity: assert null
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1",
                endDate = "2019-02-01", startDate = "2019-02-01",
                granularity = StatsGranularity.WEEKS.toString()
        )

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customWeekVisitorStatsModel)

        val customWeekVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats.isNotEmpty())

        val customDayVisitorStats4 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.DAYS, customDayVisitorStatsModel.quantity,
                customDayVisitorStatsModel.endDate, customDayVisitorStatsModel.isCustomField
        )
        assertTrue(customDayVisitorStats4.isEmpty())

        // Test Scenario - 7: Generate custom stats for different site(8) with same granularity(WEEKS),
        // same date(2019-01-01), same quantity(1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if scenario 4 is run again it should assert NOT NULL, since the stats is for different sites
        val customWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                localSiteId = 8, granularity = StatsGranularity.WEEKS.toString(),
                quantity = "1", endDate = "2019-02-01", startDate = "2019-02-01"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customWeekVisitorStatsModel2)

        val customWeekVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel2.quantity,
                customWeekVisitorStatsModel2.endDate, customWeekVisitorStatsModel2.isCustomField
        )
        assertTrue(customWeekVisitorStats2.isNotEmpty())
        assertTrue(customWeekVisitorStats.isNotEmpty())
    }

    @Test
    fun testGetVisitorStatsForThisWeekGranularity() {
        // Test Scenario - 1: Generate default visitor stats i.e. isCustomField - false
        // Get visitor Stats of the same site and granularity and assert not null
        val defaultWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString()
        )
        val site = SiteModel().apply { id = defaultWeekVisitorStatsModel.localSiteId }
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultWeekVisitorStatsModel)

        val defaultWeekVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats.isNotEmpty())

        // query for days granularity. the visitor stats should be empty
        val defaultDayVisitorStats = wcStatsStore.getNewVisitorStats(site, StatsGranularity.DAYS)
        assertTrue(defaultDayVisitorStats.isEmpty())

        // Test Scenario - 2: Generate default visitor stats with a different date
        // Get visitor of the same site and granularity and assert not null
        val defaultWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), endDate = "2019-03-20"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(defaultWeekVisitorStatsModel2)
        val defaultWeekVisitorStats2 = wcStatsStore.getNewVisitorStats(site, StatsGranularity.WEEKS)
        assertTrue(defaultWeekVisitorStats2.isNotEmpty())

        // Test Scenario - 3: Generate custom stats for same site i.e. isCustomField - true
        // Get visitor Stats of the same site and granularity and assert not null
        val customWeekVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), quantity = "1",
                endDate = "2019-08-01", startDate = "2019-08-01"
        )
        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customWeekVisitorStatsModel)

        val customWeekVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats.isNotEmpty())

        // Test Scenario - 4: Query for custom visitor stats that is not present in local cache:
        // for same site, same quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customWeekVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), quantity = "1",
                endDate = "2019-07-01", startDate = "2019-07-01"
        )
        val customWeekVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel2.quantity,
                customWeekVisitorStatsModel2.endDate, customWeekVisitorStatsModel2.isCustomField
        )
        assertTrue(customWeekVisitorStats2.isEmpty())

        // Test Scenario - 5: Query for custom visitor stats that is not present in local cache:
        // for same site, different quantity, different date
        // Get visitor Stats of the same site and granularity and assert null
        val customWeekVisitorStatsModel3 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                granularity = StatsGranularity.WEEKS.toString(), quantity = "1",
                endDate = "2019-07-01", startDate = "2019-07-01"
        )

        val customWeekVisitorStats3 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel3.quantity,
                customWeekVisitorStatsModel3.endDate, customWeekVisitorStatsModel3.isCustomField
        )
        assertTrue(customWeekVisitorStats3.isEmpty())

        // Test Scenario - 6: Generate custom visitor stats for same site with different granularity (MONTHS),
        // same date(2019-01-01), same quantity (1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if another query ran for granularity - WEEKS, with same date and same quantity: assert null
        val customMonthVisitorStatsModel = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                quantity = "1", endDate = "2019-08-01", startDate = "2019-08-01",
                granularity = StatsGranularity.MONTHS.toString())

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customMonthVisitorStatsModel)

        val customMonthVisitorStats = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.MONTHS, customMonthVisitorStatsModel.quantity,
                customMonthVisitorStatsModel.endDate, customMonthVisitorStatsModel.isCustomField
        )
        assertTrue(customMonthVisitorStats.isNotEmpty())

        val customWeekVisitorStats4 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.WEEKS, customWeekVisitorStatsModel.quantity,
                customWeekVisitorStatsModel.endDate, customWeekVisitorStatsModel.isCustomField
        )
        assertTrue(customWeekVisitorStats4.isEmpty())

        // Test Scenario - 7: Generate custom stats for different site(8) with same granularity(MONTHS),
        // same date(2019-01-01), same quantity(1) i.e. isCustomField - true
        // Get visitor Stats and assert Not Null
        // Now if scenario 4 is run again it should assert NOT NULL, since the stats is for different sites
        val customMonthVisitorStatsModel2 = WCStatsTestUtils.generateSampleNewVisitorStatsModel(
                localSiteId = 8, granularity = StatsGranularity.MONTHS.toString(),
                quantity = "1", endDate = "2019-08-01", startDate = "2019-08-01"
        )

        WCVisitorStatsSqlUtils.insertOrUpdateNewVisitorStats(customMonthVisitorStatsModel2)

        val customMonthVisitorStats2 = wcStatsStore.getNewVisitorStats(
                site, StatsGranularity.MONTHS, customMonthVisitorStatsModel2.quantity,
                customMonthVisitorStatsModel2.endDate, customMonthVisitorStatsModel2.isCustomField
        )
        assertTrue(customMonthVisitorStats2.isNotEmpty())
        assertTrue(customMonthVisitorStats.isNotEmpty())
    }
}
