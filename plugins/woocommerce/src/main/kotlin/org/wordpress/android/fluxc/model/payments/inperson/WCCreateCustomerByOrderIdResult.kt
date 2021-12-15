package org.wordpress.android.fluxc.model.payments.inperson

import com.google.gson.annotations.SerializedName

data class WCCreateCustomerByOrderIdResult(
    @SerializedName("id")
    val customerId: String?
)
