package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderStatsModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.WCOrderStatsModel

object WCStatsSqlUtils {
    fun insertOrUpdateStats(stats: WCOrderStatsModel): Int {
        val statsResult = WellSql.select(WCOrderStatsModel::class.java)
                .where().beginGroup()
                .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, stats.localSiteId)
                .equals(WCOrderStatsModelTable.UNIT, stats.unit)
                .endGroup().endWhere()
                .asModel

        if (statsResult.isEmpty()) {
            // Insert
            WellSql.insert(stats).asSingleTransaction(true).execute()
            return 1
        } else {
            // Update
            val oldId = statsResult[0].id
            return WellSql.update(WCOrderStatsModel::class.java).whereId(oldId)
                    .put(stats, UpdateAllExceptId(WCOrderStatsModel::class.java)).execute()
        }
    }
}
