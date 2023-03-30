package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.isDisplayableAttribute
import org.wordpress.android.fluxc.persistence.entity.OrderMetaDataEntity
import javax.inject.Inject

class StripOrderMetaData @Inject internal constructor(private val gson: Gson) {
    private val htmlRegex by lazy {
        Regex("<[^>]+>")
    }

    private val jsonRegex by lazy {
        Regex("""^.*(?:\{.*\}|\[.*\]).*$""")
    }

    private val type = object : TypeToken<List<WCMetaData>>() {}.type

    operator fun invoke(orderDto: OrderDto, localSiteId: LocalId): List<OrderMetaDataEntity> {
        if (orderDto.id == null) {
            return emptyList()
        }

        return parseMetaDataJSON(orderDto.meta_data)
            ?.asSequence()
            ?.filter { it.isDisplayableAttribute || it.key in WCMetaData.SUPPORTED_KEYS }
            ?.map { it.asOrderMetaDataEntity(orderDto.id, localSiteId) }
            ?.filter { it.value.isNotEmpty() && it.value.matches(jsonRegex).not() }
            ?.toList()
            ?: emptyList()
    }

    private fun parseMetaDataJSON(metadata: JsonElement?): List<WCMetaData>? {
        return metadata?.let {
            gson.runCatching {
                fromJson<List<WCMetaData>?>(metadata, type)
            }.getOrNull()
        }
    }

    private fun WCMetaData.asOrderMetaDataEntity(orderId: Long, localSiteId: LocalId) =
        OrderMetaDataEntity(
            id = id,
            localSiteId = localSiteId,
            orderId = orderId,
            key = key.orEmpty(),
            value = value.toString().replace(htmlRegex, ""),
            isDisplayable = isDisplayableAttribute
        )
}
