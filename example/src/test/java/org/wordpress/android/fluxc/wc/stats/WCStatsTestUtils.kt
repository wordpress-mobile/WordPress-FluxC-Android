package org.wordpress.android.fluxc.wc.stats

import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCOrderStatsV4Model
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object WCStatsTestUtils {
    private val dateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss")
    }

    fun generateSampleStatsModel(
        localSiteId: Int = 6,
        unit: String = OrderStatsApiUnit.DAY.toString(),
        quantity: String = "30",
        startDate: String? = null,
        endDate: String = DateTimeFormatter.ofPattern("YYYY-MM-dd").format(LocalDateTime.now()),
        fields: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-stats-fields.json"),
        data: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-stats-data.json")
    ): WCOrderStatsModel {
        return WCOrderStatsModel().apply {
            this.localSiteId = localSiteId
            this.unit = unit
            this.quantity = quantity
            this.endDate = endDate
            this.fields = fields
            this.data = data
            this.date = endDate
            startDate?.let {
                this.startDate = it
                this.isCustomField = true
            }
        }
    }

    /**
     * Generates a sample [WCOrderStatsV4Model]
     */
    fun generateSampleRevenueStatsModel(
        localSiteId: Int = 6,
        interval: String = StatsGranularity.DAYS.toString(),
        startDate: String = dateTimeFormatter.format(LocalDate.now().atStartOfDay()),
        endDate: String = dateTimeFormatter.format(LocalDate.now().atTime(23, 59, 59)),
        data: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/v4-stats-data.json")
    ): WCOrderStatsV4Model {
        return WCOrderStatsV4Model().apply {
            this.localSiteId = localSiteId
            this.interval = interval
            this.endDate = endDate
            this.data = data
            this.startDate = startDate
        }
    }
}
