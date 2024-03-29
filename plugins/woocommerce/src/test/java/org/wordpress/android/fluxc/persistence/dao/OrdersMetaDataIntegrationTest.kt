package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import org.wordpress.android.fluxc.store.InsertOrder

@RunWith(RobolectricTestRunner::class)
class OrdersMetaDataIntegrationTest {
    private lateinit var sut: OrderMetaDataDao
    private lateinit var ordersDaoDecorator: OrdersDaoDecorator
    private lateinit var database: WCAndroidDatabase
    private lateinit var insertOrder: InsertOrder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        ordersDaoDecorator = OrdersDaoDecorator(mock(), database.ordersDao)
        sut = database.orderMetaDataDao

        insertOrder = InsertOrder(mock(), ordersDaoDecorator, sut, FakeTransactionExecutor)
    }

    @Test
    fun `should remove orders metadata cascading`() {
        runBlocking {
            // given
            val siteId = LocalId(1)
            val order = OrderEntity(localSiteId = siteId, orderId = 123)
            val metaDataOfTestOrder = EMPTY_ORDER_META_DATA.copy(
                localSiteId = order.localSiteId,
                orderId = order.orderId
            )

            // when
            insertOrder(
                siteId,
                order to listOf(
                    metaDataOfTestOrder.copy(id = 1),
                    metaDataOfTestOrder.copy(id = 2),
                    metaDataOfTestOrder.copy(id = 3)
                )
            )

            // then
            assertThat(
                sut.getOrderMetaData(
                    orderId = order.orderId,
                    localSiteId = order.localSiteId
                )
            ).hasSize(3)

            // when
            ordersDaoDecorator.deleteOrder(order.localSiteId, order.orderId)

            // then
            assertThat(
                sut.getOrderMetaData(
                    orderId = order.orderId,
                    localSiteId = order.localSiteId
                )
            ).isEmpty()
        }
    }

    private companion object {
        val EMPTY_ORDER_META_DATA = OrderMetaDataEntity(
            localSiteId = LocalId(0),
            id = 0,
            orderId = 0,
            key = "",
            value = ""
        )
    }
}
