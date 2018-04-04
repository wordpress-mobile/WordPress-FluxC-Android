package org.wordpress.android.fluxc.wc.order

import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatus

object OrderTestUtils {
    fun generateSampleOrder(remoteId: Long): WCOrderModel = generateSampleOrder(remoteId, OrderStatus.PROCESSING)

    fun generateSampleOrder(remoteId: Long, orderStatus: String): WCOrderModel {
        return WCOrderModel().apply {
            remoteOrderId = remoteId
            localSiteId = 6
            status = orderStatus
            dateCreated = "1955-11-05T14:15:00Z"
            currency = "USD"
            total = "10.0"
        }
    }
}
