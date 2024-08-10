package org.wordpress.android.fluxc.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class WCMetaData(
    @SerializedName(ID)
    val id: Long,
    @SerializedName(KEY)
    val key: String,
    @JsonAdapter(WCMetaDataValue.WCMetaDataValueJsonAdapter::class)
    @SerializedName(VALUE)
    val value: WCMetaDataValue,
    @SerializedName(DISPLAY_KEY)
    val displayKey: String? = null,
    @JsonAdapter(WCMetaDataValue.WCMetaDataValueJsonAdapter::class)
    @SerializedName(DISPLAY_VALUE)
    val displayValue: WCMetaDataValue? = null
) {
    constructor(id: Long, key: String, value: String) : this(id, key, WCMetaDataValue.fromRawString(value))

    /**
     * Verify if the Metadata key is not null or a internal store attribute
     * @return false if the `key` starts with the `_` character
     * @return true otherwise
     */
    val isDisplayable
        get() = key.startsWith('_').not()

    val valueAsString: String
        get() = value.stringValue.orEmpty()

    val valueStrippedHtml: String
        get() = valueAsString.replace(htmlRegex, "")

    val isHtml: Boolean
        get() = valueAsString.contains(htmlRegex)

    val isJson: Boolean
        get() = value is WCMetaDataValue.JsonObjectValue || value is WCMetaDataValue.JsonArrayValue

    internal fun toJson(): JsonObject {
        return JsonObject().also {
            it.addProperty(ID, id)
            it.addProperty(KEY, key)
            it.add(VALUE, value.jsonValue)
            displayKey?.let { key -> it.addProperty(DISPLAY_KEY, key) }
            displayValue?.let { value -> it.add(DISPLAY_VALUE, value.jsonValue) }
        }
    }

    companion object {
        private const val ID = "id"
        const val KEY = "key"
        const val VALUE = "value"
        private const val DISPLAY_KEY = "display_key"
        private const val DISPLAY_VALUE = "display_value"

        private val htmlRegex by lazy {
            Regex("<[^>]+>")
        }

        val SUPPORTED_KEYS: Set<String> = buildSet {
            add(SubscriptionMetadataKeys.SUBSCRIPTION_RENEWAL)
            add(BundleMetadataKeys.BUNDLED_ITEM_ID)
            addAll(OrderAttributionInfoKeys.ALL_KEYS)
        }

        internal fun fromJson(json: JsonObject): WCMetaData? {
            return if (json.has(ID) && json.has(KEY) && json.has(VALUE)) {
                WCMetaData(
                    id = json[ID].asLong,
                    key = json[KEY].asString,
                    value = WCMetaDataValue.fromJsonElement(json[VALUE]),
                    displayKey = json[DISPLAY_KEY]?.asString,
                    displayValue = json[DISPLAY_VALUE]?.let { WCMetaDataValue.fromJsonElement(it) }
                )
            } else null
        }

        fun addAsMetadata(metadata: JsonArray, key: String, value: String) {
            val item = JsonObject().also {
                it.addProperty(ID, 0)
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

operator fun List<WCMetaData>.get(key: String) = firstOrNull { it.key == key }

