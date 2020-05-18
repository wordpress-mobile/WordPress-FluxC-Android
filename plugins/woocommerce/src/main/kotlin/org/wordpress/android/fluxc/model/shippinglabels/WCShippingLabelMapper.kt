package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse
import javax.inject.Inject

class WCShippingLabelMapper
@Inject constructor() {
    fun map(response: ShippingLabelApiResponse, site: SiteModel): List<WCShippingLabelModel> {
        return response.labelsData?.map { labelItem ->
            WCShippingLabelModel().apply {
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
                refund = response.refund.toString()

                localSiteId = site.id
            }
        } ?: emptyList()
    }
}
