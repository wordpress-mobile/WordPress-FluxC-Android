package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
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
    private val orderSqlDao: OrderSqlDao
) {
    suspend fun updateOrderNotes(
        initialOrder: WCOrderModel,
        site: SiteModel,
        newNotes: String
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(T.API, this, "updateOrderNotes") {
            val optimisticUpdateRowsAffected: RowAffected = orderSqlDao.updateLocalOrder(initialOrder.id) {
                customerNote = newNotes
            }
            emit(UpdateOrderResult.OptimisticUpdateResult(OnOrderChanged(optimisticUpdateRowsAffected)))

            val updateRemoteOrderPayload = wcOrderRestClient.updateCustomerOrderNote(
                    initialOrder,
                    site,
                    newNotes
            )
            val remoteUpdateResult = if (updateRemoteOrderPayload.isError) {
                OnOrderChanged(orderSqlDao.insertOrUpdateOrder(initialOrder)).apply {
                    error = updateRemoteOrderPayload.error
                }
            } else {
                OnOrderChanged(orderSqlDao.insertOrUpdateOrder(updateRemoteOrderPayload.order))
            }
            emit(RemoteUpdateResult(remoteUpdateResult))
        }
    }
}
