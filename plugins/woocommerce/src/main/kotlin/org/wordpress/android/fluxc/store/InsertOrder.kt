package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import javax.inject.Inject

class InsertOrder @Inject internal constructor(
    private val dispatcher: Dispatcher,
    private val ordersDaoDecorator: OrdersDaoDecorator,
    private val ordersMetaDataDao: OrderMetaDataDao,
    private val transactionExecutor: TransactionExecutor
) {
    suspend operator fun invoke(
        localSiteId: LocalOrRemoteId.LocalId,
        vararg ordersPack: Pair<OrderEntity, List<OrderMetaDataEntity>>
    ) {
        transactionExecutor.executeInTransaction {
            ordersPack.forEach { (order, metaData) ->
                ordersDaoDecorator.insertOrUpdateOrder(order, suppressListRefresh = true)
                ordersMetaDataDao.updateOrderMetaData(
                    order.orderId,
                    order.localSiteId,
                    metaData
                )
            }
        }
        val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(
            localSiteId = localSiteId.value
        )
        dispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
    }
}
