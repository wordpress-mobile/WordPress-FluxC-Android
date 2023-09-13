@file:Suppress("DEPRECATION_ERROR")
package org.wordpress.android.fluxc.wc.order

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toImmutableMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder.newFetchedOrderListAction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.store.InsertOrder
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
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
    lateinit var ordersDao: OrdersDao
    lateinit var orderNotesDao: OrderNotesDao
    lateinit var orderMetaDataDao: OrderMetaDataDao
    lateinit var orderStore: WCOrderStore
    private val insertOrder: InsertOrder = mock()

    @Before
    fun setUp() {
        val appContext = ApplicationProvider.getApplicationContext<Application>()

        val database = Room.inMemoryDatabaseBuilder(appContext, WCAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        ordersDao = database.ordersDao
        orderNotesDao = database.orderNotesDao
        orderMetaDataDao = database.orderMetaDataDao

        orderStore = WCOrderStore(
                dispatcher = Dispatcher(),
                wcOrderRestClient = orderRestClient,
                wcOrderFetcher = orderFetcher,
                coroutineEngine = initCoroutineEngine(),
                ordersDao = ordersDao,
                orderNotesDao = orderNotesDao,
                orderMetaDataDao = orderMetaDataDao,
                insertOrder = insertOrder
        )

        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCOrderStatusModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        runBlocking {
            val orderModel = OrderTestUtils.generateSampleOrder(42)
            ordersDao.insertOrUpdateOrder(orderModel)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }

            val storedOrders = ordersDao.getOrdersForSite(site.localId())
            assertEquals(1, storedOrders.size)
            assertEquals(42, storedOrders[0].orderId)
            assertEquals(orderModel, storedOrders[0])
        }
    }

    @Test
    fun testGetOrders() {
        runBlocking {
            val processingOrder = OrderTestUtils.generateSampleOrder(3).saveToDb()
            OrderTestUtils.generateSampleOrder(4, CoreOrderStatus.ON_HOLD.value).saveToDb()
            val cancelledOrder = OrderTestUtils.generateSampleOrder(5, CoreOrderStatus.CANCELLED.value).saveToDb()

            val site = SiteModel().apply { id = processingOrder.localSiteId.value }

            val orderList = orderStore
                .getOrdersForSite(site, CoreOrderStatus.PROCESSING.value, CoreOrderStatus.CANCELLED.value)

            assertEquals(2, orderList.size)
            assertTrue(orderList.contains(processingOrder))
            assertTrue(orderList.contains(cancelledOrder))

            val fullOrderList = orderStore.getOrdersForSite(site)
            assertEquals(3, fullOrderList.size)
        }
    }

    private fun OrderEntity.saveToDb(): OrderEntity {
        ordersDao.insertOrUpdateOrder(this)
        return copy()
    }

    @Test
    fun testGetOrderByLocalId() {
        runBlocking {
            val sampleOrder = OrderTestUtils.generateSampleOrder(3)
            ordersDao.insertOrUpdateOrder(sampleOrder)

            val site = SiteModel().apply { this.id = sampleOrder.localSiteId.value }

            val retrievedOrder = orderStore.getOrderByIdAndSite(sampleOrder.orderId, site)
            assertEquals(sampleOrder, retrievedOrder)

            // Non-existent ID should return null
            // assertNull(orderStore.getOrderByIdentifier(OrderIdentifier(WCOrderModel(id = 1955))))
        }
    }

    @Test
    fun testCustomOrderStatus() {
        runBlocking {
            val customStatus = "chronologically-incongruous"
            val customStatusOrder = OrderTestUtils.generateSampleOrder(3, customStatus)
            ordersDao.insertOrUpdateOrder(customStatusOrder)

            val site = SiteModel().apply { id = customStatusOrder.localSiteId.value }

            val orderList = orderStore.getOrdersForSite(site, customStatus)
            assertEquals(1, orderList.size)
            assertTrue(orderList.contains(customStatusOrder))

            val orderList2 = orderStore.getOrdersForSite(site, customStatus, CoreOrderStatus.CANCELLED.value)
            assertEquals(1, orderList2.size)
            assertTrue(orderList2.contains(customStatusOrder))

            val fullOrderList = orderStore.getOrdersForSite(site)
            assertEquals(1, fullOrderList.size)
        }
    }

    @Test
    fun testUpdateOrderStatus() = runBlocking {
        val orderModel = OrderTestUtils.generateSampleOrder(42)
        ordersDao.insertOrUpdateOrder(orderModel)
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val result = RemoteOrderPayload.Updating(orderModel.copy(status = CoreOrderStatus.REFUNDED.value), site)
        whenever(orderRestClient.updateOrderStatus(orderModel, site, CoreOrderStatus.REFUNDED.value))
                .thenReturn(result)

        orderStore.updateOrderStatus(orderModel.orderId, site, WCOrderStatusModel(CoreOrderStatus.REFUNDED.value))
            .toList()

        with(orderStore.getOrderByIdAndSite(orderModel.orderId, site)!!) {
            // The version of the order model in the database should have the updated status
            assertEquals(CoreOrderStatus.REFUNDED.value, status)
            // Other fields should not be altered by the update
            assertEquals(orderModel.currency, currency)
        }
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
    fun testGetOrderNotesForOrder() = runBlocking {
        val notesJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_notes.json")
        val orderId = 949L
        val localSiteId = 6
        val noteModels = OrderTestUtils.getOrderNotesFromJsonString(notesJson, localSiteId, orderId)
        val orderModel = OrderTestUtils.generateSampleOrder(orderId).copy(localSiteId = LocalId(localSiteId.toInt()))
        val site = SiteModel().apply { id = localSiteId }
        assertEquals(6, noteModels.size)
        orderNotesDao.insertNotes(noteModels[0])

        val retrievedNotes = orderStore.getOrderNotesForOrder(site, orderModel.orderId)
        assertEquals(1, retrievedNotes.size)
        assertEquals(noteModels[0], retrievedNotes[0])
    }

    @Test
    fun testOrderToIdentifierToOrder() {
        runBlocking {
            val site = SiteModel().apply { id = 6 }
            // Convert an order to identifier and restore it from the database
            OrderTestUtils.generateSampleOrder(3).saveToDb().let { sampleOrder ->
                assertEquals(sampleOrder, orderStore.getOrderByIdAndSite(3, site))
            }

            // Attempt to restore an order that doesn't exist in the database
            OrderTestUtils.generateSampleOrder(4).let {
                assertNull(orderStore.getOrderByIdAndSite(4, site))
            }

            // Restore an order that doesn't have a remote ID
            OrderTestUtils.generateSampleOrder(0).saveToDb().let { draftOrder ->
                assertEquals(draftOrder, orderStore.getOrderByIdAndSite(0, site))
            }

            // Restore an order without a local ID by matching site and remote order IDs
            OrderTestUtils.generateSampleOrder(3).let { duplicateRemoteOrder ->
                assertEquals(duplicateRemoteOrder, orderStore.getOrderByIdAndSite(3, site))
            }
        }
    }

    @Test
    fun testFetchingOnlyOutdatedOrMissingOrders() {
        runBlocking {
            val site = SiteModel().apply { id = 8 }

            val upToDate = setupUpToDateOrders(site)
            upToDate.orders.filterNotNull().forEach(ordersDao::insertOrUpdateOrder)
            assertThat(ordersDao.getOrdersForSite(site.localId())).hasSize(10)

            val outdated = setupOutdatedOrders(site)
            outdated.orders.filterNotNull().forEach(ordersDao::insertOrUpdateOrder)
            assertThat(ordersDao.getOrdersForSite(site.localId())).hasSize(20)

            val missing = setupMissingOrders()
            assertThat(ordersDao.getOrdersForSite(site.localId())).hasSize(20)

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
                    (outdated.summaries + missing.summaries).map { it.orderId }
                )
            })
        }
    }

    @Test
    fun testUpdateOrderStatusRequestUpdatesLocalDatabase() = runBlocking {
        val orderModel = OrderTestUtils.generateSampleOrder(42, orderStatus = CoreOrderStatus.PROCESSING.value)
                .saveToDb()
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val result = RemoteOrderPayload.Updating(orderModel.copy(status = CoreOrderStatus.COMPLETED.value), site)
        whenever(orderRestClient.updateOrderStatus(orderModel, site, CoreOrderStatus.COMPLETED.value))
                .thenReturn(result)

        assertThat(ordersDao.getOrder(orderModel.orderId, orderModel.localSiteId)?.status)
                .isEqualTo(CoreOrderStatus.PROCESSING.value)

        orderStore.updateOrderStatus(
                orderModel.orderId,
                site,
                WCOrderStatusModel(CoreOrderStatus.COMPLETED.value)
        ).toList()

        assertThat(ordersDao.getOrder(orderModel.orderId, orderModel.localSiteId)?.status)
                .isEqualTo(CoreOrderStatus.COMPLETED.value)
        Unit
    }

    @Test
    fun testRevertLocalOrderUpdateIfRemoteUpdateFails() = runBlocking {
        val orderModel = OrderTestUtils.generateSampleOrder(42, orderStatus = CoreOrderStatus.PROCESSING.value)
                .saveToDb()
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val error = OrderError()
        whenever(orderRestClient.updateOrderStatus(any(), any(), any())).thenReturn(
                RemoteOrderPayload.Updating(
                        error = error,
                        order = orderModel,
                        site = site
                )
        )

        val response = orderStore.updateOrderStatus(
                orderModel.orderId,
                site,
                WCOrderStatusModel(CoreOrderStatus.COMPLETED.value)
        ).toList().last()

        // Ensure the error is sent in the response
        assertThat(response.event.error).isEqualTo(error)

        assertThat(ordersDao.getOrder(orderModel.orderId, orderModel.localSiteId)?.status)
                .isEqualTo(CoreOrderStatus.PROCESSING.value)
        Unit
    }

    @Test
    fun testObserveOrdersCount() {
        runBlocking {
            val siteId = 5
            val site = SiteModel().apply { id = siteId }
            // When inserting 3 PROCESSING and 1 COMPLETED orders
            for (i in 1L..3L) {
                OrderTestUtils.generateSampleOrder(
                    siteId = siteId,
                    orderId = i,
                    orderStatus = CoreOrderStatus.PROCESSING.value
                ).saveToDb()
            }

            OrderTestUtils.generateSampleOrder(
                siteId = siteId,
                orderId = 4L,
                orderStatus = CoreOrderStatus.COMPLETED.value
            ).saveToDb()

            // Then PROCESSING orders count = 3
            var count = orderStore.observeOrderCountForSite(
                site,
                listOf(CoreOrderStatus.PROCESSING.value)
            ).first()

            assertThat(count).isEqualTo(3)

            count = orderStore.observeOrderCountForSite(
                site,
                listOf(CoreOrderStatus.COMPLETED.value)
            ).first()

            // Then COMPLETED orders count = 1
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun testHasOrdersWithoutLocalOrders() {
        runBlocking {
            // Given there are NO orders in the local database
            val orderModel = OrderTestUtils.generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            val hasOrdersResponse = FetchHasOrdersResponsePayload(site = site, hasOrders = false)
            whenever(orderRestClient.fetchHasOrders(any(), anyOrNull()))
                .thenReturn(hasOrdersResponse)

            // When checking if the store has orders
            val result = orderStore.hasOrders(site)

            // Then check with the API if the store has orders
            verify(orderRestClient).fetchHasOrders(site,null)
            assertThat(result).isInstanceOf(HasOrdersResult.Success::class.java)
            (result as? HasOrdersResult.Success)?.let { success ->
                assertThat(success.hasOrders).isEqualTo(hasOrdersResponse.hasOrders)
            }
        }
    }

    @Test
    fun testHasOrdersWithLocalOrders() {
        runBlocking {
            // Given there are orders in the local database
            val orderModel = OrderTestUtils.generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            orderModel.saveToDb()

            // When checking if the store has orders
            val result = orderStore.hasOrders(site)

            // Then use the database as proof that the store has orders and avoid
            // fetching data from the API
            verify(orderRestClient, never()).fetchHasOrders(site,null)
            assertThat(result).isInstanceOf(HasOrdersResult.Success::class.java)
            (result as? HasOrdersResult.Success)?.let { success ->
                assertThat(success.hasOrders).isEqualTo(true)
            }
        }
    }


    private fun setupMissingOrders(): MutableMap<WCOrderSummaryModel, OrderEntity?> {
        return mutableMapOf<WCOrderSummaryModel, OrderEntity?>().apply {
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
            mutableMapOf<WCOrderSummaryModel, OrderEntity>().apply {
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
                                    orderId = index,
                                    modified = baselineDate
                            )
                    )
                }
            }.toImmutableMap()

    private fun setupUpToDateOrders(site: SiteModel) =
            mutableMapOf<WCOrderSummaryModel, OrderEntity>().apply {
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
                                    orderId = index,
                                    modified = baselineDate
                            )
                    )
                }
            }.toImmutableMap()

    private val Map<WCOrderSummaryModel, OrderEntity?>.summaries
        get() = keys.toList()

    private val Map<WCOrderSummaryModel, OrderEntity?>.orders
        get() = values.toList()
}
