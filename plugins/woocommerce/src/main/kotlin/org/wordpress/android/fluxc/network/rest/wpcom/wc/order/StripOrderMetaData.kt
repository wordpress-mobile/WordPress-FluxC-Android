package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import javax.inject.Inject

class StripOrderMetaData @Inject internal constructor(private val gson: Gson) {
    private val type = object : TypeToken<List<WCMetaData>>() {}.type

    operator fun invoke(orderDto: OrderDto): List<WCMetaData> {
        return parseMetaDataJSON(orderDto.meta_data)
            ?.filter {
                (it.isDisplayable || it.key in WCMetaData.SUPPORTED_KEYS)
                        && !it.isJson
            }
            ?: emptyList()
    }

    private fun parseMetaDataJSON(metadata: JsonElement?): List<WCMetaData>? {
        return metadata?.let {
            gson.runCatching {
                fromJson<List<WCMetaData>?>(metadata, type)
            }.getOrNull()
        }
    }
}
