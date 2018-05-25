package org.wordpress.android.fluxc.wc.stats

import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.WCOrderStatsModel

object WCStatsTestUtils {
    fun generateSampleStatsModel(
        localSiteId: Int = 6,
        unit: String = "day",
        fields: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-stats-fields.json"),
        data: String = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order-stats-data.json")
    ): WCOrderStatsModel {
        return WCOrderStatsModel().apply {
            this.localSiteId = localSiteId
            this.unit = unit
            this.fields = fields
            this.data = data
        }
    }
}
