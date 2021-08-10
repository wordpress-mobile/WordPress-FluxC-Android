package org.wordpress.android.fluxc.model.pay

import com.google.gson.annotations.SerializedName

data class WCPaymentCreateCustomerByOrderIdResult(
    @SerializedName("id")
    val customerId: String?
)
