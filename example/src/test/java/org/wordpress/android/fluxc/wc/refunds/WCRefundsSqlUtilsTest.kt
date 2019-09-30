package org.wordpress.android.fluxc.wc.refunds

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WCRefundsSqlUtils
import org.wordpress.android.fluxc.persistence.WCRefundsSqlUtils.RefundsBuilder
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCRefundsSqlUtilsTest {
    private val orderId = 1L
    private val site = SiteModel().apply { id = 2 }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(RefundsBuilder::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `test refund insert`() {
        WCRefundsSqlUtils.insertOrUpdate(site, orderId, REFUND_RESPONSE)
        val refunds = WCRefundsSqlUtils.selectAllRefunds(site, orderId)
        assertEquals(1, refunds.size)
        assertEquals(REFUND_RESPONSE, refunds.first())
    }

    @Test
    fun `test refund update`() {
        val newAmount = "20"

        WCRefundsSqlUtils.insertOrUpdate(site, orderId, REFUND_RESPONSE)
        val refund = WCRefundsSqlUtils.selectRefund(site, orderId, REFUND_RESPONSE.refundId)!!
        assertEquals(REFUND_RESPONSE.amount, refund.amount)

        WCRefundsSqlUtils.insertOrUpdate(site, orderId, REFUND_RESPONSE.copy(amount = "20"))
        val updatedRefund = WCRefundsSqlUtils.selectRefund(site, orderId, REFUND_RESPONSE.refundId)!!
        assertEquals(newAmount, updatedRefund.amount)
    }


    @Test
    fun `test select`() {
        val inserted = listOf(REFUND_RESPONSE, REFUND_RESPONSE.copy(refundId = 2, amount = "20"))

        WCRefundsSqlUtils.insertOrUpdate(site, orderId, inserted)
        val refunds = WCRefundsSqlUtils.selectAllRefunds(site, orderId)
        assertEquals(inserted, refunds)

        val refund = WCRefundsSqlUtils.selectRefund(site, orderId, 2)
        assertEquals(refunds[1], refund)
    }


    @Test
    fun `test select empty result`() {
        val inserted = listOf(REFUND_RESPONSE, REFUND_RESPONSE.copy(refundId = 2, amount = "20"))

        WCRefundsSqlUtils.insertOrUpdate(site, orderId, inserted)
        val refunds = WCRefundsSqlUtils.selectAllRefunds(site, 2)
        assertTrue(refunds.isEmpty())

        val refund = WCRefundsSqlUtils.selectRefund(site, orderId, 3)
        assertNull(refund)
    }
}
