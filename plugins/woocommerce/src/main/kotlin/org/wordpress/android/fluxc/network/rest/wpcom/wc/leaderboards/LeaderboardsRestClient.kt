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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.handleResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LeaderboardsRestClient @Inject constructor(
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    @Suppress("LongParameterList")
    suspend fun fetchLeaderboards(
        site: SiteModel,
        unit: StatsGranularity?,
        startDate: String?,
        endDate: String?,
        quantity: Int?,
        addProductsPath: Boolean = false,
        forceRefresh: Boolean
    ) = when (addProductsPath) {
        true -> WOOCOMMERCE.leaderboards.products.pathV4Analytics
        else -> WOOCOMMERCE.leaderboards.pathV4Analytics
    }.requestTo(site, unit, startDate, endDate, quantity, forceRefresh)
        .handleResult()

    @Suppress("LongParameterList")
    private suspend fun String.requestTo(
        site: SiteModel,
        unit: StatsGranularity?,
        startDate: String?,
        endDate: String?,
        quantity: Int?,
        forceRefresh: Boolean
    ): JetpackResponse<Array<LeaderboardsApiResponse>> = jetpackTunnelGsonRequestBuilder.syncGetRequest(
        this@LeaderboardsRestClient,
        site,
        this,
        createParameters(site, unit, startDate, endDate, quantity, forceRefresh),
        Array<LeaderboardsApiResponse>::class.java
    )

    @Suppress("LongParameterList")
    private fun createParameters(
        site: SiteModel,
        unit: StatsGranularity?,
        startDate: String?,
        endDate: String?,
        quantity: Int?,
        forceRefresh: Boolean
    ) = mapOf(
        "before" to (endDate ?: DateUtils.getEndDateForSite(site)).toString(),
        "after" to (startDate ?: (unit?.startDateTime(site) ?: "")).toString(),
        "per_page" to quantity?.toString().orEmpty(),
        "interval" to (unit?.let { OrderStatsApiUnit.fromStatsGranularity(it).toString() } ?: ""),
        "force_cache_refresh" to forceRefresh.toString()
    ).filter { it.value.isNotEmpty() }
}
