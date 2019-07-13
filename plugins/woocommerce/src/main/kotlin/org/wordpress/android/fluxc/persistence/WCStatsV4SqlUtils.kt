package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderStatsV4ModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatsV4Model
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object WCStatsV4SqlUtils {
    fun insertOrUpdateStats(stats: WCOrderStatsV4Model): Int {
        val statsResult = WellSql.select(WCOrderStatsV4Model::class.java)
                .where().beginGroup()
                .equals(WCOrderStatsV4ModelTable.LOCAL_SITE_ID, stats.localSiteId)
                .equals(WCOrderStatsV4ModelTable.INTERVAL, stats.interval)
                .equals(WCOrderStatsV4ModelTable.START_DATE, stats.startDate)
                .equals(WCOrderStatsV4ModelTable.END_DATE, stats.endDate)
                .endGroup().endWhere()
                .asModel

        return if (statsResult.isEmpty()) {
            // insert
            WellSql.insert(stats).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = statsResult[0].id
            WellSql.update(WCOrderStatsV4Model::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCOrderStatsV4Model::class.java)).execute()
        }
    }

    fun getRawStatsForSiteIntervalAndDate(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String,
        endDate: String
    ): WCOrderStatsV4Model? {
        return WellSql.select(WCOrderStatsV4Model::class.java)
                .where()
                .beginGroup()
                .equals(WCOrderStatsV4ModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCOrderStatsV4ModelTable.INTERVAL, granularity)
                .equals(WCOrderStatsV4ModelTable.START_DATE, startDate)
                .equals(WCOrderStatsV4ModelTable.END_DATE, endDate)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }
}
