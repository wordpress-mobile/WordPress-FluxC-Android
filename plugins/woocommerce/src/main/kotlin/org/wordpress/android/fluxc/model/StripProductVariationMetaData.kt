package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import javax.inject.Inject

class StripProductVariationMetaData @Inject internal constructor(private val gson: Gson) {
    operator fun invoke(metadata: String?): String? {
        if (metadata == null) return null

        return gson.fromJson(metadata, JsonArray::class.java)
            .mapNotNull { it as? JsonObject }
            .asSequence()
            .filter { jsonObject ->
                jsonObject[WCMetaData.KEY]?.asString.orEmpty() in SUPPORTED_KEYS &&
                    jsonObject[WCMetaData.VALUE]?.asString.orEmpty().isNotBlank()
            }.toList()
            .takeIf { it.isNotEmpty() }
            ?.let { gson.toJson(it) }
    }

    companion object {
        val SUPPORTED_KEYS: Set<String> = buildSet {
            addAll(WCProductVariationModel.SubscriptionMetadataKeys.ALL_KEYS)
        }
    }
}
