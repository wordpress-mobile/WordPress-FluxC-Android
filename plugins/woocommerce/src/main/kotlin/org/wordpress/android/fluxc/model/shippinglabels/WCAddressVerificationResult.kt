package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress

sealed class WCAddressVerificationResult {
    class Valid(val suggestedAddress: ShippingLabelAddress) : WCAddressVerificationResult()
    class InvalidAddress(val message: String) : WCAddressVerificationResult()
    class InvalidRequest(val message: String) : WCAddressVerificationResult()
}
