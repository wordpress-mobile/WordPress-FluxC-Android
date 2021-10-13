package org.wordpress.android.fluxc.wc.order

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toImmutableMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder.newFetchedOrderListAction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@InternalCoroutinesApi
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCOrderStoreTest {
    private val orderFetcher: WCOrderFetcher = mock()
    private val orderRestClient: OrderRestClient = mock()
    private val orderStore = WCOrderStore(Dispatcher(), orderRestClient, orderFetcher, initCoroutineEngine())

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Application>()
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCOrderModel::class.java, WCOrderNoteModel::class.java, WCOrderStatusModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
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
        val onHoldOrder = OrderTestUtils.generateSampleOrder(4, CoreOrderStatus.ON_HOLD.value)
        val cancelledOrder = OrderTestUtils.generateSampleOrder(5, CoreOrderStatus.CANCELLED.value)
        OrderSqlUtils.insertOrUpdateOrder(processingOrder)
        OrderSqlUtils.insertOrUpdateOrder(onHoldOrder)
        OrderSqlUtils.insertOrUpdateOrder(cancelledOrder)

        val site = SiteModel().apply { id = processingOrder.localSiteId }

        val orderList = orderStore
                .getOrdersForSite(site, CoreOrderStatus.PROCESSING.value, CoreOrderStatus.CANCELLED.value)

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

        val orderList2 = orderStore.getOrdersForSite(site, customStatus, CoreOrderStatus.CANCELLED.value)
        assertEquals(1, orderList2.size)
        assertTrue(orderList2.contains(customStatusOrder))

        val fullOrderList = orderStore.getOrdersForSite(site)
        assertEquals(1, fullOrderList.size)
    }

    @Test
    fun testUpdateOrderStatus() = runBlocking {
        val orderModel = OrderTestUtils.generateSampleOrder(42)
        val site = SiteModel().apply { id = orderModel.localSiteId }
        OrderSqlUtils.insertOrUpdateOrder(orderModel)
        val result = RemoteOrderPayload(orderModel.apply { status = CoreOrderStatus.REFUNDED.value }, site)
        whenever(orderRestClient.updateOrderStatus(orderModel, site, CoreOrderStatus.REFUNDED.value))
            .thenReturn(result)

        orderStore.updateOrderStatus(LocalId(orderModel.id), site, CoreOrderStatus.REFUNDED.value)
            .toList()

        with(orderStore.getOrderByIdentifier(orderModel.getIdentifier())!!) {
            // The version of the order model in the database should have the updated status
            assertEquals(CoreOrderStatus.REFUNDED.value, status)
            // Other fields should not be altered by the update
            assertEquals(orderModel.currency, currency)
        }
        Unit
    }

    @Test
    fun testGetOrderStatusOptions() {
        val site = SiteModel().apply { id = 8 }
        val optionsJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_status_options.json")
        val orderStatusOptions = OrderTestUtils.getOrderStatusOptionsFromJson(optionsJson, site.id)
        orderStatusOptions.sumBy { OrderSqlUtils.insertOrUpdateOrderStatusOption(it) }

        // verify that the order status options are stored correctly
        val storedOrderStatusOptions = OrderSqlUtils.getOrderStatusOptionsForSite(site)
        assertEquals(orderStatusOptions.size, storedOrderStatusOptions.size)

        val firstOrderStatusOption = storedOrderStatusOptions[0]
        assertEquals(firstOrderStatusOption.label, orderStatusOptions[0].label)
        assertEquals(firstOrderStatusOption.statusCount, orderStatusOptions[0].statusCount)
        firstOrderStatusOption.apply { statusCount = 100 }

        // Simulate incoming action with updated order status model list
        val payload = FetchOrderStatusOptionsResponsePayload(site, storedOrderStatusOptions)
        orderStore.onAction(WCOrderActionBuilder.newFetchedOrderStatusOptionsAction(payload))

        with(OrderSqlUtils.getOrderStatusOptionsForSite(site)[0]) {
            // The status count of the first order status model in the database should have updated
            assertEquals(firstOrderStatusOption.statusCount, statusCount)
            // Other fields should not be altered by the update
            assertEquals(firstOrderStatusOption.label, label)
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

        val retrievedNotes = orderStore.getOrderNotesForOrder(orderModel.id)
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

    @Test
    fun testFetchingOnlyOutdatedOrMissingOrders() {
        val site = SiteModel().apply { id = 8 }

        val upToDate = setupUpToDateOrders(site)
        upToDate.orders.filterNotNull().forEach(OrderSqlUtils::insertOrUpdateOrder)
        assertThat(OrderSqlUtils.getOrdersForSite(site)).hasSize(10)

        val outdated = setupOutdatedOrders(site)
        outdated.orders.filterNotNull().forEach(OrderSqlUtils::insertOrUpdateOrder)
        assertThat(OrderSqlUtils.getOrdersForSite(site)).hasSize(20)

        val missing = setupMissingOrders()
        assertThat(OrderSqlUtils.getOrdersForSite(site)).hasSize(20)

        orderStore.onAction(
                newFetchedOrderListAction(
                        FetchOrderListResponsePayload(
                                WCOrderListDescriptor(site = site),
                                requestStartTime = Calendar.getInstance(),
                                orderSummaries = upToDate.summaries + outdated.summaries + missing.summaries
                        )
                )
        )

        verify(orderFetcher).fetchOrders(eq(site), check { remoteIdsToFetch ->
            assertThat(remoteIdsToFetch).containsExactlyInAnyOrderElementsOf(
                    (outdated.summaries + missing.summaries).map { RemoteId(it.remoteOrderId) }
            )
        })
    }

    @Test
    fun testUpdateOrderStatusRequestUpdatesLocalDatabase() = runBlocking {
        val orderModel = OrderTestUtils.generateSampleOrder(42, orderStatus = CoreOrderStatus.PROCESSING.value)
        val site = SiteModel().apply { id = orderModel.localSiteId }
        OrderSqlUtils.insertOrUpdateOrder(orderModel)
        val result = RemoteOrderPayload(orderModel.apply { status = CoreOrderStatus.COMPLETED.value }, site)
        whenever(orderRestClient.updateOrderStatus(orderModel, site, CoreOrderStatus.COMPLETED.value))
                .thenReturn(result)

        assertThat(OrderSqlUtils.getOrderByLocalId(orderModel.id).status).isEqualTo(CoreOrderStatus.PROCESSING.value)

        orderStore.updateOrderStatus(LocalId(orderModel.id), site, CoreOrderStatus.COMPLETED.value)
                .toList()

        assertThat(OrderSqlUtils.getOrderByLocalId(orderModel.id).status).isEqualTo(CoreOrderStatus.COMPLETED.value)
        Unit
    }

    @Test
    fun testRevertLocalOrderUpdateIfRemoteUpdateFails() = runBlocking {
        val orderModel = OrderTestUtils.generateSampleOrder(42, orderStatus = CoreOrderStatus.PROCESSING.value)
        val site = SiteModel().apply { id = orderModel.localSiteId }
        OrderSqlUtils.insertOrUpdateOrder(orderModel)
        whenever(orderRestClient.updateOrderStatus(any(), any(), any())).thenReturn(
            RemoteOrderPayload(
                error = OrderError(),
                order = orderModel,
                site = site
            )
        )

        orderStore.updateOrderStatus(LocalId(orderModel.id), site, CoreOrderStatus.COMPLETED.value)
                .toList()

        assertThat(OrderSqlUtils.getOrderByLocalId(orderModel.id).status).isEqualTo(CoreOrderStatus.PROCESSING.value)
        Unit
    }

    private fun setupMissingOrders(): MutableMap<WCOrderSummaryModel, WCOrderModel?> {
        return mutableMapOf<WCOrderSummaryModel, WCOrderModel?>().apply {
            (21L..30L).forEach { index ->
                put(
                        OrderTestUtils.generateSampleOrderSummary(
                                id = index,
                                remoteId = index
                        ),
                        null
                )
            }
        }
    }

    private fun setupOutdatedOrders(site: SiteModel) =
            mutableMapOf<WCOrderSummaryModel, WCOrderModel>().apply {
                val baselineDate = "2021-01-05T12:00:00Z"
                val oneDayAfterBaselineDate = "2021-01-06T12:00:00Z"
                (11L..20L).forEach { index ->
                    put(
                            OrderTestUtils.generateSampleOrderSummary(
                                    id = index,
                                    remoteId = index,
                                    modified = oneDayAfterBaselineDate
                            ),
                            OrderTestUtils.generateSampleOrder(
                                    siteId = site.id,
                                    remoteId = index,
                                    modified = baselineDate
                            )
                    )
                }
            }.toImmutableMap()

    private fun setupUpToDateOrders(site: SiteModel) =
            mutableMapOf<WCOrderSummaryModel, WCOrderModel>().apply {
                val baselineDate = "2021-01-05T12:00:00Z"
                (1L..10L).forEach { index ->
                    put(
                            OrderTestUtils.generateSampleOrderSummary(
                                    id = index,
                                    remoteId = index,
                                    modified = baselineDate
                            ),
                            OrderTestUtils.generateSampleOrder(
                                    siteId = site.id,
                                    remoteId = index,
                                    modified = baselineDate
                            )
                    )
                }
            }.toImmutableMap()

    private val Map<WCOrderSummaryModel, WCOrderModel?>.summaries
        get() = keys.toList()

    private val Map<WCOrderSummaryModel, WCOrderModel?>.orders
        get() = values.toList()
}
