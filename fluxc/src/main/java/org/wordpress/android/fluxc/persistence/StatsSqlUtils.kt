package org.wordpress.android.fluxc.persistence

import android.arch.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.map
import org.wordpress.android.fluxc.persistence.room.StatsDao
import org.wordpress.android.fluxc.persistence.room.StatsDao.StatsBlock
import javax.inject.Inject
import javax.inject.Singleton

const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

@Singleton
class StatsSqlUtils
@Inject constructor(private val statsDao: StatsDao) {
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
        val currentId = if (replaceExistingData) {
            val currentItem = statsDao.select(site.id, blockType, statsType, date, postId)
            currentItem?.id
        } else {
            null
        }
        statsDao.insertOrReplace(StatsBlock(currentId, site.id, blockType, statsType, date, postId, json))
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
                .map { results -> results.map { gson.fromJson(it.json, classOfT) } }
    }

    fun <T> selectAll(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): List<T> {
        return statsDao.selectAll(site.id, blockType, statsType, date, postId).map { gson.fromJson(it.json, classOfT) }
    }

    fun <T> liveSelect(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): LiveData<T> {
        return statsDao.liveSelect(site.id, blockType, statsType, date, postId).map { gson.fromJson(it.json, classOfT) }
    }

    fun <T> select(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): T? {
        val statsBlock = statsDao.select(site.id, blockType, statsType, date, postId)
        return statsBlock?.let { gson.fromJson(statsBlock.json, classOfT) }
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
