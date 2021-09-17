package org.wordpress.android.fluxc.network.rest.wpcom.wc.system

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
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
class WooSystemRestClient @Inject constructor(
    dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    suspend fun fetchInstalledPlugins(site: SiteModel): WooPayload<ActivePluginsResponse> {
        val url = WOOCOMMERCE.system_status.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
                this,
                site,
                url,
                mapOf("_fields" to "active_plugins,inactive_plugins"),
                ActivePluginsResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    suspend fun fetchSSR(site: SiteModel): WooPayload<SSRResponse> {
        val url = WOOCOMMERCE.system_status.pathV3

        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            mapOf("_fields" to "environment,database,active_plugins,theme,settings,security,pages"),
            SSRResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> {
                WooPayload(response.data)
            }
            is JetpackError -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class ActivePluginsResponse(
        @SerializedName("active_plugins") private val activePlugins: List<SystemPluginModel>?,
        @SerializedName("inactive_plugins") private val inactivePlugins: List<SystemPluginModel>?
    ) {
        val plugins: List<SystemPluginModel>
            get() = activePlugins.orEmpty().map { it.copy(isActive = true) } + inactivePlugins.orEmpty()

        data class SystemPluginModel(
            val name: String,
            val version: String,
            val isActive: Boolean = false
        )
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
