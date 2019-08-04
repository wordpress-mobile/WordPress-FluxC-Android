package org.wordpress.android.fluxc.wc.refunds

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundModel
import org.wordpress.android.fluxc.model.refunds.RefundsMapper
import org.wordpress.android.fluxc.model.stats.time.ClicksModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.RefundsSqlUtils
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.RefundsStore.RefundsPayload
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(MockitoJUnitRunner::class)
class RefundsStoreTest {
    @Mock lateinit var restClient: RefundsRestClient
    @Mock lateinit var site: SiteModel
    @Mock lateinit var mapper: RefundsMapper
    private lateinit var store: RefundsStore

    @Before
    fun setUp() {
        store = RefundsStore(
                restClient,
                Unconfined,
                mapper
        )
    }

    @Test
    fun `returns refunds of an order`() = test {
        val data = arrayOf(REFUND_RESPONSE)
        val fetchRefundsPayload = RefundsPayload(
                data
        )
        val orderId = 1L
        whenever(restClient.fetchAllRefunds(site, orderId)).thenReturn(
                fetchRefundsPayload
        )
        val model = mock<RefundModel>()
        whenever(mapper.map(REFUND_RESPONSE)).thenReturn(model)

        val responseModel = store.fetchAllRefunds(site, orderId)

        assertThat(responseModel.model).isEqualTo(model)
        verify(RefundsSqlUtils).insert(site, orderId, data.toList())
    }
}
