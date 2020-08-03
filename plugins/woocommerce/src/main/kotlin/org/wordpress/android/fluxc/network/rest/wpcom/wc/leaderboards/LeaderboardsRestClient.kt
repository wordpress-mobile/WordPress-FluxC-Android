package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Singleton

@Singleton
class LeaderboardsRestClient
constructor(
    appContext: Context?,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchLeaderboards(
        site: SiteModel,
        unit: StatsGranularity?,
        queryTimeRange: LongRange?,
        quantity: Int?
    ) = WOOCOMMERCE.leaderboards.pathV4Analytics
            .requestTo(site, unit, queryTimeRange, quantity)
            .handleResult()

    private suspend fun String.requestTo(
        site: SiteModel,
        unit: StatsGranularity?,
        queryTimeRange: LongRange?,
        quantity: Int?
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this@LeaderboardsRestClient,
            site,
            this,
            createParameters(unit, queryTimeRange, quantity),
            Array<LeaderboardsApiResponse>::class.java
    )

    private fun createParameters(unit: StatsGranularity?, queryTimeRange: LongRange?, quantity: Int?) = mapOf(
            "before" to (queryTimeRange?.endInclusive ?: "").toString(),
            "after" to (queryTimeRange?.start ?: "").toString(),
            "per_page" to quantity?.toString().orEmpty(),
            "interval" to (unit?.let { OrderStatsApiUnit.fromStatsGranularity(it).toString() } ?: "")
    ).filter { it.value.isNotEmpty() }

    private fun <T> JetpackResponse<T>.handleResult() =
            when (this) {
                is JetpackSuccess -> {
                    WooPayload(data)
                }
                is JetpackError -> {
                    WooPayload(error.toWooError())
                }
            }
}
