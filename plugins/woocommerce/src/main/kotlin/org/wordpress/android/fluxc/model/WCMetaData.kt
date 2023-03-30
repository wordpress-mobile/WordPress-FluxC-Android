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
        }
    }
    object SubscriptionMetadataKeys {
        const val SUBSCRIPTION_RENEWAL = "_subscription_renewal"
    }
}
