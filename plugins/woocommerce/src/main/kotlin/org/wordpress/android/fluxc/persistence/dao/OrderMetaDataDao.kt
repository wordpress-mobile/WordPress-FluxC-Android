package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

@Dao
abstract class OrderMetaDataDao {
    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateMetaData(metaDataEntity: OrderMetaDataEntity)

    @Transaction
    @Query("DELETE FROM OrderMetaDataEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract suspend fun deleteOrderMetaData(localSiteId: LocalId, orderId: Long)

    open suspend fun insertOrUpdateOrderMetaData(
        orderDto: OrderDto,
        localSiteId: LocalId
    ) {
        val orderId = orderDto.id ?: 0
        deleteOrderMetaData(localSiteId, orderId)
        val responseType = object : TypeToken<List<WCMetaData>>() {}.type
        val metaData = Gson().fromJson(orderDto.meta_data, responseType) as? List<WCMetaData>
            ?: emptyList()
        metaData.forEach { meta ->
            insertOrUpdateMetaData(
                OrderMetaDataEntity(
                    id = meta.id,
                    localSiteId = localSiteId,
                    orderId = orderId,
                    key = meta.key,
                    value = meta.value.toString(),
                    displayKey = meta.displayKey,
                    displayValue = meta.displayKey
                )
            )
        }
    }
}
