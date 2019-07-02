package org.wordpress.android.fluxc.persistence

import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.room.StatsBlock
import org.wordpress.android.fluxc.persistence.room.StatsDao
import org.wordpress.android.fluxc.utils.map
import javax.inject.Inject
import javax.inject.Singleton

const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

@Singleton
class StatsSqlUtils
@Inject constructor(
    private val statsDao: StatsDao
) {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.setDateFormat(DATE_FORMAT)
        builder.create()
    }

    fun <T> insert(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        item: T,
        replaceExistingData: Boolean,
        date: String? = null,
        postId: Long? = null
    ) {
        val json = gson.toJson(item)
        if (replaceExistingData) {
            statsDao.delete(site.id, blockType, statsType, date, postId)
        }
        statsDao.insertOrReplace(
                StatsBlock(
                        localSiteId = site.id,
                        blockType = blockType,
                        statsType = statsType,
                        date = date,
                        postId = postId,
                        json = json
                )
        )
    }

    fun <T> selectAll(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): List<T> {
        return statsDao.selectAll(site.id, blockType, statsType, date, postId).map { gson.fromJson(it, classOfT) }
    }

    fun <T> liveSelectAll(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): LiveData<List<T>> {
        return statsDao.liveSelectAll(site.id, blockType, statsType, date, postId)
                .map { it.map { json -> gson.fromJson(json, classOfT) } }
    }

    fun <T> select(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): T? {
        return statsDao.select(site.id, blockType, statsType, date, postId)?.let {
            gson.fromJson(it, classOfT)
        }
    }

    fun <T> liveSelect(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): LiveData<T> {
        return statsDao.liveSelect(site.id, blockType, statsType, date, postId).map {
            it.let { gson.fromJson(it, classOfT) }
        }
    }

    enum class StatsType {
        INSIGHTS,
        DAY,
        WEEK,
        MONTH,
        YEAR
    }

    enum class BlockType {
        ALL_TIME_INSIGHTS,
        MOST_POPULAR_INSIGHTS,
        LATEST_POST_DETAIL_INSIGHTS,
        DETAILED_POST_STATS,
        TODAYS_INSIGHTS,
        WP_COM_FOLLOWERS,
        EMAIL_FOLLOWERS,
        COMMENTS_INSIGHTS,
        TAGS_AND_CATEGORIES_INSIGHTS,
        POSTS_AND_PAGES_VIEWS,
        REFERRERS,
        CLICKS,
        VISITS_AND_VIEWS,
        COUNTRY_VIEWS,
        AUTHORS,
        SEARCH_TERMS,
        VIDEO_PLAYS,
        PUBLICIZE_INSIGHTS,
        POSTING_ACTIVITY
    }
}
