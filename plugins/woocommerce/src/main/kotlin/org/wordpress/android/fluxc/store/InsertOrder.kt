package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import javax.inject.Inject

class InsertOrder @Inject internal constructor(
    private val ordersDao: OrdersDao,
    private val ordersMetaDataDao: OrderMetaDataDao,
    private val transactionExecutor: TransactionExecutor
) {
    suspend operator fun invoke(vararg ordersPack: Pair<OrderEntity, List<OrderMetaDataEntity>>) {
        transactionExecutor.executeInTransaction {
            ordersPack.forEach { (order, metaData) ->
                ordersDao.insertOrUpdateOrder(order)
                ordersMetaDataDao.updateOrderMetaData(
                    order.orderId,
                    order.localSiteId,
                    metaData
                )
            }
        }
    }
}
