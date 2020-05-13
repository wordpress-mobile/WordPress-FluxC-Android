package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse.LabelItem
import javax.inject.Inject

class WCShippingLabelMapper
@Inject constructor() {
    fun map(response: ShippingLabelApiResponse, labelItem: LabelItem): WCShippingLabelModel {
        return WCShippingLabelModel().apply {
            remoteShippingLabelId = labelItem.labelId ?: 0L
            trackingNumber = labelItem.trackingNumber ?: ""
            carrierId = labelItem.carrierId ?: ""
            serviceName = labelItem.serviceName ?: ""
            status = labelItem.status ?: ""
            packageName = labelItem.packageName ?: ""
            rate = labelItem.rate?.toFloat() ?: 0F
            refundableAmount = labelItem.refundableAmount?.toFloat() ?: 0F
            currency = labelItem.currency ?: ""
            productNames = labelItem.productNames.toString()

            localOrderId = response.orderId ?: 0L
            paperSize = response.paperSize ?: ""
            storeOptions = response.storeOptions.toString()
            formData = response.formData.toString()
        }
    }
}
