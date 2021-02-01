package org.wordpress.android.fluxc.model.attribute

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.fetchSingleStoredAttribute

data class WCProductAttributeModel(
    @SerializedName("id")
    val globalAttributeId: Int,
    val name: String = "",
    val position: Int = 0,
    val visible: Boolean = false,
    val options: List<String> = emptyList()
) {
    fun asGlobalAttribute(siteID: Int) =
            fetchSingleStoredAttribute(globalAttributeId, siteID)
}
