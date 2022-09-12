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

    fun getCachedTopPerformerProducts(
        siteId: Long,
        granularity: StatsGranularity
    ): List<TopPerformerProductEntity> =
        topPerformersDao.getTopPerformerProductsFor(siteId, granularity.toString())

    suspend fun fetchTopPerformerProducts(
        site: SiteModel,
        granularity: StatsGranularity = DAYS,
        queryTimeRange: LongRange? = null,
        quantity: Int? = null,
        addProductsPath: Boolean = false,
        forceRefresh: Boolean = false
    ): WooResult<List<TopPerformerProductEntity>> =
        coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchLeaderboards") {
            fetchAllLeaderboards(
                site,
                granularity,
                queryTimeRange,
                quantity,
                addProductsPath,
                forceRefresh
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
        queryTimeRange: LongRange? = null,
        quantity: Int? = null,
        addProductsPath: Boolean = false,
        forceRefresh: Boolean
    ): WooResult<List<LeaderboardsApiResponse>> =
        with(
            restClient.fetchLeaderboards(
                site,
                unit,
                queryTimeRange,
                quantity,
                addProductsPath,
                forceRefresh
            )
        ) {
            return when {
                isError -> WooResult(error)
                result != null -> WooResult(result.toList())
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
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
