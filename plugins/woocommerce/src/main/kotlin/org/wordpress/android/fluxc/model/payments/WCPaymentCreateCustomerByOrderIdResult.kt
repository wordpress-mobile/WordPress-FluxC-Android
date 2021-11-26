package org.wordpress.android.fluxc.model.payments

import com.google.gson.annotations.SerializedName

data class WCPaymentCreateCustomerByOrderIdResult(
    @SerializedName("id")
    val customerId: String?
)
