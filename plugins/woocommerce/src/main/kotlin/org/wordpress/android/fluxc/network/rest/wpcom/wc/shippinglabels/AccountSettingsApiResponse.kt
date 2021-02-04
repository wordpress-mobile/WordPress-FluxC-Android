package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.shippinglabels.WCPaymentMethod

data class AccountSettingsApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("formData") val formData: FormData,
    @SerializedName("formMeta") val formMeta: FormMeta,
    @SerializedName("userMeta") val userMeta: UserMeta
) {
    data class FormData(
        @SerializedName("selected_payment_method_id") val selectedPaymentId: Int?
    )

    data class FormMeta(
        @SerializedName("can_manage_payments") val canManagePayments: Boolean,
        @SerializedName("payment_methods") val paymentMethods: List<WCPaymentMethod>?
    )

    data class UserMeta(
        @SerializedName("last_box_id") val lastBoxId: String?
    )
}
