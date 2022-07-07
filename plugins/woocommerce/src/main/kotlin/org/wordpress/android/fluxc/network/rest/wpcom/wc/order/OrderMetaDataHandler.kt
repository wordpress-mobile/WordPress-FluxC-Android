package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.isInternalAttribute
import org.wordpress.android.fluxc.persistence.dao.OrderMetaDataDao
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import javax.inject.Inject

class OrderMetaDataHandler @Inject constructor(
    private val gson: Gson,
    private val orderMetaDataDao: OrderMetaDataDao
) {
    operator fun invoke(orderDto: OrderDto, localSiteId: LocalId) {
        if (orderDto.id == null) {
            return
        }
        val responseType = object : TypeToken<List<WCMetaData>>() {}.type
        val metaData = gson.fromJson(orderDto.meta_data, responseType) as? List<WCMetaData>
            ?: emptyList()

        orderMetaDataDao.updateOrderMetaData(
            orderId = orderDto.id,
            localSiteId = localSiteId,
            metaData = metaData.filter { it.isInternalAttribute.not() }
                .map {
                    OrderMetaDataEntity(
                        id = it.id,
                        localSiteId = localSiteId,
                        orderId = orderDto.id,
                        key = it.key,
                        value = it.value.toString(),
                        displayKey = it.displayKey,
                        displayValue = it.displayValue.toString()
                    )
                }
        )
    }
}
