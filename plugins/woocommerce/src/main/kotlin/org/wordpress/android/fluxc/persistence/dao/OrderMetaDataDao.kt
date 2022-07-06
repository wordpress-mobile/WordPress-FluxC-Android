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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.isInternalAttribute
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity

@Dao
abstract class OrderMetaDataDao {
    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateMetaData(metaDataEntity: OrderMetaDataEntity)

    @Query("SELECT * FROM OrderMetaDataEntity WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract suspend fun getOrderMetaData(orderId: Long, localSiteId: LocalId): List<OrderMetaDataEntity>

    @Transaction
    @Query("DELETE FROM OrderMetaDataEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract fun deleteOrderMetaData(localSiteId: LocalId, orderId: Long)

    @Transaction
    open fun updateOrderMetaData(
        orderDto: OrderDto,
        localSiteId: LocalId
    ) {
        val orderId = orderDto.id ?: 0
        deleteOrderMetaData(localSiteId, orderId)

        val responseType = object : TypeToken<List<WCMetaData>>() {}.type
        val metaData = Gson().fromJson(orderDto.meta_data, responseType) as? List<WCMetaData>
            ?: emptyList()
        metaData.filter { it.isInternalAttribute.not() }
            .map {
                insertOrUpdateMetaData(
                    OrderMetaDataEntity(
                        id = it.id,
                        localSiteId = localSiteId,
                        orderId = orderId,
                        key = it.key,
                        value = it.value.toString(),
                        displayKey = it.displayKey,
                        displayValue = it.displayValue.toString()
                    )
                )
            }
    }
}

