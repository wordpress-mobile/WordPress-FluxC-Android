package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.AUTHORS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.CLICKS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.REFERRERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeStatsSqlUtils
@Inject constructor(private val statsSqlUtils: StatsSqlUtils, private val statsUtils: StatsUtils) {
    fun insert(site: SiteModel, data: PostAndPageViewsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                POSTS_AND_PAGES_VIEWS,
                granularity.toStatsType(),
                data,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun insert(site: SiteModel, data: ReferrersResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                REFERRERS,
                granularity.toStatsType(),
                data,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun insert(site: SiteModel, data: ClicksResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                CLICKS,
                granularity.toStatsType(),
                data,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun insert(site: SiteModel, data: AuthorsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                AUTHORS,
                granularity.toStatsType(),
                data,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun selectPostAndPageViews(site: SiteModel, granularity: StatsGranularity, date: Date): PostAndPageViewsResponse? {
        return statsSqlUtils.select(
                site,
                POSTS_AND_PAGES_VIEWS,
                granularity.toStatsType(),
                PostAndPageViewsResponse::class.java,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun selectReferrers(site: SiteModel, granularity: StatsGranularity, date: Date): ReferrersResponse? {
        return statsSqlUtils.select(
                site,
                REFERRERS,
                granularity.toStatsType(),
                ReferrersResponse::class.java,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun selectClicks(site: SiteModel, granularity: StatsGranularity, date: Date): ClicksResponse? {
        return statsSqlUtils.select(
                site,
                CLICKS,
                granularity.toStatsType(),
                ClicksResponse::class.java,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    fun selectAuthors(site: SiteModel, granularity: StatsGranularity, date: Date): AuthorsResponse? {
        return statsSqlUtils.select(
                site,
                AUTHORS,
                granularity.toStatsType(),
                AuthorsResponse::class.java,
                statsUtils.getFormattedDate(site, granularity, date)
        )
    }

    private fun StatsGranularity.toStatsType(): StatsType {
        return when (this) {
            DAYS -> StatsType.DAY
            WEEKS -> StatsType.WEEK
            MONTHS -> StatsType.MONTH
            YEARS -> StatsType.YEAR
        }
    }
}
