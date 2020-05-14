package org.wordpress.android.fluxc.wc.shippinglabels

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse

object WCShippingLabelTestUtils {
    private fun generateSampleShippingLabel(
        remoteId: Long,
        orderId: Long = 12,
        siteId: Int = 6,
        carrierId: String = "usps",
        serviceName: String = "USPS - Priority Mail",
        status: String = "PURCHASED",
        packageName: String = "Small Flat Rate Box",
        rate: Float = 7.65F,
        refundableAmount: Float = 7.65F,
        currency: String = "USD",
        paperSize: String = "label"
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

    fun generateSampleShippingLabelApiResponse(): ShippingLabelApiResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/shipping-labels.json")
        val responseType = object : TypeToken<ShippingLabelApiResponse>() {}.type
        return Gson().fromJson(json, responseType) as? ShippingLabelApiResponse
    }
}
