package org.wordpress.android.fluxc.wc.refunds

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.refunds.RefundModel
import org.wordpress.android.fluxc.model.refunds.RefundsMapper
import org.wordpress.android.fluxc.model.stats.time.ClicksModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.persistence.RefundsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.RefundsStore.RefundsPayload
import org.wordpress.android.fluxc.store.StatsStore.FetchStatsPayload
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.API_ERROR
import org.wordpress.android.fluxc.test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefundsStoreTest {
    val restClient = mock<RefundsRestClient>()
    val site = mock<SiteModel>()
    val mapper = mock<RefundsMapper>()
    private lateinit var store: RefundsStore

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(RefundsSqlUtils.RefundsBuilder::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

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

        assertThat(responseModel.model).isEqualTo(listOf(model))
        verify(RefundsSqlUtils).insert(site, orderId, data.toList())
    }

//    @Test
//    fun `returns cached data per site`() = test {
//        whenever(sqlUtils.hasFreshRequest(site, DAYS, DATE, ITEMS_TO_LOAD)).thenReturn(true)
//        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(CLICKS_RESPONSE)
//        val model = mock<ClicksModel>()
//        whenever(mapper.map(CLICKS_RESPONSE, limitMode)).thenReturn(model)
//
//        val forced = false
//        val responseModel = store.fetchClicks(site, DAYS, limitMode, DATE, forced)
//
//        assertThat(responseModel.model).isEqualTo(model)
//        assertThat(responseModel.cached).isTrue()
//        verify(sqlUtils, never()).insert(any(), any(), any(), any(), isNull())
//    }
//
//    @Test
//    fun `returns error when clicks call fail`() = test {
//        val type = API_ERROR
//        val message = "message"
//        val errorPayload = FetchStatsPayload<ClicksResponse>(StatsError(type, message))
//        val forced = true
//        whenever(restClient.fetchClicks(site, DAYS, DATE, ITEMS_TO_LOAD + 1, forced)).thenReturn(errorPayload)
//
//        val responseModel = store.fetchClicks(site, DAYS, limitMode, DATE, forced)
//
//        assertNotNull(responseModel.error)
//        val error = responseModel.error!!
//        assertEquals(type, error.type)
//        assertEquals(message, error.message)
//    }
//
//    @Test
//    fun `returns clicks from db`() {
//        whenever(sqlUtils.select(site, DAYS, DATE)).thenReturn(CLICKS_RESPONSE)
//        val model = mock<ClicksModel>()
//        whenever(mapper.map(CLICKS_RESPONSE, limitMode)).thenReturn(model)
//
//        val result = store.getClicks(site, DAYS, limitMode, DATE)
//
//        assertThat(result).isEqualTo(model)
//    }
}
