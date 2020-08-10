package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCOrderStatsModelTable
import com.wellsql.generated.WCTopPerformerProductModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object WCLeaderboardsSqlUtils {
    fun deleteCurrentLeaderboards(siteId: Int, unit: StatsGranularity) =
            WellSql.delete(WCTopPerformerProductModel::class.java)
                    .where()
                    .equals(WCOrderStatsModelTable.LOCAL_SITE_ID, siteId)
                    .equals(WCTopPerformerProductModelTable.UNIT, unit.toString())
                    .endWhere()
                    .execute()

    fun getCurrentLeaderboards(siteId: Int, unit: StatsGranularity) =
            WellSql.select(WCTopPerformerProductModel::class.java)
                    .where()
                    .equals(WCTopPerformerProductModelTable.LOCAL_SITE_ID, siteId)
                    .equals(WCTopPerformerProductModelTable.UNIT, unit.toString())
                    .endWhere()
                    .asModel
                    ?.toList()
                    .orEmpty()

    fun insertNewLeaderboards(leadearboards: List<WCTopPerformerProductModel>, siteId: Int, unit: StatsGranularity) {
        deleteCurrentLeaderboards(siteId, unit)
        WellSql.insert(leadearboards)
                .asSingleTransaction(true).execute()
    }
}
