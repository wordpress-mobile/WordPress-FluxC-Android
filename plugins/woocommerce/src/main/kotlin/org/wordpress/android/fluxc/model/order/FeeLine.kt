package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

/**
 * Represents a fee line
 * We are reading only the name and the total, as the tax is already included in the order totalTax
 */
class FeeLine {
    @SerializedName("name")
    val name: String? = null

    @SerializedName("total")
    val total: String? = null
}
