package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

data class WCMetaData(
    @SerializedName(ID) val id: Long,
    @SerializedName(KEY) val key: String?,
    @SerializedName(VALUE) val value: Any,
    @SerializedName(DISPLAY_KEY) val displayKey: String?,
    @SerializedName(DISPLAY_VALUE) val displayValue: Any?
){
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
    }
    object SubscriptionMetadataKeys {
        const val SUBSCRIPTION_RENEWAL = "_subscription_renewal"
    }
    object BundleMetadataKeys {
        const val BUNDLED_ITEM_ID = "_bundled_item_id"
    }
}
