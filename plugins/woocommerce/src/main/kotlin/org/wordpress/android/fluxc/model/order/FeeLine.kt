package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

/**
 * Represents a fee line.
 */
class FeeLine {
    @SerializedName("id")
    val id: Long? = null

    @SerializedName("name")
    val name: String? = null

    @SerializedName("total")
    val total: String? = null

    @SerializedName("total_tax")
    val totalTax: String? = null

    @SerializedName("tax_status")
    val taxStatus: String? = null
}

enum class FeeLineTaxStatus {
    @SerializedName("taxable")
    Taxable,
    @SerializedName("none")
    None
}
