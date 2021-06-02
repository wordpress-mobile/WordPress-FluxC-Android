package org.wordpress.android.fluxc.model.pay

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.Store.OnChangedError

class WCCapturePaymentResponsePayload(
    val site: SiteModel,
    val paymentId: String,
    val orderId: Long,
    val status: String?
) : Payload<WCCapturePaymentError?>() {
    public constructor(
        error: WCCapturePaymentError,
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ) : this(site, paymentId, orderId, null) {
        this.error = error
    }
}

class WCCapturePaymentError(
    val type: WCCapturePaymentErrorType = WCCapturePaymentErrorType.GENERIC_ERROR,
    val message: String = ""
) : OnChangedError

enum class WCCapturePaymentErrorType {
    GENERIC_ERROR,
    PAYMENT_ALREADY_CAPTURED,
    MISSING_ORDER,
    CAPTURE_ERROR,
    SERVER_ERROR,
    NETWORK_ERROR
}
