package org.wordpress.android.fluxc.wc.order

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderNoteApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderShipmentTrackingApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderStatusApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderSummaryApiResponse
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.utils.DateUtils
import kotlin.collections.MutableMap.MutableEntry
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
            datePaid = "1956-11-05T14:15:00Z"
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
                statusCount = it.total
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
                remoteTrackingId = it.tracking_id ?: ""
                trackingNumber = it.tracking_number ?: ""
                trackingProvider = it.tracking_provider ?: ""
                trackingLink = it.tracking_link ?: ""
                dateShipped = it.date_shipped ?: ""
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

    fun getOrderShipmentProvidersFromJson(json: String, siteId: Int): List<WCOrderShipmentProviderModel> {
        val providers = mutableListOf<WCOrderShipmentProviderModel>()
        val jsonElement = JsonParser().parse(json)
        jsonElement.asJsonObject.entrySet().forEach { countryEntry: MutableEntry<String, JsonElement> ->
            countryEntry.value.asJsonObject.entrySet().map { carrierEntry ->
                carrierEntry?.let { carrier ->
                    val provider = WCOrderShipmentProviderModel().apply {
                        localSiteId = siteId
                        this.country = countryEntry.key
                        this.carrierName = carrier.key
                        this.carrierLink = carrier.value.asString
                    }
                    providers.add(provider)
                }
            }
        }
        return providers
    }

    fun generateOrderShipmentProvider(siteId: Int): WCOrderShipmentProviderModel {
        return WCOrderShipmentProviderModel().apply {
            localSiteId = siteId
            country = "Australia"
            carrierName = "Amanda Test"
            carrierLink = "http://google.com"
        }
    }

    fun getOrderSummariesFromJsonString(json: String, siteId: Int): List<WCOrderSummaryModel> {
        val responseType = object : TypeToken<List<OrderSummaryApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderSummaryApiResponse> ?: emptyList()
        return converted.map { response ->
            WCOrderSummaryModel().apply {
                localSiteId = siteId
                remoteOrderId = response.id ?: 0
                dateCreated = response.dateCreatedGmt?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: ""
                dateModified = response.dateModifiedGmt?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: ""
            }
        }
    }

    fun getAndSaveTestSite(): SiteModel {
        var siteModel = SiteUtils.generateTestSite(556, "", "", false, true).apply {
            name = "Generic WP site"
        }
        SiteSqlUtils.insertOrUpdateSite(siteModel)
        siteModel = SiteSqlUtils.getSitesByNameOrUrlMatching("Generic").firstOrNull()
        assertNotNull(siteModel)

        return siteModel
    }

    fun getTestOrderSummaryList(site: SiteModel): List<WCOrderSummaryModel> {
        val json = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-summaries.json")
        val summaryList = getOrderSummariesFromJsonString(json, site.id)
        assertEquals(10, summaryList.size)

        return summaryList
    }
}
