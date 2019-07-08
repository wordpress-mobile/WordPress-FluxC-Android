package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCRevenueStatsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit

object WCRevenueStatsSqlUtils {
    fun insertOrUpdateStats(stats: WCRevenueStatsModel): Int {
        val statsResult = WellSql.select(WCRevenueStatsModel::class.java)
                .where().beginGroup()
                .equals(WCRevenueStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                .equals(WCRevenueStatsModelTable.INTERVAL, stats.interval)
                .equals(WCRevenueStatsModelTable.START_DATE, stats.startDate)
                .equals(WCRevenueStatsModelTable.END_DATE, stats.endDate)
                .endGroup().endWhere()
                .asModel

        if (statsResult.isEmpty()) {
            // insert
            WellSql.insert(stats).asSingleTransaction(true).execute()
            return 1
        } else {
            // Update
            val oldId = statsResult[0].id
            return WellSql.update(WCRevenueStatsModel::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCRevenueStatsModel::class.java)).execute()
        }
    }

    private fun getRawStatsForSiteIntervalAndDate(
        site: SiteModel,
        interval: OrderStatsApiUnit,
        startDate: String,
        endDate: String
    ): WCRevenueStatsModel? {
        return WellSql.select(WCRevenueStatsModel::class.java)
                .where()
                .beginGroup()
                .equals(WCRevenueStatsModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCRevenueStatsModelTable.INTERVAL, interval)
                .equals(WCRevenueStatsModelTable.START_DATE, startDate)
                .equals(WCRevenueStatsModelTable.END_DATE, endDate)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }
}
