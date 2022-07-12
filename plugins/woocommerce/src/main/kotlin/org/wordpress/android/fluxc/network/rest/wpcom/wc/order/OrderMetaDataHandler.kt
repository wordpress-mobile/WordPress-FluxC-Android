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
        val metaData = gson.fromJson<List<WCMetaData>?>(orderDto.meta_data, responseType)
                ?.filter { it.isInternalAttribute.not() }
                ?.map { OrderMetaDataEntity(orderDto.id, localSiteId, it) }
                ?.filter { it.value.isNotEmpty() }
                ?.map { it.strippedOfHtmlTags }
                ?: emptyList()

        orderMetaDataDao.updateOrderMetaData(
            orderId = orderDto.id,
            localSiteId = localSiteId,
            metaData = metaData
        )
    }

    private val OrderMetaDataEntity.strippedOfHtmlTags
        get() = copy(value = value.replace("\\<.*?\\>", ""))
}
