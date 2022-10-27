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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
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
        startDate: String,
        endDate: String,
        quantity: Int?,
        forceRefresh: Boolean,
        interval: String = "",
        addProductsPath: Boolean = false,
    ): WooPayload<Array<LeaderboardsApiResponse>> {
        val url = when (addProductsPath) {
            true -> WOOCOMMERCE.leaderboards.products.pathV4Analytics
            else -> WOOCOMMERCE.leaderboards.pathV4Analytics
        }

        val parameters = createParameters(startDate, endDate, quantity, forceRefresh, interval)

        return jetpackTunnelGsonRequestBuilder.syncGetRequest(
            restClient = this@LeaderboardsRestClient,
            site = site,
            url = url,
            params = parameters,
            clazz = Array<LeaderboardsApiResponse>::class.java
        ).handleResult()
    }

    @Suppress("LongParameterList")
    private fun createParameters(
        startDate: String,
        endDate: String,
        quantity: Int?,
        forceRefresh: Boolean,
        interval: String = ""
    ) = mapOf(
        "before" to endDate,
        "after" to startDate,
        "per_page" to quantity?.toString().orEmpty(),
        "interval" to interval,
        "force_cache_refresh" to forceRefresh.toString()
    ).filter { it.value.isNotEmpty() }
}
