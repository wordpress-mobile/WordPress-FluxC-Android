package org.wordpress.android.fluxc.wc.order

import com.nhaarman.mockito_kotlin.mock
import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCOrderStoreTest {
    private val orderStore = WCOrderStore(Dispatcher(), mock())

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
    fun testSimpleInsertionAndRetrieval() {
        val orderModel = OrderTestUtils.generateSampleOrder(42)
        val site = SiteModel().apply { id = orderModel.localSiteId }

        OrderSqlUtils.insertOrUpdateOrder(orderModel)

        val storedOrders = OrderSqlUtils.getOrdersForSite(site)
        assertEquals(1, storedOrders.size)
        assertEquals(42, storedOrders[0].remoteOrderId)
        assertEquals(orderModel, storedOrders[0])
    }

    @Test
    fun testGetOrders() {
        val processingOrder = OrderTestUtils.generateSampleOrder(3)
        val onHoldOrder = OrderTestUtils.generateSampleOrder(4, OrderStatus.ON_HOLD.toString())
        val cancelledOrder = OrderTestUtils.generateSampleOrder(5, OrderStatus.CANCELLED.toString())
        OrderSqlUtils.insertOrUpdateOrder(processingOrder)
        OrderSqlUtils.insertOrUpdateOrder(onHoldOrder)
        OrderSqlUtils.insertOrUpdateOrder(cancelledOrder)

        val site = SiteModel().apply { id = processingOrder.localSiteId }

        val orderList = orderStore.getOrdersForSite(
                site, OrderStatus.PROCESSING.toString(), OrderStatus.CANCELLED.toString())

        assertEquals(2, orderList.size)
        assertTrue(orderList.contains(processingOrder))
        assertTrue(orderList.contains(cancelledOrder))

        val fullOrderList = orderStore.getOrdersForSite(site)
        assertEquals(3, fullOrderList.size)
    }

    @Test
    fun testGetOrderByLocalId() {
        val sampleOrder = OrderTestUtils.generateSampleOrder(3)
        OrderSqlUtils.insertOrUpdateOrder(sampleOrder)

        val retrievedOrder = orderStore.getOrderByIdentifier(sampleOrder.getIdentifier())
        assertEquals(sampleOrder, retrievedOrder)

        // Non-existent ID should return null
        assertNull(orderStore.getOrderByIdentifier(OrderIdentifier(WCOrderModel(id = 1955))))
    }

    @Test
    fun testCustomOrderStatus() {
        val customStatus = "chronologically-incongruous"
        val customStatusOrder = OrderTestUtils.generateSampleOrder(3, customStatus)
        OrderSqlUtils.insertOrUpdateOrder(customStatusOrder)

        val site = SiteModel().apply { id = customStatusOrder.localSiteId }

        val orderList = orderStore.getOrdersForSite(site, customStatus)
        assertEquals(1, orderList.size)
        assertTrue(orderList.contains(customStatusOrder))

        val orderList2 = orderStore.getOrdersForSite(site, customStatus, OrderStatus.CANCELLED.toString())
        assertEquals(1, orderList2.size)
        assertTrue(orderList2.contains(customStatusOrder))

        val fullOrderList = orderStore.getOrdersForSite(site)
        assertEquals(1, fullOrderList.size)
    }

    @Test
    fun testUpdateOrderStatus() {
        val orderModel = OrderTestUtils.generateSampleOrder(42)
        val site = SiteModel().apply { id = orderModel.localSiteId }
        OrderSqlUtils.insertOrUpdateOrder(orderModel)

        // Simulate incoming action with updated order model
        val payload = RemoteOrderPayload(orderModel.apply { status = OrderStatus.REFUNDED.toString() }, site)
        orderStore.onAction(WCOrderActionBuilder.newUpdatedOrderStatusAction(payload))

        with (orderStore.getOrderByIdentifier(orderModel.getIdentifier())!!) {
            // The version of the order model in the database should have the updated status
            assertEquals(OrderStatus.REFUNDED.toString(), status)
            // Other fields should not be altered by the update
            assertEquals(orderModel.currency, currency)
        }
    }

    @Test
    fun testOrderErrorType() {
        assertEquals(OrderErrorType.INVALID_PARAM, OrderErrorType.fromString("invalid_param"))
        assertEquals(OrderErrorType.INVALID_PARAM, OrderErrorType.fromString("INVALID_PARAM"))
        assertEquals(OrderErrorType.GENERIC_ERROR, OrderErrorType.fromString(""))
    }

    @Test
    fun testGetOrderNotesForOrder() {
        val notesJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_notes.json")
        val noteModels = OrderTestUtils.getOrderNotesFromJsonString(notesJson, 6, 949)
        val orderModel = OrderTestUtils.generateSampleOrder(1).apply { id = 949 }
        assertEquals(6, noteModels.size)
        OrderSqlUtils.insertOrIgnoreOrderNote(noteModels[0])

        val retrievedNotes = orderStore.getOrderNotesForOrder(orderModel)
        assertEquals(1, retrievedNotes.size)
        assertEquals(noteModels[0], retrievedNotes[0])
    }

    @Test
    fun testOrderToIdentifierToOrder() {
        // Convert an order to identifier and restore it from the database
        OrderTestUtils.generateSampleOrder(3).let { sampleOrder ->
            OrderSqlUtils.insertOrUpdateOrder(sampleOrder)

            val packagedOrder = sampleOrder.getIdentifier()
            assertEquals("1-3-6", packagedOrder)

            assertEquals(sampleOrder, orderStore.getOrderByIdentifier(packagedOrder))
        }

        // Attempt to restore an order that doesn't exist in the database
        OrderTestUtils.generateSampleOrder(4).let { unpersistedOrder ->
            val packagedOrder = unpersistedOrder.getIdentifier()
            assertEquals("0-4-6", packagedOrder)

            assertNull(orderStore.getOrderByIdentifier(packagedOrder))
        }

        // Restore an order that doesn't have a remote ID
        OrderTestUtils.generateSampleOrder(0).let { draftOrder ->
            OrderSqlUtils.insertOrUpdateOrder(draftOrder)

            val packagedOrder = draftOrder.getIdentifier()
            assertEquals("2-0-6", packagedOrder)

            assertEquals(draftOrder, orderStore.getOrderByIdentifier(packagedOrder))
        }

        // Restore an order without a local ID by matching site and remote order IDs
        OrderTestUtils.generateSampleOrder(3).let { duplicateRemoteOrder ->
            val packagedOrder = duplicateRemoteOrder.getIdentifier()
            assertEquals("0-3-6", packagedOrder)

            assertEquals(duplicateRemoteOrder.apply { id = 1 }, orderStore.getOrderByIdentifier(packagedOrder))
        }
    }
}
