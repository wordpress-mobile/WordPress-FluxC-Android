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
    private val htmlRegex by lazy {
        Regex("<[^>]+>")
    }

    private val jsonRegex by lazy {
        Regex("""^.*(?:\{.*\}|\[.*\]).*$""")
    }

    operator fun invoke(orderDto: OrderDto, localSiteId: LocalId) {
        if (orderDto.id == null) {
            return
        }

        val responseType = object : TypeToken<List<WCMetaData>>() {}.type
        val metaData = gson.fromJson<List<WCMetaData>?>(orderDto.meta_data, responseType)
            ?.filter { it.isInternalAttribute.not() }
            ?.asSequence()
            ?.map { it.asOrderMetaDataEntity(orderDto.id, localSiteId) }
            ?.filter { it.value.isNotEmpty() and it.value.matches(jsonRegex).not() }
            ?.toList()
            ?: emptyList()

        orderMetaDataDao.updateOrderMetaData(
            orderId = orderDto.id,
            localSiteId = localSiteId,
            metaData = metaData
        )
    }

    private fun WCMetaData.asOrderMetaDataEntity(orderId: Long, localSiteId: LocalId) =
        OrderMetaDataEntity(
            orderId = orderId,
            localSiteId = localSiteId,
            id = id,
            key = key,
            value = value.toString().replace(htmlRegex, ""),
            displayKey = displayKey,
            displayValue = displayValue.toString()
        )
}