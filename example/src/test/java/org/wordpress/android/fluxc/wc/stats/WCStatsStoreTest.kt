package org.wordpress.android.fluxc.wc.stats

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
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
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.fluxc.utils.SiteUtils.getCurrentDateTimeForSite
import org.wordpress.android.fluxc.utils.SiteUtils.getDateTimeForSite
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


    @Test
    fun testGetQuantityForDays() {
        val quantity1 = wcStatsStore.getQuantityByGranularity("2018-01-25", "2018-01-28", StatsGranularity.DAYS, 30)
        assertEquals(4, quantity1)

        val quantity2 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2018-01-01", StatsGranularity.DAYS, 30)
        assertEquals(1, quantity2)

        val quantity3 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2018-01-31", StatsGranularity.DAYS, 30)
        assertEquals(31, quantity3)

        val quantity4 = wcStatsStore.getQuantityByGranularity("2018-01-28", "2018-01-25", StatsGranularity.DAYS, 30)
        assertEquals(4, quantity4)

        val quantity5 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2018-01-01", StatsGranularity.DAYS, 30)
        assertEquals(1, quantity5)

        val quantity6 = wcStatsStore.getQuantityByGranularity("2018-01-31", "2018-01-01", StatsGranularity.DAYS, 30)
        assertEquals(31, quantity6)

        val defaultQuantity1 = wcStatsStore.getQuantityByGranularity("", "", StatsGranularity.DAYS, 30)
        assertEquals(30, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByGranularity(null, null, StatsGranularity.DAYS, 30)
        assertEquals(30, defaultQuantity2)
    }


    @Test
    fun testGetQuantityForWeeks() {
        val quantity1 = wcStatsStore.getQuantityByGranularity("2018-10-22", "2018-10-23", StatsGranularity.WEEKS, 17)
        assertEquals(1, quantity1)

        val quantity2 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.WEEKS, 17)
        assertEquals(53, quantity2)

        val quantity3 = wcStatsStore.getQuantityByGranularity("2019-01-20", "2019-01-13", StatsGranularity.WEEKS, 17)
        assertEquals(2, quantity3)

        val quantity4 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-03-01", StatsGranularity.WEEKS, 17)
        assertEquals(61, quantity4)

        val quantity5 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2018-01-31", StatsGranularity.WEEKS, 17)
        assertEquals(5, quantity5)

        val quantity6 = wcStatsStore.getQuantityByGranularity("2018-12-01", "2018-12-31", StatsGranularity.WEEKS, 17)
        assertEquals(6, quantity6)

        val quantity7 = wcStatsStore.getQuantityByGranularity("2018-11-01", "2018-11-30", StatsGranularity.WEEKS, 17)
        assertEquals(5, quantity7)


        val inverseQuantity1 = wcStatsStore.getQuantityByGranularity("2018-10-23", "2018-10-22", StatsGranularity.WEEKS, 17)
        assertEquals(1, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2017-01-01", StatsGranularity.WEEKS, 17)
        assertEquals(53, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getQuantityByGranularity("2019-01-13", "2019-01-20", StatsGranularity.WEEKS, 17)
        assertEquals(2, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getQuantityByGranularity("2018-03-01", "2017-01-01", StatsGranularity.WEEKS, 17)
        assertEquals(61, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getQuantityByGranularity("2018-01-31", "2018-01-01", StatsGranularity.WEEKS, 17)
        assertEquals(5, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getQuantityByGranularity("2018-12-31", "2018-12-01", StatsGranularity.WEEKS, 17)
        assertEquals(6, inverseQuantity6)

        val inverseQuantity7 = wcStatsStore.getQuantityByGranularity("2018-11-30", "2018-11-01", StatsGranularity.WEEKS, 17)
        assertEquals(5, inverseQuantity7)

        val defaultQuantity1 = wcStatsStore.getQuantityByGranularity("", "", StatsGranularity.WEEKS, 17)
        assertEquals(17, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByGranularity(null, null, StatsGranularity.WEEKS, 17)
        assertEquals(17, defaultQuantity2)
    }


    @Test
    fun testGetQuantityForMonths() {
        val quantity1 = wcStatsStore.getQuantityByGranularity("2018-10-22", "2018-10-23", StatsGranularity.MONTHS, 12)
        assertEquals(1, quantity1)

        val quantity2 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(13, quantity2)

        val quantity3 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2018-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(1, quantity3)

        val quantity4 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-03-01", StatsGranularity.MONTHS, 12)
        assertEquals(15, quantity4)

        val quantity5 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-01-31", StatsGranularity.MONTHS, 12)
        assertEquals(13, quantity5)

        val quantity6 = wcStatsStore.getQuantityByGranularity("2018-12-31", "2019-01-01", StatsGranularity.MONTHS, 1)
        assertEquals(2, quantity6)

        val inverseQuantity1 = wcStatsStore.getQuantityByGranularity("2018-10-23", "2018-10-22", StatsGranularity.MONTHS, 12)
        assertEquals(1, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2017-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(13, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2018-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(1, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getQuantityByGranularity("2018-03-01", "2017-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(15, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getQuantityByGranularity("2018-01-31", "2017-01-01", StatsGranularity.MONTHS, 12)
        assertEquals(13, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getQuantityByGranularity("2019-01-01", "2018-12-31", StatsGranularity.MONTHS, 1)
        assertEquals(2, inverseQuantity6)

        val defaultQuantity1 = wcStatsStore.getQuantityByGranularity("", "", StatsGranularity.MONTHS, 12)
        assertEquals(12, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByGranularity(null, null, StatsGranularity.MONTHS, 12)
        assertEquals(12, defaultQuantity2)
    }


    @Test
    fun testGetQuantityForYears() {
        val quantity1 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-01-01", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity1)

        val quantity2 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-03-01", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity2)

        val quantity3 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2018-01-05", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity3)

        val quantity4 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2019-03-01", StatsGranularity.YEARS, 1)
        assertEquals(3, quantity4)

        val quantity5 = wcStatsStore.getQuantityByGranularity("2015-03-05", "2017-01-01", StatsGranularity.YEARS, 1)
        assertEquals(3, quantity5)

        val quantity6 = wcStatsStore.getQuantityByGranularity("2018-12-31", "2019-01-01", StatsGranularity.YEARS, 1)
        assertEquals(2, quantity6)

        val quantity7 = wcStatsStore.getQuantityByGranularity("2019-01-25", "2019-01-25", StatsGranularity.YEARS, 1)
        assertEquals(1, quantity7)


        val inverseQuantity1 = wcStatsStore.getQuantityByGranularity("2018-01-01", "2017-01-01", StatsGranularity.YEARS, 1)
        assertEquals(2, inverseQuantity1)

        val inverseQuantity2 = wcStatsStore.getQuantityByGranularity("2018-03-01", "2017-01-01", StatsGranularity.YEARS, 1)
        assertEquals(2, inverseQuantity2)

        val inverseQuantity3 = wcStatsStore.getQuantityByGranularity("2018-01-05", "2017-01-01", StatsGranularity.YEARS, 1)
        assertEquals(2, inverseQuantity3)

        val inverseQuantity4 = wcStatsStore.getQuantityByGranularity("2019-03-01", "2017-01-01", StatsGranularity.YEARS, 1)
        assertEquals(3, inverseQuantity4)

        val inverseQuantity5 = wcStatsStore.getQuantityByGranularity("2017-01-01", "2015-03-05", StatsGranularity.YEARS, 1)
        assertEquals(3, inverseQuantity5)

        val inverseQuantity6 = wcStatsStore.getQuantityByGranularity("2019-01-01", "2018-12-31", StatsGranularity.YEARS, 1)
        assertEquals(2, inverseQuantity6)


        val defaultQuantity1 = wcStatsStore.getQuantityByGranularity("", "", StatsGranularity.YEARS, 1)
        assertEquals(1, defaultQuantity1)

        val defaultQuantity2 = wcStatsStore.getQuantityByGranularity(null, null, StatsGranularity.YEARS, 1)
        assertEquals(1, defaultQuantity2)
    }

    @Test
    fun testFetchOrderStatsForDaysDate() {
        val startDate = "2019-01-01"
        val endDate = "2019-01-01"
        val plus12SiteDate = SiteModel().apply { timezone = "12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getDateTimeForSite(it, "yyyy-MM-dd", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.DAYS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getDateTimeForSite(it, "yyyy-MM-dd", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = SiteUtils.formatDate("YYYY-MM-dd", SiteUtils.getDateFromString(endDate))
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

            val timeOnSite = getDateTimeForSite(it, "yyyy-'W'ww", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.WEEKS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getDateTimeForSite(it, "yyyy-'W'ww", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = SiteUtils.formatDate("yyyy-'W'ww", SiteUtils.getDateFromString(endDate))

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

            val timeOnSite = getDateTimeForSite(it, "yyyy-MM", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.MONTHS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getDateTimeForSite(it, "yyyy-MM", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = SiteUtils.formatDate("yyyy-MM", SiteUtils.getDateFromString(endDate))

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

            val timeOnSite = getDateTimeForSite(it, "yyyy", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        reset(mockOrderStatsRestClient)

        val minus12SiteDate = SiteModel().apply { timezone = "-12" }.let {
            val payload = FetchOrderStatsPayload(it, StatsGranularity.YEARS, startDate, endDate)
            wcStatsStore.onAction(WCStatsActionBuilder.newFetchOrderStatsAction(payload))

            val timeOnSite = getDateTimeForSite(it, "yyyy", endDate)

            // The date value passed to the network client should match the current date on the site
            val dateArgument = argumentCaptor<String>()
            verify(mockOrderStatsRestClient).fetchStats(any(), any(), dateArgument.capture(), any(), any())
            val siteDate = dateArgument.firstValue
            assertEquals(timeOnSite, siteDate)
            return@let siteDate
        }

        val localDate = SiteUtils.formatDate("yyyy", SiteUtils.getDateFromString(endDate))

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

        val quantity: Long = wcStatsStore.getQuantityByGranularity(startDate, endDate, StatsGranularity.DAYS, 30)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient).fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any())
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

        val quantity: Long = wcStatsStore.getQuantityByGranularity(startDate, endDate, StatsGranularity.WEEKS, 17)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient).fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any())
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

        val quantity: Long = wcStatsStore.getQuantityByGranularity(startDate, endDate, StatsGranularity.MONTHS, 12)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient).fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any())
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

        val quantity: Long = wcStatsStore.getQuantityByGranularity(startDate, endDate, StatsGranularity.YEARS, 12)

        val quantityArgument = argumentCaptor<Long>()
        verify(mockOrderStatsRestClient).fetchStats(any(), any(), any(), quantityArgument.capture().toInt(), any())
        val calculatedQuantity: Long = quantityArgument.firstValue
        assertEquals(quantity, calculatedQuantity)
    }
}
