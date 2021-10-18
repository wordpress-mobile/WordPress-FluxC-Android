package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.dao.OrdersDao
import org.wordpress.android.fluxc.persistence.wrappers.OrderSqlDao
import org.wordpress.android.fluxc.persistence.wrappers.RowAffected
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderUpdateStore @Inject internal constructor(
    private val coroutineEngine: CoroutineEngine,
    private val wcOrderRestClient: OrderRestClient,
    private val ordersDao: OrdersDao
) {
    suspend fun updateCustomerOrderNote(
        orderLocalId: LocalId,
        site: SiteModel,
        newCustomerNote: String
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateCustomerOrderNote") {
            val initialOrder = ordersDao.getOrderByLocalId(orderLocalId)

            if (initialOrder == null) {
                emit(UpdateOrderResult.OptimisticUpdateResult(
                        OnOrderChanged(NO_ROWS_AFFECTED).apply {
                            error = WCOrderStore.OrderError(
                                    message = "Order with id ${orderLocalId.value} not found"
                            )
                        }
                ))
            } else {
                ordersDao.updateLocalOrder(initialOrder.id) {
                    copy(customerNote = newCustomerNote)
                }
                emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged(0)))

                val updateRemoteOrderPayload = wcOrderRestClient.updateCustomerOrderNote(
                        initialOrder,
                        site,
                        newCustomerNote
                )
                val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
                    ordersDao.insertOrUpdateOrder(initialOrder)
                    OnOrderChanged(0).apply {
                        error = updateRemoteOrderPayload.error
                    }
                } else {
                    ordersDao.insertOrUpdateOrder(updateRemoteOrderPayload.order)
                    OnOrderChanged(0)
                }
                emit(RemoteUpdateResult(remoteUpdateResult))
            }
        }
    }

    private companion object {
        const val NO_ROWS_AFFECTED = 0
    }
}
