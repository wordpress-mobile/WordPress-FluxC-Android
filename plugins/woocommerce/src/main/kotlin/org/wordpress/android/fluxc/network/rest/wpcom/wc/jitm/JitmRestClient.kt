package org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.JPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class JitmRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent,
    private val wooNetwork: WooNetwork
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchJitmMessage(
        site: SiteModel,
        messagePath: String,
        query: String,
    ): WooPayload<Array<JITMApiResponse>> {
        val url = JPAPI.jitm.pathV4

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = mapOf(
                "message_path" to messagePath,
                "query" to query,
            ),
            clazz = Array<JITMApiResponse>::class.java
        )

        return response.toWooPayload()
    }

    suspend fun dismissJitmMessage(
        site: SiteModel,
        jitmId: String,
        featureClass: String,
    ): WooPayload<Boolean> {
        val url = JPAPI.jitm.pathV4

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = mapOf(
                "id" to jitmId,
                "feature_class" to featureClass
            ),
            clazz = Any::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(true)
            }
            is WPAPIResponse.Error -> {
                if (response.error.type == BaseRequest.GenericErrorType.NOT_FOUND) {
                    WooPayload(false)
                } else {
                    WooPayload(response.error.toWooError())
                }
            }
        }
    }
}
