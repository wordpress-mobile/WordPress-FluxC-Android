package org.wordpress.android.fluxc.wc.leaderboards

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsProductApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleTopPerformerApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCLeaderboardsStoreTest {
    private val leaderboardsRestClient: LeaderboardsRestClient = mock()
    private val reportsRestClient: ReportsRestClient = mock()
    private var mapper = WCProductLeaderboardsMapper()
    private val topPerformersDao: TopPerformerProductsDao = mock()

    private lateinit var storeUnderTest: WCLeaderboardsStore

    fun setup(prepareMocks: () -> Unit = {}) {
        prepareMocks()
        createStoreUnderTest()
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
            appContext,
            listOf(
                SiteModel::class.java,
                WCTopPerformerProductModel::class.java,
                WCProductModel::class.java
            ),
            WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `fetch top performer products with empty result should return WooError`() = test {
        givenFetchTopPerformerReturns(emptyArray())
        setup()

        val result = storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch top performer products should return mapped top performer entities correctly`() =
        test {
            setup()
            val response = generateSampleTopPerformerApiResponse()
            givenFetchTopPerformerReturns(response)

            val topPerformerProducts = storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)
            val result = setMillisSinceLastUpdated(topPerformerProducts, DEFAULT_MILLIS_SINCE_LAST_UPDATE)

            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(TOP_PERFORMER_ENTITY_LIST)
            assertThat(result.error).isNull()
        }

    @Test
    fun `fetching top performer products should update database with new data`() =
        test {
            setup()
            val response = generateSampleTopPerformerApiResponse()
            givenFetchTopPerformerReturns(response)

            storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

            verify(topPerformersDao, times(1))
                .updateTopPerformerProductsFor(
                    eq(stubSite.localId()),
                    eq(DAYS.datePeriod(stubSite)),
                    any()
                )
        }

    @Test
    fun `invalidating top performer products should update database`() =
        test {
            setup()
            givenCachedTopPerformers()

            storeUnderTest.invalidateTopPerformers(stubSite)

            verify(topPerformersDao, times(1)).getTopPerformerProductsForSite(stubSite.localId())
            verify(topPerformersDao).updateTopPerformerProductsForSite(eq(stubSite.localId()), any())
        }

    private suspend fun givenCachedTopPerformers() {
        whenever(
            topPerformersDao.getTopPerformerProductsForSite(stubSite.localId())
        ).thenReturn(TOP_PERFORMER_ENTITY_LIST)
    }

    private suspend fun givenFetchTopPerformerReturns(response: Array<ReportsProductApiResponse>?) {
        whenever(
            reportsRestClient.fetchTopPerformerProducts(
                site = any(),
                startDate = any(),
                endDate = any(),
                quantity = anyOrNull()
            )
        ).thenReturn(WooPayload(response))
    }

    private fun setMillisSinceLastUpdated(
        result: WooResult<List<TopPerformerProductEntity>>,
        millis: Long
    ): WooResult<List<TopPerformerProductEntity>> {
        val updatedList = result.model?.map { entity ->
            entity.copy(millisSinceLastUpdated = millis)
        }
        return WooResult(updatedList)
    }

    private fun createStoreUnderTest() {
        storeUnderTest = WCLeaderboardsStore(
            leaderboardsRestClient,
            mapper,
            initCoroutineEngine(),
            topPerformersDao,
            reportsRestClient
        )
    }

    companion object {
        const val DEFAULT_MILLIS_SINCE_LAST_UPDATE = 100L
        val TOP_PERFORMER_ENTITY_LIST =
            listOf(
                TopPerformerProductEntity(
                    localSiteId = stubSite.localId(),
                    datePeriod = DAYS.datePeriod(stubSite),
                    productId = RemoteId(14),
                    name = "Polo",
                    imageUrl = "https://superlativecentaur.wpcomstaging.com/wp-content/uploads/2023/01/polo-2.jpg",
                    quantity = 25,
                    currency = "",
                    total = 506.0000000000001,
                    millisSinceLastUpdated = DEFAULT_MILLIS_SINCE_LAST_UPDATE
                ),
                TopPerformerProductEntity(
                    localSiteId = stubSite.localId(),
                    datePeriod = DAYS.datePeriod(stubSite),
                    productId = RemoteId(22),
                    name = "Sunglasses Subscription",
                    imageUrl = "https://superlativecentaur.wpcomstaging.com/wp-content/uploads/2023/01/sunglasses-2.jpg",
                    quantity = 25,
                    currency = "",
                    total = 1374.0,
                    millisSinceLastUpdated = DEFAULT_MILLIS_SINCE_LAST_UPDATE
                ),
                TopPerformerProductEntity(
                    localSiteId = stubSite.localId(),
                    datePeriod = DAYS.datePeriod(stubSite),
                    productId = RemoteId(15),
                    name = "Beanie",
                    imageUrl = "https://superlativecentaur.wpcomstaging.com/wp-content/uploads/2023/01/beanie-2.jpg",
                    quantity = 21,
                    currency = "",
                    total = 385.0,
                    millisSinceLastUpdated = DEFAULT_MILLIS_SINCE_LAST_UPDATE
                )
            )
    }
}
