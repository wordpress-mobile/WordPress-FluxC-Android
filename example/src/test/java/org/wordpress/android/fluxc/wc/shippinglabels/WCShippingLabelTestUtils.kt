package org.wordpress.android.fluxc.wc.shippinglabels

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.GetPackageTypesResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.ShippingLabelRestClient.PrintShippingLabelApiResponse

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
        paperSize: String = "label",
        refund: String? = null,
        productNames: String = "[Woo T-shirt, Herman Chair]"
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
            this.productNames = productNames
            refund?.let { this.refund = it }
        }
    }

    fun generateShippingLabelList(
        siteId: Int = 6,
        orderId: Long = 12,
        remoteShippingLabelId: Long = 0
    ): List<WCShippingLabelModel> {
        with(ArrayList<WCShippingLabelModel>()) {
            add(generateSampleShippingLabel(
                    siteId = siteId,
                    orderId = orderId,
                    remoteId = remoteShippingLabelId + 1,
                    refund = "{\"status\": \"pending\",\"request_date\": 1604847663000}"
            ))
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

    fun generateSamplePrintShippingLabelApiResponse(): PrintShippingLabelApiResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/print-shipping-labels.json")
        val responseType = object : TypeToken<PrintShippingLabelApiResponse>() {}.type
        return Gson().fromJson(json, responseType) as? PrintShippingLabelApiResponse
    }

    fun generateSampleGetPackagesApiResponse(): GetPackageTypesResponse? {
        val json = UnitTestUtils.getStringFromResourceFile(
                this.javaClass,
                "wc/shipping-labels-packages.json"
        )
        val responseType = object : TypeToken<GetPackageTypesResponse>() {}.type
        return Gson().fromJson(json, responseType) as? GetPackageTypesResponse
    }
}
