package org.wordpress.android.fluxc.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class WCMetaData(
    @SerializedName(ID) val id: Long,
    @SerializedName(KEY) val key: String?,
    @SerializedName(VALUE) val value: Any,
    @SerializedName(DISPLAY_KEY) val displayKey: String?,
    @SerializedName(DISPLAY_VALUE) val displayValue: Any?
) {
    companion object {
        const val ID = "id"
        const val KEY = "key"
        const val VALUE = "value"
        const val DISPLAY_KEY = "display_key"
        const val DISPLAY_VALUE = "display_value"
        val SUPPORTED_KEYS: Set<String> = buildSet {
            add(SubscriptionMetadataKeys.SUBSCRIPTION_RENEWAL)
            add(BundleMetadataKeys.BUNDLED_ITEM_ID)
        }

        fun addAsMetadata(metadata: JsonArray, key: String, value: String) {
            val item = JsonObject().also {
                it.addProperty(KEY, key)
                it.addProperty(VALUE, value)
            }
            metadata.add(item)
        }
    }

    object SubscriptionMetadataKeys {
        const val SUBSCRIPTION_RENEWAL = "_subscription_renewal"
    }
    object BundleMetadataKeys {
        const val BUNDLED_ITEM_ID = "_bundled_item_id"
        const val BUNDLE_MIN_SIZE = "_bundle_min_size"
        const val BUNDLE_MAX_SIZE = "_bundle_max_size"
    }
}
