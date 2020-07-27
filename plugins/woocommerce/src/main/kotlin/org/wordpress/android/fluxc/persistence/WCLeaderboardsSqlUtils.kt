package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsModel

object WCLeaderboardsSqlUtils {
    fun deleteCurrentLeaderboards() =
            WellSql.delete(WCProductLeaderboardsModel::class.java).execute()

    fun getCurrentLeaderboards() =
            WellSql.select(WCProductLeaderboardsModel::class.java)
                    .asModel
                    .toList()
                    .firstOrNull()

    fun insertNewLeaderboards(leadearboards: WCProductLeaderboardsModel) {
        deleteCurrentLeaderboards()
        WellSql.insert(leadearboards).asSingleTransaction(true).execute()
    }
}
