package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.annotations.SerializedName

data class WCShippingAccountSettings(
    val canManagePayments: Boolean,
    val selectedPaymentMethodId: Int?,
    val paymentMethods: List<WCPaymentMethod>,
    val lastUsedBoxId: String?
)

data class WCPaymentMethod(
    @SerializedName("payment_method_id")
    val paymentMethodId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("card_type")
    val cardType: String,
    @SerializedName("card_digits")
    val cardDigits: String,
    @SerializedName("expiry")
    val expiry: String
)
