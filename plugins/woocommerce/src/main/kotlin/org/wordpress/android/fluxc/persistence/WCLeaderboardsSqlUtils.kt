package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WCTopPerformerProductModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel

object WCLeaderboardsSqlUtils {
    fun deleteCurrentLeaderboards() =
            WellSql.delete(WCTopPerformerProductModel::class.java).execute()

    fun getCurrentLeaderboards(site: SiteModel) =
            WellSql.select(WCTopPerformerProductModel::class.java)
                    .where()
                    .equals(WCTopPerformerProductModelTable.LOCAL_SITE_ID, site.id)
                    .endWhere()
                    .asModel
                    .toList()

    fun insertNewLeaderboards(leadearboards: List<WCTopPerformerProductModel>) {
        deleteCurrentLeaderboards()
        WellSql.insert(leadearboards).asSingleTransaction(true).execute()
    }
}
