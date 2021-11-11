package org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class TrackerRestClient @Inject constructor(
    appContext: Context,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val requestBuilder: JetpackTunnelGsonRequestBuilder
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun sendTelemetry(appVersion: String, site: SiteModel): WooPayload<Unit> {
        val url = WOOCOMMERCE.tracker.pathWcTelemetry

        val response = requestBuilder.syncPostRequest(
            restClient = this,
            site = site,
            url = url,
            clazz = Unit::class.java,
            body = mapOf(
                "platform" to "android",
                "version" to appVersion
            )
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(Unit)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }
}
