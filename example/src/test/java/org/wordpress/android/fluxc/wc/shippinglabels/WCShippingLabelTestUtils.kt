package org.wordpress.android.fluxc.wc.shippinglabels

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel

object WCShippingLabelTestUtils {
    private fun generateSampleShippingLabel(
        remoteId: Long,
        orderId: Long = 12,
        siteId: Int = 6,
        carrierId: String = "",
        serviceName: String = "",
        status: String = "",
        packageName: String = "",
        rate: Float = 0F,
        refundableAmount: Float = 0F,
        currency: String = "",
        paperSize: String = ""
    ): WCShippingLabelModel {
        return WCShippingLabelModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteShippingLabelId = remoteId
            this.carrierId = carrierId
            this.serviceName = serviceName
            this.packageName = packageName
            this.status = status
            this.rate = rate
            this.refundableAmount = refundableAmount
            this.currency = currency
            this.paperSize = paperSize
        }
    }

    fun generateShippingLabelList(
        siteId: Int = 6,
        orderId: Long = 12,
        remoteShippingLabelId: Long = 0
    ): List<WCShippingLabelModel> {
        with(ArrayList<WCShippingLabelModel>()) {
            add(generateSampleShippingLabel(siteId = siteId, orderId = orderId, remoteId = remoteShippingLabelId + 1))
            add(generateSampleShippingLabel(siteId = siteId, orderId = orderId, remoteId = remoteShippingLabelId + 2))
            add(generateSampleShippingLabel(siteId = siteId, orderId = orderId, remoteId = remoteShippingLabelId + 3))
            add(generateSampleShippingLabel(siteId = siteId, orderId = orderId, remoteId = remoteShippingLabelId + 4))
            add(generateSampleShippingLabel(siteId = siteId, orderId = orderId, remoteId = remoteShippingLabelId + 5))
            return this
        }
    }
}
