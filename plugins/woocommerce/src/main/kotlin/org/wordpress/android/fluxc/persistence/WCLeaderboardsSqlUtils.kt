package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCTopPerformerProductModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object WCLeaderboardsSqlUtils {
    fun deleteCurrentLeaderboards(unit: StatsGranularity) =
            WellSql.delete(WCTopPerformerProductModel::class.java)
                    .where()
                    .equals(WCTopPerformerProductModelTable.UNIT, unit.toString())
                    .endWhere()
                    .execute()

    fun getCurrentLeaderboards(site: SiteModel, unit: StatsGranularity) =
            WellSql.select(WCTopPerformerProductModel::class.java)
                    .where()
                    .equals(WCTopPerformerProductModelTable.LOCAL_SITE_ID, site.id)
                    .equals(WCTopPerformerProductModelTable.UNIT, unit.toString())
                    .endWhere()
                    .asModel
                    .toList()
                    .orEmpty()//required since WellSql Java implementation does return null

    fun insertNewLeaderboards(leadearboards: List<WCTopPerformerProductModel>, unit: StatsGranularity) {
        deleteCurrentLeaderboards(unit)
        WellSql.insert(leadearboards).asSingleTransaction(true).execute()
    }
}
