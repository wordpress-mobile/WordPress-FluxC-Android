package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCLeaderboardsStore @Inject constructor(
    private val restClient: LeaderboardsRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchAllLeaderboards(
        site: SiteModel,
        unit: OrderStatsApiUnit? = null,
        queryTimeRange: LongRange? = null,
        quantity: Int? = null
    ): WooResult<List<LeaderboardsApiResponse>> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchLeaderboards") {
                with(restClient.fetchLeaderboards(site, unit, queryTimeRange, quantity)) {
                    return@withDefaultContext when {
                        isError -> WooResult(error)
                        result != null -> WooResult(result.toList())
                        else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                    }
                }
            }

    suspend fun fetchProductLeaderboards(
        site: SiteModel,
        unit: OrderStatsApiUnit? = null,
        queryTimeRange: LongRange? = null,
        quantity: Int? = null
    ): WooResult<LeaderboardsApiResponse> =
            fetchAllLeaderboards(site, unit, queryTimeRange, quantity)
                    .model
                    ?.firstOrNull { it.type == PRODUCTS }
                    ?.run { WooResult(this) }
                    ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
}
