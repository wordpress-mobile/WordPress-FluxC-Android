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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCOrderStoreTest {
    private val orderStore = WCOrderStore(Dispatcher(), mock())

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        val config = SingleStoreWellSqlConfigForTests(appContext, WCOrderModel::class.java,
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
        val onHoldOrder = OrderTestUtils.generateSampleOrder(4, OrderStatus.ON_HOLD)
        val cancelledOrder = OrderTestUtils.generateSampleOrder(5, OrderStatus.CANCELLED)
        OrderSqlUtils.insertOrUpdateOrder(processingOrder)
        OrderSqlUtils.insertOrUpdateOrder(onHoldOrder)
        OrderSqlUtils.insertOrUpdateOrder(cancelledOrder)

        val site = SiteModel().apply { id = processingOrder.localSiteId }

        val orderList = orderStore.getOrdersForSite(site, OrderStatus.PROCESSING, OrderStatus.CANCELLED)

        assertEquals(2, orderList.size)
        assertTrue(orderList.contains(processingOrder))
        assertTrue(orderList.contains(cancelledOrder))

        val fullOrderList = orderStore.getOrdersForSite(site)
        assertEquals(3, fullOrderList.size)
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

        val orderList2 = orderStore.getOrdersForSite(site, customStatus, OrderStatus.CANCELLED)
        assertEquals(1, orderList2.size)
        assertTrue(orderList2.contains(customStatusOrder))

        val fullOrderList = orderStore.getOrdersForSite(site)
        assertEquals(1, fullOrderList.size)
    }

    @Test
    fun testOrderErrorType() {
        assertEquals(OrderErrorType.REST_INVALID_PARAM, OrderErrorType.fromString("rest_invalid_param"))
        assertEquals(OrderErrorType.REST_INVALID_PARAM, OrderErrorType.fromString("REST_INVALID_PARAM"))
        assertEquals(OrderErrorType.GENERIC_ERROR, OrderErrorType.fromString(""))
    }
}
