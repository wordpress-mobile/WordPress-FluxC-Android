package org.wordpress.android.fluxc.model

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class WCMetaData(
    @SerializedName(ID) val id: Long,
    @SerializedName(KEY) val key: String,
    @SerializedName(VALUE) val value: JsonElement,
    @SerializedName(DISPLAY_KEY) val displayKey: String? = null,
    @SerializedName(DISPLAY_VALUE) val displayValue: JsonElement? = null
) {
    /**
     * Verify if the Metadata key is not null or a internal store attribute
     * @return false if the `key` starts with the `_` character
     * @return true otherwise
     */
    val isDisplayable
        get() = key.startsWith('_').not()

    val valueAsString: String
        get() = value.toString().trim('"')

    val valueStrippedHtml: String
        get() = valueAsString.replace(htmlRegex, "")

    val isHtml: Boolean
        get() = valueAsString.contains(htmlRegex)

    val isJson: Boolean
        get() = value.isJsonObject || value.isJsonArray

    companion object {
        const val ID = "id"
        const val KEY = "key"
        const val VALUE = "value"
        const val DISPLAY_KEY = "display_key"
        const val DISPLAY_VALUE = "display_value"

        private val htmlRegex by lazy {
            Regex("<[^>]+>")
        }
        val SUPPORTED_KEYS: Set<String> = buildSet {
            add(SubscriptionMetadataKeys.SUBSCRIPTION_RENEWAL)
            add(BundleMetadataKeys.BUNDLED_ITEM_ID)
            addAll(OrderAttributionInfoKeys.ALL_KEYS)
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

    object OrderAttributionInfoKeys {
        const val SOURCE_TYPE = "_wc_order_attribution_source_type"
        const val CAMPAIGN = "_wc_order_attribution_utm_campaign"
        const val SOURCE = "_wc_order_attribution_utm_source"
        const val MEDIUM = "_wc_order_attribution_utm_medium"
        const val DEVICE_TYPE = "_wc_order_attribution_device_type"
        const val SESSION_PAGE_VIEWS = "_wc_order_attribution_session_pages"

        val ALL_KEYS
            get() = setOf(
                SOURCE_TYPE,
                CAMPAIGN,
                SOURCE,
                MEDIUM,
                DEVICE_TYPE,
                SESSION_PAGE_VIEWS
            )
    }
}
