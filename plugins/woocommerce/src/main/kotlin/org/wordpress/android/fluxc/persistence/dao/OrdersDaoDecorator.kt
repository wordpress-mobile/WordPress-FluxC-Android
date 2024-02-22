package org.wordpress.android.fluxc.persistence.dao

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersDaoDecorator @Inject constructor(
    private val dispatcher: Dispatcher,
    private val ordersDao: OrdersDao,
) {
    suspend fun updateLocalOrder(
        orderId: Long,
        localSiteId: LocalOrRemoteId.LocalId,
        updateOrder: OrderEntity.() -> OrderEntity
    ) {
        getOrder(orderId, localSiteId)
            ?.let(updateOrder)
            ?.let { insertOrUpdateOrder(it) }
    }

    @Suppress("unused")
    suspend fun getAllOrders(): List<OrderEntity> = ordersDao.getAllOrders()

    /**
     * @param suppressListRefresh Suppresses emit of ListRequiresRefresh event. Can be used
     * when this method is invoked in a loop and the app needs to emit the event at the end.
     */
    fun insertOrUpdateOrder(order: OrderEntity, suppressListRefresh: Boolean = false) {
        ordersDao.insertOrUpdateOrder(order)
        if (!suppressListRefresh) emitRefreshListEvent(order.localSiteId)
    }

    suspend fun getOrder(orderId: Long, localSiteId: LocalOrRemoteId.LocalId): OrderEntity? =
        ordersDao.getOrder(orderId, localSiteId)

    @Suppress("unused")
    fun observeOrder(orderId: Long, localSiteId: LocalOrRemoteId.LocalId): Flow<OrderEntity?> =
        ordersDao.observeOrder(orderId, localSiteId)

    suspend fun getPaidOrdersForSiteDesc(
        localSiteId: LocalOrRemoteId.LocalId,
        status: List<String> = listOf(CoreOrderStatus.COMPLETED.value)
    ): List<OrderEntity> = ordersDao.getPaidOrdersForSiteDesc(localSiteId, status)

    suspend fun getOrdersForSite(
        localSiteId: LocalOrRemoteId.LocalId, status: List<String>
    ): List<OrderEntity> = ordersDao.getOrdersForSite(localSiteId, status)

    suspend fun getOrdersForSite(localSiteId: LocalOrRemoteId.LocalId): List<OrderEntity> =
        ordersDao.getOrdersForSite(localSiteId)

    fun observeOrdersForSite(localSiteId: LocalOrRemoteId.LocalId): Flow<List<OrderEntity>> =
        ordersDao.observeOrdersForSite(localSiteId)

    fun observeOrdersForSite(
        localSiteId: LocalOrRemoteId.LocalId, status: List<String>
    ): Flow<List<OrderEntity>> = ordersDao.observeOrdersForSite(localSiteId, status)

    fun getOrdersForSiteByRemoteIds(
        localSiteId: LocalOrRemoteId.LocalId, orderIds: List<Long>
    ): List<OrderEntity> = ordersDao.getOrdersForSiteByRemoteIds(localSiteId, orderIds)

    fun deleteOrdersForSite(localSiteId: LocalOrRemoteId.LocalId) {
        ordersDao.deleteOrdersForSite(localSiteId)
        emitRefreshListEvent(localSiteId)
    }

    fun getOrderCountForSite(localSiteId: LocalOrRemoteId.LocalId): Int =
        ordersDao.getOrderCountForSite(localSiteId)

    fun observeOrderCountForSite(localSiteId: LocalOrRemoteId.LocalId, status: List<String>): Flow<Int> =
        ordersDao.observeOrderCountForSite(localSiteId, status)

    suspend fun deleteOrder(localSiteId: LocalOrRemoteId.LocalId, orderId: Long) {
        ordersDao.deleteOrder(localSiteId, orderId)
        emitRefreshListEvent(localSiteId)
    }

    private fun emitRefreshListEvent(localSiteId: LocalOrRemoteId.LocalId) {
        val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(
            localSiteId = localSiteId.value
        )
        dispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
    }
}


