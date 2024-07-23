package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

@Dao
abstract class OrderMetaDataDao {
    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateMetaData(metaDataEntity: OrderMetaDataEntity)

    @Query("SELECT * FROM OrderMetaData WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract suspend fun getOrderMetaData(
        orderId: Long,
        localSiteId: LocalId
    ): List<OrderMetaDataEntity>

    @Query("SELECT * FROM OrderMetaData WHERE orderId = :orderId AND localSiteId = :localSiteId AND isDisplayable = 1")
    abstract suspend fun getDisplayableOrderMetaData(
        orderId: Long,
        localSiteId: LocalId
    ): List<OrderMetaDataEntity>

    @Query("SELECT COUNT(*) FROM OrderMetaData WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract suspend fun getOrderMetaDataCount(orderId: Long, localSiteId: LocalId): Int

    @Query(
        """
        SELECT COUNT(*) FROM OrderMetaData 
        WHERE orderId = :orderId AND localSiteId = :localSiteId AND isDisplayable = 1
        """
    )
    abstract suspend fun getDisplayableOrderMetaDataCount(orderId: Long, localSiteId: LocalId): Int

    @Transaction
    @Query("DELETE FROM OrderMetaData WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract fun deleteOrderMetaData(localSiteId: LocalId, orderId: Long)

    @Transaction
    open fun updateOrderMetaData(
        orderId: Long,
        localSiteId: LocalId,
        metaData: List<OrderMetaDataEntity>
    ) {
        deleteOrderMetaData(localSiteId, orderId)
        metaData.forEach {
            insertOrUpdateMetaData(it)
        }
    }

    suspend fun hasOrderMetaData(orderId: Long, localSiteId: LocalId): Boolean {
        return getOrderMetaDataCount(orderId, localSiteId) > 0
    }
}
