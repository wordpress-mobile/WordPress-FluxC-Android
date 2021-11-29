package org.wordpress.android.fluxc.model.payments.inperson

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.CapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError

class CapturePaymentResponsePayload(
    val site: SiteModel,
    val paymentId: String,
    val orderId: Long,
    val status: String?
) : Payload<CapturePaymentError?>() {
    constructor(
        error: CapturePaymentError,
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ) : this(site, paymentId, orderId, null) {
        this.error = error
    }
}

class CapturePaymentError(
    val type: CapturePaymentErrorType = GENERIC_ERROR,
    val message: String = ""
) : OnChangedError

enum class CapturePaymentErrorType {
    GENERIC_ERROR,
    PAYMENT_ALREADY_CAPTURED,
    MISSING_ORDER,
    CAPTURE_ERROR,
    SERVER_ERROR,
    NETWORK_ERROR
}
