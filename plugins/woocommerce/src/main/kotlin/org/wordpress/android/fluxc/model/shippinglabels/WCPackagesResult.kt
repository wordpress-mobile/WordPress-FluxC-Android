package org.wordpress.android.fluxc.model.shippinglabels

data class WCPackagesResult(
    val customPackages: List<CustomPackage>,
    val predefinedOptions: List<PredefinedOption>
) {
    data class CustomPackage(
        val title: String
    )

    data class PredefinedOption(
        val title: String,
        val predefinedPackages: List<PredefinedPackage>
    ) {
        data class PredefinedPackage(
            val title: String,
            val isLetter: Boolean,
            val dimensions: String
        )
    }
}
