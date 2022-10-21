package org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm

import com.google.gson.annotations.SerializedName

data class JitmDismissApiResponse(
    @SerializedName("data") val data: Boolean
)