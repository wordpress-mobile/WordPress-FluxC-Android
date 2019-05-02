package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import org.wordpress.android.fluxc.persistence.room.StatsRequestDao
import org.wordpress.android.fluxc.persistence.room.StatsRequestDao.StatsRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRequestSqlUtils
@Inject constructor(private val statsRequestDao: StatsRequestDao) {
    fun insert(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        requestedItems: Int? = null,
        date: String? = null,
        postId: Long? = null
    ) {
        statsRequestDao.delete(site.id, blockType, statsType, date, postId)
        statsRequestDao.insertOrReplace(
                StatsRequest(
                        localSiteId = site.id,
                        blockType = blockType,
                        statsType = statsType,
                        date = date,
                        postId = postId,
                        timeStamp = System.currentTimeMillis(),
                        requestedItems = requestedItems
                )
        )
    }

    fun hasFreshRequest(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        requestedItems: Int? = null,
        after: Long = System.currentTimeMillis() - STALE_PERIOD,
        date: String? = null,
        postId: Long? = null
    ): Boolean {
        return statsRequestDao.select(site.id, blockType, statsType, date, postId, after, requestedItems) != null
    }

    companion object {
        private const val STALE_PERIOD = 5 * 60 * 1000
    }
}
