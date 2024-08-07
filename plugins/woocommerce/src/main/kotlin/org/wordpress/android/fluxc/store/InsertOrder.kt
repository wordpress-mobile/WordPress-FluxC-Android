package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.persistence.entity.MetaDataEntity
import javax.inject.Inject

class InsertOrder @Inject internal constructor(
    private val dispatcher: Dispatcher,
    private val ordersDaoDecorator: OrdersDaoDecorator,
    private val ordersMetaDataDao: MetaDataDao,
    private val transactionExecutor: TransactionExecutor
) {
    suspend operator fun invoke(
        localSiteId: LocalOrRemoteId.LocalId,
        vararg ordersPack: Pair<OrderEntity, List<WCMetaData>>
    ) {
        var orderChanged = false
        transactionExecutor.executeInTransaction {
            ordersPack.forEach { (order, metaData) ->
                val result = ordersDaoDecorator.insertOrUpdateOrder(
                    order,
                    OrdersDaoDecorator.ListUpdateStrategy.SUPPRESS
                )
                if (result != OrdersDaoDecorator.UpdateOrderResult.UNCHANGED) {
                    orderChanged = true
                }
                ordersMetaDataDao.updateMetaData(
                    order.orderId,
                    order.localSiteId,
                    metaData.map {
                        MetaDataEntity(
                            localSiteId = order.localSiteId,
                            id = it.id,
                            parentId = order.orderId,
                            key = it.key,
                            value = it.valueAsString
                        )
                    }
                )
            }
        }
        val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(
            localSiteId = localSiteId.value
        )
        // Re-fetch the list only when at least one of the inserted orders has changed
        if (orderChanged) {
            dispatcher.dispatch(ListActionBuilder.newListRequiresRefreshAction(listTypeIdentifier))
        }
    }
}
