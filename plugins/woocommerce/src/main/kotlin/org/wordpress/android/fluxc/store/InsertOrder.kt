package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import javax.inject.Inject

class InsertOrder @Inject internal constructor(
    private val ordersDaoDecorator: OrdersDaoDecorator,
    private val ordersMetaDataDao: OrderMetaDataDao,
    private val transactionExecutor: TransactionExecutor
) {
    suspend operator fun invoke(
        vararg ordersPack: Pair<OrderEntity, List<OrderMetaDataEntity>>
    ) {
        val listSize = ordersPack.size
        transactionExecutor.executeInTransaction {
            ordersPack.forEachIndexed { index, (order, metaData) ->
                ordersDaoDecorator.insertOrUpdateOrder(order, suppressListRefresh = index != listSize - 1 )
                ordersMetaDataDao.updateOrderMetaData(
                    order.orderId,
                    order.localSiteId,
                    metaData
                )
            }
        }
    }
}
