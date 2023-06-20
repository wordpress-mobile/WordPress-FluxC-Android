package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCProductModel.AddOnsMetadataKeys
import org.wordpress.android.fluxc.model.WCProductModel.QuantityRulesMetadataKeys
import org.wordpress.android.fluxc.utils.JsonStringUtils
import org.wordpress.android.fluxc.utils.isJsonEmptyElement
import javax.inject.Inject

class StripProductMetaData @Inject internal constructor(private val gson: Gson) {
    operator fun invoke(metadata: String?): String {
        if (metadata.isNullOrEmpty()) return JsonStringUtils.EMPTY.ARRAY

        return gson.fromJson(metadata, JsonArray::class.java)
            .mapNotNull { it as? JsonObject }
            .asSequence()
            .filter { jsonObject ->
                val isNullOrEmpty = isElementNullOrEmpty(jsonObject[WCMetaData.VALUE])
                jsonObject[WCMetaData.KEY]?.asString.orEmpty() in SUPPORTED_KEYS && isNullOrEmpty.not()
            }.toList()
            .takeIf { it.isNotEmpty() }
            ?.let { gson.toJson(it) } ?: JsonStringUtils.EMPTY.ARRAY
    }

    private fun isElementNullOrEmpty(jsonElement: JsonElement?): Boolean {
        return jsonElement?.let {
            if (it.isJsonNull) return@let true

            val valueString = gson.toJson(jsonElement)
            valueString.isJsonEmptyElement()
        } ?: true
    }

    companion object {
        val SUPPORTED_KEYS: Set<String> = buildSet {
            add(AddOnsMetadataKeys.ADDONS_METADATA_KEY)
            addAll(QuantityRulesMetadataKeys.ALL_KEYS)
        }
    }
}
