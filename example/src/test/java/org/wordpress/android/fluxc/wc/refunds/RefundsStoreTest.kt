package org.wordpress.android.fluxc.wc.refunds

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.Dispatchers.Unconfined
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.RefundsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient
import org.wordpress.android.fluxc.persistence.WCRefundsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCRefundsStore
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsResult
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsError
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsErrorType.INVALID_REFUND_ID
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsPayload
import org.wordpress.android.fluxc.test

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefundsStoreTest {
    private val restClient = mock<RefundsRestClient>()
    private val site = mock<SiteModel>()
    private val mapper = RefundsMapper()
    private lateinit var store: WCRefundsStore

    private val orderId = 1L
    private val refundId = REFUND_RESPONSE.refundId
    private val error = RefundsError(INVALID_REFUND_ID, NOT_FOUND, "Invalid order ID")

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCRefundsSqlUtils.RefundsBuilder::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCRefundsStore(
                restClient,
                Unconfined,
                mapper
        )
    }

    @Test
    fun `fetch all refunds of an order`() = test {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val result = fetchAllTestRefunds()

        assertThat(result.model?.size).isEqualTo(data.size)
        assertThat(result.model?.first()).isEqualTo(mapper.map(data.first()))

        val invalidRequestResult = store.fetchAllRefunds(site, 2)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get all refunds of an order`() = test {
        fetchAllTestRefunds()

        val refunds = store.getAllRefunds(site, orderId)

        assertThat(refunds.size).isEqualTo(1)
        assertThat(refunds.first()).isEqualTo(mapper.map(REFUND_RESPONSE))

        val invalidRequestResult = store.getAllRefunds(site, 2)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `fetch specific refund`() = test {
        val refund = fetchSpecificTestRefund()

        assertThat(refund.model).isEqualTo(mapper.map(REFUND_RESPONSE))
    }

    @Test
    fun `get specific refund`() = test {
        fetchSpecificTestRefund()
        val refund = store.getRefund(site, orderId, refundId)

        assertThat(refund).isEqualTo(mapper.map(REFUND_RESPONSE))
    }

    private suspend fun fetchSpecificTestRefund(): RefundsResult<WCRefundModel> {
        val fetchRefundsPayload = RefundsPayload(
                REFUND_RESPONSE
        )
        whenever(restClient.fetchRefund(site, orderId, refundId)).thenReturn(
                fetchRefundsPayload
        )

        whenever(restClient.fetchRefund(site, 2, refundId)).thenReturn(
                RefundsPayload(error)
        )
        return store.fetchRefund(site, orderId, refundId)
    }

    private suspend fun fetchAllTestRefunds(): RefundsResult<List<WCRefundModel>> {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val fetchRefundsPayload = RefundsPayload(
                data
        )
        whenever(restClient.fetchAllRefunds(site, orderId)).thenReturn(
                fetchRefundsPayload
        )
        whenever(restClient.fetchAllRefunds(site, 2)).thenReturn(
                RefundsPayload(error)
        )
        return store.fetchAllRefunds(site, orderId)
    }
}
