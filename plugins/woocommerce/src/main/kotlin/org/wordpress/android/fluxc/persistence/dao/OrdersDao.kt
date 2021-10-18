package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdSet

@Dao
abstract class OrdersDao {

    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateOrder(order: WCOrderModel)

    fun getOrderByLocalId(localId: LocalId): WCOrderModel? = getOrderByLocalId(localId.value)

    @Query("SELECT * FROM WCOrderModel WHERE id = :localId")
    abstract fun getOrderByLocalId(localId: Int): WCOrderModel?

    @Query("SELECT * FROM WCOrderModel WHERE remoteOrderId = :remoteOrderId AND localSiteId = :localSiteId")
    @Deprecated("Identify orders by local id")
    abstract fun getOrderByRemoteIdAndLocalSiteId(remoteOrderId: Long, localSiteId: Int): WCOrderModel?

    open fun updateLocalOrder(localOrderId: Int, updateOrder: WCOrderModel.() -> WCOrderModel) {
        getOrderByLocalId(localOrderId)
                ?.let(updateOrder)
                ?.let { insertOrUpdateOrder(it) }
    }

    @Transaction
    open fun getOrderForIdSet(orderIdSet: OrderIdSet): WCOrderModel? {
        return getOrderByLocalId(orderIdSet.id) ?: getOrderByRemoteIdAndLocalSiteId(
                remoteOrderId = orderIdSet.remoteOrderId,
                localSiteId = orderIdSet.localSiteId
        )
    }

    @Query("SELECT * FROM WCOrderModel WHERE localSiteId = :localSiteId AND status IN (:status)")
    abstract fun getOrdersForSite(localSiteId: Int, status: List<String>): List<WCOrderModel>

    @Query("SELECT * FROM WCOrderModel WHERE localSiteId = :localSiteId")
    abstract fun getOrdersForSite(localSiteId: Int): List<WCOrderModel>

    open fun getOrdersForSiteByRemoteIds(site: SiteModel, remoteOrderIds: List<RemoteId>): List<WCOrderModel> {
        return getOrdersForSiteByRemoteIds(site.id, remoteOrderIds.map(RemoteId::value))
    }

    @Query("SELECT * FROM WCOrderModel WHERE localSiteId = :localSiteId AND remoteOrderId IN (:remoteOrderIds)")
    abstract fun getOrdersForSiteByRemoteIds(localSiteId: Int, remoteOrderIds: List<Long>): List<WCOrderModel>

    @Query("DELETE FROM WCOrderModel WHERE localSiteId = :localSiteId")
    abstract fun deleteOrdersForSite(localSiteId: Int)

    @Query("SELECT COUNT(*) FROM WCOrderModel WHERE localSiteId = :localSiteId")
    abstract fun getOrderCountForSite(localSiteId: Int): Int
}
