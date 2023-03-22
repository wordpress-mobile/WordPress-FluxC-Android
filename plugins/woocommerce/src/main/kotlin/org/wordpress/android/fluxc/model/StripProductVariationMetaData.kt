package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import javax.inject.Inject

class StripProductVariationMetaData @Inject internal constructor(private val gson: Gson) {
    operator fun invoke(metadata: String?): String? {
        if (metadata == null) return null

        val filteredMetadata = gson.fromJson(metadata, JsonArray::class.java)
            .mapNotNull { it as? JsonObject }
            .filter { jsonObject ->
                val key = jsonObject[WCMetaData.KEY]?.asString ?: ""
                key in SUPPORTED_KEYS
            }
            .filter { jsonObject ->
                val value = jsonObject[WCMetaData.VALUE]?.asString ?: ""
                value.isBlank().not()
            }
            .toList()

        return if (filteredMetadata.isEmpty()) null else gson.toJson(filteredMetadata)
    }

    companion object {
        val SUPPORTED_KEYS: Set<String> = buildSet {
            addAll(WCProductVariationModel.SubscriptionMetadataKeys.ALL_KEYS)
        }
    }
}
