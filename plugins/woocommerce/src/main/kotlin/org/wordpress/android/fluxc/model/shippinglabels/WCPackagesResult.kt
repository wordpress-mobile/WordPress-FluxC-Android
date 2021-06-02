package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.annotations.SerializedName

data class WCPackagesResult(
    val customPackages: List<CustomPackage>,
    val predefinedOptions: List<PredefinedOption>
) {
    data class CustomPackage(
        @SerializedName("name") val title: String,
        @SerializedName("is_letter") val isLetter: Boolean,
        @SerializedName("inner_dimensions") val dimensions: String,
        @SerializedName("box_weight") val boxWeight: Float
    )

    data class PredefinedOption(
        val title: String,
        val predefinedPackages: List<PredefinedPackage>
    ) {
        data class PredefinedPackage(
            val id: String,
            val title: String,
            val isLetter: Boolean,
            val dimensions: String,
            val boxWeight: Float
        )
    }
}
