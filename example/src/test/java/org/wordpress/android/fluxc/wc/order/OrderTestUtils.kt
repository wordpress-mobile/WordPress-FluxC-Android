package org.wordpress.android.fluxc.wc.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderNoteApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderShipmentTrackingApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatusApiResponse

object OrderTestUtils {
    fun generateSampleOrder(
        remoteId: Long,
        orderStatus: String = CoreOrderStatus.PROCESSING.value,
        siteId: Int = 6
    ): WCOrderModel {
        return WCOrderModel().apply {
            remoteOrderId = remoteId
            localSiteId = siteId
            status = orderStatus
            dateCreated = "1955-11-05T14:15:00Z"
            currency = "USD"
            total = "10.0"
        }
    }

    fun getOrderNotesFromJsonString(json: String, siteId: Int, orderId: Int): List<WCOrderNoteModel> {
        val responseType = object : TypeToken<List<OrderNoteApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderNoteApiResponse> ?: emptyList()
        return converted.map {
            WCOrderNoteModel().apply {
                remoteNoteId = it.id ?: 0
                dateCreated = "${it.date_created_gmt}Z"
                note = it.note ?: ""
                isCustomerNote = it.customer_note
                localSiteId = siteId
                localOrderId = orderId
            }
        }
    }

    fun generateSampleNote(remoteId: Long, siteId: Int, orderId: Int): WCOrderNoteModel {
        return WCOrderNoteModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteNoteId = remoteId
            dateCreated = "1955-11-05T14:15:00Z"
            note = "This is a test note"
            isCustomerNote = true
        }
    }

    fun getOrderStatusOptionsFromJson(json: String, siteId: Int): List<WCOrderStatusModel> {
        val responseType = object : TypeToken<List<OrderStatusApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderStatusApiResponse> ?: emptyList()
        return converted.map {
            WCOrderStatusModel().apply {
                localSiteId = siteId
                statusKey = it.slug ?: ""
                label = it.name ?: ""
            }
        }
    }

    fun getOrderShipmentTrackingsFromJson(
        json: String,
        siteId: Int,
        orderId: Int
    ): List<WCOrderShipmentTrackingModel> {
        val responseType = object : TypeToken<List<OrderShipmentTrackingApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderShipmentTrackingApiResponse> ?: emptyList()
        return converted.map {
            WCOrderShipmentTrackingModel().apply {
                localSiteId = siteId
                localOrderId = orderId
            }
        }
    }

    fun generateOrderShipmentTracking(siteId: Int, orderId: Int): WCOrderShipmentTrackingModel {
        return WCOrderShipmentTrackingModel().apply {
            localSiteId = siteId
            localOrderId = orderId
            remoteTrackingId = "3290834092801"
            trackingNumber = "ZZ9939921"
            trackingProvider = "USPS"
            trackingLink = ""
            dateShipped = "2019-01-01"
        }
    }
}
