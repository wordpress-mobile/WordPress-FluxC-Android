package org.wordpress.android.fluxc.network.rest.wpcom.wc.system

import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wcapi.BaseWCAPIRestClient
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wcapi.WCAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WCAPISystemRestClient @Inject constructor(
        dispatcher: Dispatcher,
        private val wcAPIGsonRequestBuilder: WCAPIGsonRequestBuilder,
        @Named("regular") requestQueue: RequestQueue,
        userAgent: UserAgent
) : BaseWCAPIRestClient(dispatcher, requestQueue, userAgent) {

    suspend fun fetchSSR(site: SiteModel): WooPayload<SSRResponse> {
        val url = site.url + "/wp-json" + WOOCOMMERCE.system_status.pathV3

        val response = wcAPIGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            params = mapOf("_fields" to "environment,database,active_plugins,theme,settings,security,pages"),
            body = emptyMap(),
            clazz = SSRResponse::class.java,
            enableCaching = true,
            cacheTimeToLive = BaseRequest.DEFAULT_CACHE_LIFETIME,
            basicAuthKey = AUTH_KEY
        )
        return when (response) {
            is Success -> {
                WooPayload(response.data)
            }
            is Error -> {
                WooPayload(
                        WooError(
                        WooErrorType.GENERIC_ERROR,
                        BaseRequest.GenericErrorType.UNKNOWN,
                        response.error.message)
                )
            }
        }
    }

    data class SSRResponse(
            val environment: JsonElement? = null,
            val database: JsonElement? = null,
            @SerializedName("active_plugins") val activePlugins: JsonElement? = null,
            val theme: JsonElement? = null,
            val settings: JsonElement? = null,
            val security: JsonElement? = null,
            val pages: JsonElement? = null
    )
}
