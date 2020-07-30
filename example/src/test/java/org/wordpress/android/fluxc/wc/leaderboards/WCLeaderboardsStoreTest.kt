package org.wordpress.android.fluxc.wc.leaderboards

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleShippingLabelApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubbedTopPerformersList

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCLeaderboardsStoreTest {
    private lateinit var storeUnderTest: WCLeaderboardsStore
    private lateinit var restClient: LeaderboardsRestClient
    private lateinit var productStore: WCProductStore
    private lateinit var mapper: WCProductLeaderboardsMapper

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCTopPerformerProductModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
        initMocks()
        createStoreUnderTest()
    }

    @Test
    fun `fetch product leaderboards with empty result should return WooError`() = test {
        whenever(restClient.fetchLeaderboards(stubSite, null, null, null))
                .thenReturn(WooPayload(emptyArray()))
        val result = storeUnderTest.fetchProductLeaderboards(stubSite)
        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    @Test
    fun `fetch product leaderboards should filter leaderboards by PRODUCTS type`() = test {
        mapper = spy()
        createStoreUnderTest()
        val response = generateSampleShippingLabelApiResponse()
        val filteredResponse = response?.firstOrNull { it.type == PRODUCTS }

        whenever(restClient.fetchLeaderboards(stubSite, null, null, null))
                .thenReturn(WooPayload(response))
        storeUnderTest.fetchProductLeaderboards(stubSite)
        verify(mapper).map(filteredResponse!!, stubSite, productStore)
    }

    @Test
    fun `fetch product leaderboards should call mapper once`() = test {
        mapper = spy()
        createStoreUnderTest()
        val response = generateSampleShippingLabelApiResponse()
        whenever(restClient.fetchLeaderboards(stubSite, null, null, null))
                .thenReturn(WooPayload(response))
        storeUnderTest.fetchProductLeaderboards(stubSite)
        verify(mapper, times(1)).map(any(), any(), any())
    }

    @Test
    fun `fetch product leaderboards should return WooResult correctly`() = test {
        val response = generateSampleShippingLabelApiResponse()
        val filteredResponse = response?.firstOrNull { it.type == PRODUCTS }
        whenever(restClient.fetchLeaderboards(stubSite, null, null, null))
                .thenReturn(WooPayload(response))
        whenever(mapper.map(filteredResponse!!, stubSite, productStore)).thenReturn(stubbedTopPerformersList)
        val result = storeUnderTest.fetchProductLeaderboards(stubSite)
        assertThat(result.model).isNotNull
        assertThat(result.model).isEqualTo(stubbedTopPerformersList)
        assertThat(result.error).isNull()
    }

    @Test
    fun `fetch product leaderboards from a invalid site ID should return WooResult with error`() = test {
        val response = generateSampleShippingLabelApiResponse()
        val filteredResponse = response?.firstOrNull { it.type == PRODUCTS }
        whenever(restClient.fetchLeaderboards(stubSite, null, null, null))
                .thenReturn(WooPayload(response))
        whenever(mapper.map(filteredResponse!!, SiteModel().apply { id = 100 }, productStore)).thenReturn(stubbedTopPerformersList)
        val result = storeUnderTest.fetchProductLeaderboards(stubSite)
        assertThat(result.model).isNull()
        assertThat(result.error).isNotNull
    }

    private fun initMocks() {
        restClient = mock()
        productStore = mock()
        mapper = mock()
    }

    private fun createStoreUnderTest() =
            WCLeaderboardsStore(
                    restClient,
                    productStore,
                    mapper,
                    initCoroutineEngine()
            ).apply { storeUnderTest = this }
}
