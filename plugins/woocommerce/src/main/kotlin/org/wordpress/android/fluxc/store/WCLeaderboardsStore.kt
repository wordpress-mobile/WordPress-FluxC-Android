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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsRestClient
import org.wordpress.android.fluxc.persistence.dao.TopPerformerProductsDao
import org.wordpress.android.fluxc.persistence.entity.TopPerformerProductEntity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCLeaderboardsStore @Inject constructor(
    private val restClient: LeaderboardsRestClient,
    private val mapper: WCProductLeaderboardsMapper,
    private val coroutineEngine: CoroutineEngine,
    private val topPerformersDao: TopPerformerProductsDao,
    private val reportsRestClient: ReportsRestClient
) {
    @Suppress("Unused")
    fun observeTopPerformerProducts(
        site: SiteModel,
        datePeriod: String
    ): Flow<List<TopPerformerProductEntity>> =
        topPerformersDao
            .observeTopPerformerProducts(site.localId(), datePeriod)
            .distinctUntilChanged()

    @Suppress("Unused")
    suspend fun getCachedTopPerformerProducts(
        site: SiteModel,
        datePeriod: String
    ): List<TopPerformerProductEntity> =
        topPerformersDao.getTopPerformerProductsFor(site.localId(), datePeriod)

    suspend fun fetchTopPerformerProducts(
        site: SiteModel,
        granularity: StatsGranularity,
        quantity: Int = 5,
    ): WooResult<List<TopPerformerProductEntity>> {
        val startDate = granularity.startDateTime(site)
        val endDate = granularity.endDateTime(site)
        return fetchTopPerformerProducts(
            site = site,
            startDate = startDate,
            endDate = endDate,
            quantity = quantity
        )
    }

    suspend fun fetchTopPerformerProducts(
        site: SiteModel,
        startDate: String,
        endDate: String,
        quantity: Int = 5,
    ): WooResult<List<TopPerformerProductEntity>> {
        val response = reportsRestClient.fetchTopPerformerProducts(
            site = site,
            startDate = startDate,
            endDate = endDate,
            quantity = quantity
        )

        return when {
            response.isError -> WooResult(response.error)
            response.result != null -> {
                if(response.result.isEmpty()) return WooResult(WooError(GENERIC_ERROR, UNKNOWN))

                val period = DateUtils.getDatePeriod(startDate, endDate)
                val topPerformers = response.result.map { item ->
                    mapper.mapTopPerformerProductsEntity(item, site, period)
                }

                topPerformersDao.updateTopPerformerProductsFor(
                    localSiteId = site.localId(),
                    datePeriod = period,
                    topPerformerProducts = topPerformers
                )

                WooResult(topPerformers)
            }

            else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }
    }

    @Suppress("LongParameterList")
    private suspend fun fetchAllLeaderboards(
        site: SiteModel,
        startDate: String,
        endDate: String,
        quantity: Int?,
        addProductsPath: Boolean,
        forceRefresh: Boolean,
        interval: String,
    ): WooResult<List<LeaderboardsApiResponse>> {
        val response = restClient.fetchLeaderboards(
            site = site,
            startDate = startDate,
            endDate = endDate,
            quantity = quantity,
            addProductsPath = addProductsPath,
            interval = interval,
            forceRefresh = forceRefresh
        )

        return when {
            response.isError -> WooResult(response.error)
            response.result != null -> WooResult(response.result.toList())
            else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }
    }

    fun invalidateTopPerformers(site: SiteModel) {
        coroutineEngine.launch(AppLog.T.DB, this, "Invalidating top performer products") {
            val invalidatedTopPerformers =
                topPerformersDao.getTopPerformerProductsForSite(site.localId())
                    .map { it.copy(millisSinceLastUpdated = 0) }
            topPerformersDao.updateTopPerformerProductsForSite(site.localId(), invalidatedTopPerformers)
        }
    }
}
