package org.wordpress.android.fluxc.wc.stats

import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object WCStatsTestUtils {
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
}
