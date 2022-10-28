package org.wordpress.android.fluxc.wc.leaderboards

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleLeaderboardsApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCLeaderboardsStoreTest {
    private val restClient: LeaderboardsRestClient = mock()
    private val productStore: WCProductStore = mock()
    private var mapper: WCProductLeaderboardsMapper = mock()
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
        givenFetchLeaderBoardsReturns(emptyArray())
        setup()

        val result = storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch top performer products should filter leaderboards by PRODUCTS type`() = test {
        setup { mapper = spy() }
        val response = generateSampleLeaderboardsApiResponse()
        givenFetchLeaderBoardsReturns(response)

        storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

        verify(mapper).mapTopPerformerProductsEntity(
            response?.firstOrNull { it.type == PRODUCTS }!!,
            stubSite,
            productStore,
            DAYS.datePeriod(stubSite)
        )
    }

    @Test
    fun `fetch top performer products should call mapper once`() = test {
        setup()
        val response = generateSampleLeaderboardsApiResponse()
        givenFetchLeaderBoardsReturns(response)

        storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

        verify(mapper, times(1)).mapTopPerformerProductsEntity(any(), any(), any(), any())
    }

    @Test
    fun `fetch top performer products should return mapped top performer entities correctly`() =
        test {
            setup()
            val response = generateSampleLeaderboardsApiResponse()
            givenFetchLeaderBoardsReturns(response)
            givenTopPerformersMapperReturns(
                givenResponse = response?.firstOrNull { it.type == PRODUCTS }!!,
                returnedTopPerformersList = TOP_PERFORMER_ENTITY_LIST
            )

            val result = storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

            assertThat(result.model).isNotNull
            assertThat(result.model).isEqualTo(TOP_PERFORMER_ENTITY_LIST)
            assertThat(result.error).isNull()
        }

    @Test
    fun `fetch top performer products from a invalid site ID should return WooResult with error`() =
        test {
            setup()
            val response = generateSampleLeaderboardsApiResponse()
            givenFetchLeaderBoardsReturns(response)
            givenTopPerformersMapperReturns(
                givenResponse = response?.firstOrNull { it.type == PRODUCTS }!!,
                returnedTopPerformersList = TOP_PERFORMER_ENTITY_LIST,
                SiteModel().apply { id = 100 },
            )

            val result = storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

            assertThat(result.model).isNull()
            assertThat(result.error).isNotNull
        }

    @Test
    fun `fetching top performer products should update database with new data`() =
        test {
            setup()
            val response = generateSampleLeaderboardsApiResponse()
            givenFetchLeaderBoardsReturns(response)
            givenTopPerformersMapperReturns(
                givenResponse = response?.firstOrNull { it.type == PRODUCTS }!!,
                returnedTopPerformersList = TOP_PERFORMER_ENTITY_LIST
            )

            storeUnderTest.fetchTopPerformerProducts(stubSite, DAYS)

            verify(topPerformersDao, times(1))
                .updateTopPerformerProductsFor(
                    stubSite.siteId,
                    DAYS.datePeriod(stubSite),
                    TOP_PERFORMER_ENTITY_LIST
                )
        }

    @Test
    fun `invalidating top performer products should update database`() =
        test {
            setup()
            givenCachedTopPerformers()

            storeUnderTest.invalidateTopPerformers(stubSite.siteId)

            verify(topPerformersDao, times(1))
                .getTopPerformerProductsForSite(stubSite.siteId)
            verify(topPerformersDao, times(1))
                .updateTopPerformerProductsForSite(
                    stubSite.siteId,
                    INVALIDATED_TOP_PERFORMER_ENTITY_LIST
                )
        }

    private suspend fun givenCachedTopPerformers() {
        whenever(
            topPerformersDao.getTopPerformerProductsForSite(stubSite.siteId)
        ).thenReturn(TOP_PERFORMER_ENTITY_LIST)
    }

    private suspend fun givenFetchLeaderBoardsReturns(response: Array<LeaderboardsApiResponse>?) {
        whenever(
            restClient.fetchLeaderboards(
                site = any(),
                startDate = any(),
                endDate = any(),
                quantity = anyOrNull(),
                forceRefresh = any(),
                interval = any(),
                addProductsPath = any(),
            )
        ).thenReturn(WooPayload(response))
    }

    private fun createStoreUnderTest() {
        storeUnderTest = WCLeaderboardsStore(
            restClient,
            productStore,
            mapper,
            initCoroutineEngine(),
            topPerformersDao
        )
    }

    private suspend fun givenTopPerformersMapperReturns(
        givenResponse: LeaderboardsApiResponse,
        returnedTopPerformersList: List<TopPerformerProductEntity>,
        siteModel: SiteModel = stubSite
    ) {
        whenever(
            mapper.mapTopPerformerProductsEntity(
                givenResponse,
                siteModel,
                productStore,
                DAYS.datePeriod(siteModel)
            )
        ).thenReturn(returnedTopPerformersList)
    }

    companion object {
        val TOP_PERFORMER_ENTITY_LIST =
            listOf(
                TopPerformerProductEntity(
                    siteId = 1,
                    datePeriod = DAYS.datePeriod(stubSite),
                    productId = 123,
                    name = "product",
                    imageUrl = null,
                    quantity = 5,
                    currency = "USD",
                    total = 10.5,
                    millisSinceLastUpdated = 100
                )
            )
        val INVALIDATED_TOP_PERFORMER_ENTITY_LIST =
            listOf(
                TopPerformerProductEntity(
                    siteId = 1,
                    datePeriod = DAYS.datePeriod(stubSite),
                    productId = 123,
                    name = "product",
                    imageUrl = null,
                    quantity = 5,
                    currency = "USD",
                    total = 10.5,
                    millisSinceLastUpdated = 0
                )
            )
    }
}
