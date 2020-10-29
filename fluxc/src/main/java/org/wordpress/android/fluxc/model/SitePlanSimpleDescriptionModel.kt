package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName

data class SitePlanSimpleDescriptionModel(
    @SerializedName("ID") val planId: Int,
    @SerializedName("name") val name: String
)
