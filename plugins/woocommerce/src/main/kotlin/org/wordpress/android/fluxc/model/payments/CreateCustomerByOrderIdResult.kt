package org.wordpress.android.fluxc.model.payments

import com.google.gson.annotations.SerializedName

data class CreateCustomerByOrderIdResult(
    @SerializedName("id")
    val customerId: String?
)
