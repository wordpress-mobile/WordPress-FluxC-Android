package org.wordpress.android.fluxc.model.attribute

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.fetchSingleStoredAttribute

data class WCProductAttributeModel(
    @SerializedName("id")
    val globalAttributeId: Int,
    val name: String = "",
    val position: Int = 0,
    val visible: Boolean = false,
    val variation: Boolean = false,
    var options: MutableList<String> = mutableListOf()
) {
    init {
        options.add(anyOption)
    }

    fun asGlobalAttribute(siteID: Int) =
            fetchSingleStoredAttribute(globalAttributeId, siteID)

    fun generateProductVariantOption(selectedOption: String) =
            takeIf { options.contains(selectedOption) }?.let {
                ProductVariantOption(
                        id = globalAttributeId.toLong(),
                        name = name,
                        option = selectedOption
                )
            }

    companion object {
        const val anyOption = "Any"
    }
}
