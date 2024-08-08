package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCProductModel.AddOnsMetadataKeys
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys
import org.wordpress.android.fluxc.utils.EMPTY_JSON_ARRAY
import org.wordpress.android.fluxc.utils.isElementNullOrEmpty
import javax.inject.Inject

class StripProductMetaData @Inject internal constructor(private val gson: Gson) {
    operator fun invoke(metadata: String?): String {
        if (metadata.isNullOrEmpty()) return EMPTY_JSON_ARRAY

        return gson.fromJson(metadata, JsonArray::class.java)
            .mapNotNull { it as? JsonObject }
            .asSequence()
            .filter { jsonObject ->
                val isNullOrEmpty = jsonObject[WCMetaData.VALUE].isElementNullOrEmpty()
                jsonObject[WCMetaData.KEY]?.asString.orEmpty() in SUPPORTED_KEYS && isNullOrEmpty.not()
            }.toList()
            .takeIf { it.isNotEmpty() }
            ?.let { gson.toJson(it) } ?: EMPTY_JSON_ARRAY
    }

    companion object {
        val SUPPORTED_KEYS: Set<String> = buildSet {
            add(AddOnsMetadataKeys.ADDONS_METADATA_KEY)
            addAll(SubscriptionMetadataKeys.ALL_KEYS)
        }
    }
}
