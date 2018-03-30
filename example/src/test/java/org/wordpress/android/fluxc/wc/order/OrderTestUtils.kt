package org.wordpress.android.fluxc.wc.order

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus

object OrderTestUtils {
    fun generateSampleOrder(remoteId: Long): WCOrderModel = generateSampleOrder(remoteId, OrderStatus.PROCESSING)

    fun generateSampleOrder(remoteId: Long, status: String): WCOrderModel {
        val example = WCOrderModel().apply { remoteOrderId = remoteId }
        example.localSiteId = 6
        example.status = status
        return example
    }
}
