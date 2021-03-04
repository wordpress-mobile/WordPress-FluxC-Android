package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class PurchaseShippingLabelApiResponse(
    @SerializedName("success")
    val isSuccess: Boolean = false,
    @SerializedName("labels")
    val labels: List<LabelItem>? = null
)
