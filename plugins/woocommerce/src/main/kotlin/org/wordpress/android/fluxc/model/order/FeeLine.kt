package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

/**
 * Represents a fee line
 * We are reading only the id, the name and the total, as the tax is already included in the order totalTax
 */
class FeeLine {
    @SerializedName("id")
    val id: Long? = null

    @SerializedName("name")
    val name: String? = null

    @SerializedName("total")
    val total: String? = null
}
