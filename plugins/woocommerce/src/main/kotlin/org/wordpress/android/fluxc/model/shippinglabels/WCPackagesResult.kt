package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress

sealed class WCPackagesResult {
    data class Default(val title: String) : WCPackagesResult()
    data class Custom(val title: String) : WCPackagesResult()
}
