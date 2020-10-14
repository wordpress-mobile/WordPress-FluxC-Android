package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress

sealed class WCAddressVerificationResult {
    class Valid(val suggestedAddress: ShippingLabelAddress) : WCAddressVerificationResult()
    class Invalid(val message: String) : WCAddressVerificationResult()
}
