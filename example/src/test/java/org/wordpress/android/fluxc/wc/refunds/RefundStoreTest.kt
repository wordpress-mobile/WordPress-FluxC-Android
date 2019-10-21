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
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.persistence.WCRefundSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCRefundStore.RefundResult
import org.wordpress.android.fluxc.store.WCRefundStore.RefundError
import org.wordpress.android.fluxc.store.WCRefundStore.RefundErrorType.INVALID_REFUND_ID
import org.wordpress.android.fluxc.store.WCRefundStore.RefundPayload
import org.wordpress.android.fluxc.test

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefundStoreTest {
    private val restClient = mock<RefundRestClient>()
    private val site = mock<SiteModel>()
    private val mapper = RefundMapper()
    private lateinit var store: WCRefundStore

    private val orderId = 1L
    private val refundId = REFUND_RESPONSE.refundId
    private val error = RefundError(INVALID_REFUND_ID, NOT_FOUND, "Invalid order ID")

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCRefundSqlUtils.RefundBuilder::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()

        store = WCRefundStore(
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

    private suspend fun fetchSpecificTestRefund(): RefundResult<WCRefundModel> {
        val fetchRefundsPayload = RefundPayload(
                REFUND_RESPONSE
        )
        whenever(restClient.fetchRefund(site, orderId, refundId)).thenReturn(
                fetchRefundsPayload
        )

        whenever(restClient.fetchRefund(site, 2, refundId)).thenReturn(
                RefundPayload(error)
        )
        return store.fetchRefund(site, orderId, refundId)
    }

    private suspend fun fetchAllTestRefunds(): RefundResult<List<WCRefundModel>> {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val fetchRefundsPayload = RefundPayload(
                data
        )
        whenever(
                restClient.fetchAllRefunds(
                        site,
                        orderId,
                        WCRefundStore.DEFAULT_PAGE,
                        WCRefundStore.DEFAULT_PAGE_SIZE
                )
        ).thenReturn(
                fetchRefundsPayload
        )
        whenever(
                restClient.fetchAllRefunds(
                        site,
                        2,
                        WCRefundStore.DEFAULT_PAGE,
                        WCRefundStore.DEFAULT_PAGE_SIZE
                )
        ).thenReturn(
                RefundPayload(error)
        )
        return store.fetchAllRefunds(site, orderId)
    }
}
