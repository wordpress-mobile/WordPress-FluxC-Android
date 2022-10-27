package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCLeaderboardsStore @Inject constructor(
    private val restClient: LeaderboardsRestClient,
    private val productStore: WCProductStore,
    private val mapper: WCProductLeaderboardsMapper,
    private val coroutineEngine: CoroutineEngine,
    private val topPerformersDao: TopPerformerProductsDao,
) {
    fun observeTopPerformerProducts(
        siteId: Long,
        granularity: StatsGranularity
    ): Flow<List<TopPerformerProductEntity>> =
        topPerformersDao
            .observeTopPerformerProducts(siteId, granularity.toString())
            .distinctUntilChanged()

    suspend fun getCachedTopPerformerProducts(
        siteId: Long,
        granularity: StatsGranularity
    ): List<TopPerformerProductEntity> =
        topPerformersDao.getTopPerformerProductsFor(siteId, granularity.toString())

    suspend fun fetchTopPerformerProducts(
        site: SiteModel,
        granularity: StatsGranularity = DAYS,
        quantity: Int? = null,
        addProductsPath: Boolean = false,
        forceRefresh: Boolean = false,
        startDate: String? = null,
        endDate: String? = null,
    ): WooResult<List<TopPerformerProductEntity>> =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchLeaderboards") {
            fetchAllLeaderboards(
                site,
                granularity,
                quantity,
                addProductsPath,
                forceRefresh,
                getStartDateForProductsLeaderboards(site, granularity, startDate),
                getEndDateForProductsLeaderboards(site, granularity, endDate),
            )
                .model
                ?.firstOrNull { it.type == PRODUCTS }
                ?.run {
                    mapper.mapTopPerformerProductsEntity(
                        this,
                        site,
                        productStore,
                        granularity
                    )
                }
                ?.let {
                    topPerformersDao.updateTopPerformerProductsFor(
                        site.siteId,
                        granularity.toString(),
                        it
                    )
                    WooResult(it)
                } ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }

    private suspend fun fetchAllLeaderboards(
        site: SiteModel,
        unit: StatsGranularity? = null,
        quantity: Int? = null,
        addProductsPath: Boolean = false,
        forceRefresh: Boolean,
        startDate: String? = null,
        endDate: String? = null,
    ): WooResult<List<LeaderboardsApiResponse>> {
        val fetchLeaderboards = restClient.fetchLeaderboards(
            site,
            unit,
            startDate,
            endDate,
            quantity,
            addProductsPath,
            forceRefresh
        )

        return when {
                fetchLeaderboards.isError -> WooResult(fetchLeaderboards.error)
                fetchLeaderboards.result != null -> WooResult(fetchLeaderboards.result.toList())
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }

    /**
     * Given a [startDate], formats the date based on the site's timezone in format yyyy-MM-dd'T'hh:mm:ss
     * If the start date is empty, fetches the date based on the [granularity]
     */
    private fun getStartDateForProductsLeaderboards(
        site: SiteModel,
        granularity: StatsGranularity,
        startDate: String?
    ): String {
        return if (startDate.isNullOrEmpty()) {
            when (granularity) {
                StatsGranularity.DAYS -> DateUtils.getStartDateForSite(site, DateUtils.getStartOfCurrentDay())
                StatsGranularity.WEEKS -> DateUtils.getFirstDayOfCurrentWeekBySite(site)
                StatsGranularity.MONTHS -> DateUtils.getFirstDayOfCurrentMonthBySite(site)
                StatsGranularity.YEARS -> DateUtils.getFirstDayOfCurrentYearBySite(site)
            }
        } else {
            DateUtils.getStartDateForSite(site, startDate)
        }
    }

    /**
     * Given a [endDate], formats the date based on the site's timezone in format yyyy-MM-dd'T'hh:mm:ss
     * If the end date is empty, fetches the date based on the [granularity]
     */
    private fun getEndDateForProductsLeaderboards(
        site: SiteModel,
        granularity: StatsGranularity,
        endDate: String?
    ): String {
        return if (endDate.isNullOrEmpty()) {
            when (granularity) {
                StatsGranularity.DAYS -> DateUtils.getEndDateForSite(site)
                StatsGranularity.WEEKS -> DateUtils.getLastDayOfCurrentWeekForSite(site)
                StatsGranularity.MONTHS -> DateUtils.getLastDayOfCurrentMonthForSite(site)
                StatsGranularity.YEARS -> DateUtils.getLastDayOfCurrentYearForSite(site)
            }
        } else {
            DateUtils.getEndDateForSite(site, endDate)
        }
    }

    fun invalidateTopPerformers(siteId: Long) {
        coroutineEngine.launch(AppLog.T.DB, this, "Invalidating top performer products") {
            val invalidatedTopPerformers =
                topPerformersDao.getTopPerformerProductsForSite(siteId)
                    .map { it.copy(millisSinceLastUpdated = 0) }
            topPerformersDao.updateTopPerformerProductsForSite(siteId, invalidatedTopPerformers)
        }
    }
}
