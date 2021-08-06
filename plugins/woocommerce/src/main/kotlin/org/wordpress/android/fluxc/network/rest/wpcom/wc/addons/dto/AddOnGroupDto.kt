package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.addons.WCProductAddonModel

data class AddOnGroupDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("restrict_to_category_ids") val categoryIds: List<Int>,
    @SerializedName("fields") val addons: List<WCProductAddonModel>
)
