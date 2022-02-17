package org.wordpress.android.fluxc.model.order

import com.google.gson.annotations.SerializedName

/**
 * Represents a fee line.
 */
class FeeLine {
    @SerializedName("id")
    var id: Long? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("total")
    var total: String? = null

    @SerializedName("total_tax")
    var totalTax: String? = null

    @SerializedName("tax_status")
    var taxStatus: FeeLineTaxStatus? = null
}

enum class FeeLineTaxStatus(val value: String) {
    @SerializedName("taxable")
    Taxable("taxable"),
    @SerializedName("none")
    None("none")
}
