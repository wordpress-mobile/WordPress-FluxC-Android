package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel

object WCLeaderboardsSqlUtils {
    fun deleteCurrentLeaderboards() =
            WellSql.delete(WCTopPerformerProductModel::class.java).execute()

    fun getCurrentLeaderboards() =
            WellSql.select(WCTopPerformerProductModel::class.java)
                    .asModel
                    .toList()

    fun insertNewLeaderboards(leadearboards: List<WCTopPerformerProductModel>) {
        deleteCurrentLeaderboards()
        WellSql.insert(leadearboards).asSingleTransaction(true).execute()
    }
}
