package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.persistence.WCLeaderboardsSqlUtils.getCurrentLeaderboards
import org.wordpress.android.fluxc.persistence.WCLeaderboardsSqlUtils.insertNewLeaderboards
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
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchProductLeaderboards(
        site: SiteModel,
        unit: StatsGranularity = DAYS,
        startDate: String? = null,
        endDate: String? = null,
        quantity: Int? = null
    ): WooResult<List<WCTopPerformerProductModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchLeaderboards") {
            fetchAllLeaderboards(
                site,
                unit,
                getStartDateForProductsLeaderboards(site, unit, startDate),
                getEndDateForProductsLeaderboards(site, unit, endDate),
                quantity
            )
                .model
                ?.firstOrNull { it.type == PRODUCTS }
                ?.run { mapper.map(this, site, productStore, unit) }
                ?.let {
                    insertNewLeaderboards(it, site.id, unit)
                    getCurrentLeaderboards(site.id, unit)
                }
                ?.distinctBy { it.product.remoteProductId }
                ?.let { WooResult(it) }
                ?: getCurrentLeaderboards(site.id, unit)
                    .takeIf { it.isNotEmpty() }
                    ?.let { WooResult(it) }
                ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
        }
    }

    fun fetchCachedProductLeaderboards(
        site: SiteModel,
        unit: StatsGranularity
    ) = WooResult(getCurrentLeaderboards(site.id, unit))

    private suspend fun fetchAllLeaderboards(
        site: SiteModel,
        unit: StatsGranularity? = null,
        startDate: String? = null,
        endDate: String? = null,
        quantity: Int? = null
    ): WooResult<List<LeaderboardsApiResponse>> =
        with(restClient.fetchLeaderboards(site, unit, startDate, endDate, quantity)) {
            return when {
                isError -> WooResult(error)
                result != null -> WooResult(result.toList())
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
}
