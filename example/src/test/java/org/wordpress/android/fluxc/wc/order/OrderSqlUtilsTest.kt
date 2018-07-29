package org.wordpress.android.fluxc.wc.order

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class OrderSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCOrderModel::class.java, WCOrderNoteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testInsertOrUpdateOrder() {
        val orderModel = OrderTestUtils.generateSampleOrder(42)
        val site = SiteModel().apply { id = orderModel.localSiteId }

        // Test inserting order
        OrderSqlUtils.insertOrUpdateOrder(orderModel)
        val storedOrders = OrderSqlUtils.getOrdersForSite(site)
        assertEquals(1, storedOrders.size)
        assertEquals(42, storedOrders[0].remoteOrderId)
        assertEquals(orderModel, storedOrders[0])

        // Test updating order
        storedOrders[0].apply {
            status = "processing"
            customerNote = "please gift wrap"
        }.also {
            OrderSqlUtils.insertOrUpdateOrder(it)
        }
        val updatedOrders = OrderSqlUtils.getOrdersForSite(site)
        assertEquals(1, storedOrders.size)
        assertEquals(storedOrders[0].id, updatedOrders[0].id)
        assertEquals(storedOrders[0].customerNote, updatedOrders[0].customerNote)
        assertEquals(storedOrders[0].status, updatedOrders[0].status)
    }

    @Test
    fun testGetOrdersForSite() {
        val processingOrder = OrderTestUtils.generateSampleOrder(3)
        val onHoldOrder = OrderTestUtils.generateSampleOrder(4, OrderStatus.ON_HOLD.value)
        val cancelledOrder = OrderTestUtils.generateSampleOrder(5, OrderStatus.CANCELLED.value)
        OrderSqlUtils.insertOrUpdateOrder(processingOrder)
        OrderSqlUtils.insertOrUpdateOrder(onHoldOrder)
        OrderSqlUtils.insertOrUpdateOrder(cancelledOrder)
        val site = SiteModel().apply { id = processingOrder.localSiteId }

        // Test getting orders without specifying a status
        val storedOrders = OrderSqlUtils.getOrdersForSite(site)
        assertEquals(3, storedOrders.size)

        // Test pulling orders with a single status specified
        val processingOrders = OrderSqlUtils.getOrdersForSite(site, listOf(OrderStatus.PROCESSING.value))
        assertEquals(1, processingOrders.size)

        // Test pulling orders with multiple statuses specified
        val mixStatusOrders = OrderSqlUtils
                .getOrdersForSite(site, listOf(OrderStatus.ON_HOLD.value, OrderStatus.CANCELLED.value))
        assertEquals(2, mixStatusOrders.size)
    }

    @Test
    fun testDeleteOrdersForSite() {
        val order1 = OrderTestUtils.generateSampleOrder(1)
        val order2 = OrderTestUtils.generateSampleOrder(2)
        OrderSqlUtils.insertOrUpdateOrder(order1)
        OrderSqlUtils.insertOrUpdateOrder(order2)
        val site = SiteModel().apply { id = order1.localSiteId }

        val storedOrders = OrderSqlUtils.getOrdersForSite(site)
        assertEquals(2, storedOrders.size)

        val deletedCount = OrderSqlUtils.deleteOrdersForSite(site)
        assertEquals(2, deletedCount)

        val deletedOrders = OrderSqlUtils.getOrdersForSite(site)
        assertEquals(0, deletedOrders.size)
    }

    @Test
    fun testInsertOrIgnoreOrderNotes() {
        val order = OrderTestUtils.generateSampleOrder(42)
        val note1 = OrderTestUtils.generateSampleNote(1, order.localSiteId, order.id)
        val note2 = OrderTestUtils.generateSampleNote(2, order.localSiteId, order.id)

        // Test inserting notes
        OrderSqlUtils.insertOrIgnoreOrderNotes(listOf(note1, note2))
        val storedNotes = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(2, storedNotes.size)

        // Test ignoring notes already saved to db
        val inserted = OrderSqlUtils.insertOrIgnoreOrderNotes(listOf(note1))
        assertEquals(0, inserted)
        val storedNotes2 = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(2, storedNotes2.size)
    }

    @Test
    fun testInsertOrIgnoreOrderNote() {
        val order = OrderTestUtils.generateSampleOrder(42)
        val note1 = OrderTestUtils.generateSampleNote(1, order.localSiteId, order.id)
        val note2 = OrderTestUtils.generateSampleNote(2, order.localSiteId, order.id)

        // Test inserting notes
        OrderSqlUtils.insertOrIgnoreOrderNote(note1)
        OrderSqlUtils.insertOrIgnoreOrderNote(note2)
        val storedNotes = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(2, storedNotes.size)

        // Test ignoring notes already saved to db
        val inserted = OrderSqlUtils.insertOrIgnoreOrderNote(note1)
        assertEquals(0, inserted)
        val storedNotes2 = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(2, storedNotes2.size)
    }

    @Test
    fun testGetOrderNotesForOrder() {
        val order = OrderTestUtils.generateSampleOrder(42)
        val note1 = OrderTestUtils.generateSampleNote(1, order.localSiteId, order.id)
        val note2 = OrderTestUtils.generateSampleNote(2, order.localSiteId, order.id)
        OrderSqlUtils.insertOrIgnoreOrderNotes(listOf(note1, note2))

        val storedNotes = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(2, storedNotes.size)
    }

    @Test
    fun testDeleteOrderNotesForSite() {
        val order = OrderTestUtils.generateSampleOrder(42)
        val note1 = OrderTestUtils.generateSampleNote(1, order.localSiteId, order.id)
        val note2 = OrderTestUtils.generateSampleNote(2, order.localSiteId, order.id)
        OrderSqlUtils.insertOrIgnoreOrderNotes(listOf(note1, note2))
        val site = SiteModel().apply { id = order.localSiteId }

        val storedNotes = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(2, storedNotes.size)

        val deletedCount = OrderSqlUtils.deleteOrderNotesForSite(site)
        assertEquals(2, deletedCount)
        val verify = OrderSqlUtils.getOrderNotesForOrder(order.id)
        assertEquals(0, verify.size)
    }
}
