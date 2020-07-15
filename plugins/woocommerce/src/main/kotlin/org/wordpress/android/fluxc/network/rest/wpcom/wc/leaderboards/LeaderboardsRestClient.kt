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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
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
        site: SiteModel
    ) = WOOCOMMERCE.leaderboards.pathV4Analytics
            .requestTo(site)
            .handleResult()

    private suspend fun String.requestTo(
        site: SiteModel
    ) = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this@LeaderboardsRestClient,
            site,
            this,
            emptyMap(),
            Array<LeaderboardsApiResponse>::class.java
    )

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
