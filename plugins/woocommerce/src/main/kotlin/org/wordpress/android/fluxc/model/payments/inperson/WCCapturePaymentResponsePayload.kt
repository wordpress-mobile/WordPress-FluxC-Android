package org.wordpress.android.fluxc.model.payments.inperson

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError

class WCCapturePaymentResponsePayload(
    val site: SiteModel,
    val paymentId: String,
    val orderId: Long,
    val status: String?
) : Payload<WCCapturePaymentError?>() {
    constructor(
        error: WCCapturePaymentError,
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ) : this(site, paymentId, orderId, null) {
        this.error = error
    }
}

class WCCapturePaymentError(
    val type: WCCapturePaymentErrorType = GENERIC_ERROR,
    val message: String = ""
) : OnChangedError

sealed class WCCapturePaymentErrorType {
    data object GENERIC_ERROR : WCCapturePaymentErrorType()
    data object PAYMENT_ALREADY_CAPTURED : WCCapturePaymentErrorType()
    data object MISSING_ORDER : WCCapturePaymentErrorType()
    data object CAPTURE_ERROR : WCCapturePaymentErrorType()
    data object SERVER_ERROR : WCCapturePaymentErrorType()
    data object NETWORK_ERROR : WCCapturePaymentErrorType()
    data class AMOUNT_TOO_SMALL(
        val minAllowedAmountInStripeMinorUnit: Long,
        val currency: String
    ) : WCCapturePaymentErrorType()
}
