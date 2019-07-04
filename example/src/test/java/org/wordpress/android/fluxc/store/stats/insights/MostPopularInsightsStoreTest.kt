package org.wordpress.android.fluxc.store.stats.insights

import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.fluxc.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient.MostPopularResponse
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.MostPopularSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.store.stats.MOST_POPULAR_RESPONSE
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MostPopularInsightsStoreTest : BaseUnitTest() {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var restClient: MostPopularRestClient
    @Mock lateinit var sqlUtils: MostPopularSqlUtils
    @Mock lateinit var mapper: InsightsMapper
    private val liveMostPopularResponse = MutableLiveData<MostPopularResponse>()
    private lateinit var store: MostPopularInsightsStore
    @Before
    fun setUp() {
        store = MostPopularInsightsStore(
                restClient,
                sqlUtils,
                mapper,
                Dispatchers.Unconfined
        )
    }

    @Test
    fun `returns most popular insights per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val responseModel = store.fetchMostPopularInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns error when most popular insights call fail`() = test {
        val type = API_ERROR
        val message = "message"
        val errorPayload = FetchStatsPayload<MostPopularResponse>(StatsError(type, message))
        val forced = true
        whenever(restClient.fetchMostPopularInsights(site, forced)).thenReturn(errorPayload)

        val responseModel = store.fetchMostPopularInsights(site, forced)

        assertNotNull(responseModel.error)
        val error = responseModel.error!!
        assertEquals(type, error.type)
        assertEquals(message, error.message)
    }

    @Test
    fun `returns most popular insights from db`() {
        whenever(sqlUtils.select(site)).thenReturn(MOST_POPULAR_RESPONSE)
        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val result = store.getMostPopularInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns live most popular insights from db`() {
        whenever(sqlUtils.liveSelect(site)).thenReturn(liveMostPopularResponse)

        val model = mock<InsightsMostPopularModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE, site)).thenReturn(model)

        val liveData = store.liveMostPopularInsights(site)

        var result: InsightsMostPopularModel? = null
        liveData.observeForever {
            result = it
        }
        liveMostPopularResponse.value = MOST_POPULAR_RESPONSE

        assertThat(result).isNotNull
        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns years insights per site`() = test {
        val fetchInsightsPayload = FetchStatsPayload(
                MOST_POPULAR_RESPONSE
        )
        val forced = true
        whenever(restClient.fetchMostPopularInsights(site, forced)).thenReturn(fetchInsightsPayload)
        val model = mock<YearsInsightsModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE)).thenReturn(model)

        val responseModel = store.fetchYearsInsights(site, forced)

        assertThat(responseModel.model).isEqualTo(model)
        verify(sqlUtils).insert(site, MOST_POPULAR_RESPONSE)
    }

    @Test
    fun `returns years insights from db`() {
        whenever(sqlUtils.select(site)).thenReturn(MOST_POPULAR_RESPONSE)
        val model = mock<YearsInsightsModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE)).thenReturn(model)

        val result = store.getYearsInsights(site)

        assertThat(result).isEqualTo(model)
    }

    @Test
    fun `returns live years insights from db`() {
        whenever(sqlUtils.liveSelect(site)).thenReturn(liveMostPopularResponse)

        val model = mock<YearsInsightsModel>()
        whenever(mapper.map(MOST_POPULAR_RESPONSE)).thenReturn(model)

        val liveData = store.liveYearsInsights(site)

        var result: YearsInsightsModel? = null
        liveData.observeForever {
            result = it
        }
        liveMostPopularResponse.value = MOST_POPULAR_RESPONSE

        assertThat(result).isNotNull
        assertThat(result).isEqualTo(model)
    }
}
