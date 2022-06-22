package org.wordpress.android.fluxc.network.rest.wpcom.wc

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.UpdateSettingRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WooCommerceRestClient @Inject constructor(
    appContext: Context,
    private val dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to the root wp-json endpoint (`/`) via the Jetpack tunnel (see [JetpackTunnelGsonRequest])
     * for the given [SiteModel], and parses through the `namespaces` field in the result for supported versions
     * of the Woo API.
     *
     */
    suspend fun fetchSupportedWooApiVersion(site: SiteModel): WooPayload<RootWPAPIRestResponse> {
        val url = "/"
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            mapOf("_fields" to "authentication,namespaces"),
            RootWPAPIRestResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * Makes a GET call to `/wc/v3/settings/general` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving site settings for the given WooCommerce [SiteModel].
     *
     */
    suspend fun fetchSiteSettingsGeneral(site: SiteModel): WooPayload<List<SiteSettingsResponse>> {
        val url = WOOCOMMERCE.settings.general.pathV3
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            emptyMap(),
            Array<SiteSettingsResponse>::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data?.toList())
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    suspend fun fetchSiteSettingsProducts(site: SiteModel): WooPayload<List<SiteSettingsResponse>> {
        val url = WOOCOMMERCE.settings.products.pathV3
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            emptyMap(),
            Array<SiteSettingsResponse>::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data?.toList())
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * Updates an option in the Site Setting.
     * @param groupId Possible group ID's: https://woocommerce.github.io/woocommerce-rest-api-docs/?shell#list-all-settings-groups
     * @param optionId The particular option to be updated.
     */
    suspend fun updateSiteSettingOption(
        site: SiteModel,
        request: UpdateSettingRequest,
        groupId: String,
        optionId: String
    ): WooPayload<SiteSettingResponse> {
        val url = WOOCOMMERCE.settings.group(groupId).id(optionId).pathV3
        val params = request.toNetworkRequest()

        val response = jetpackTunnelGsonRequestBuilder.syncPutRequest(
            this,
            site,
            url,
            params,
            SiteSettingResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    private fun UpdateSettingRequest.toNetworkRequest(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("value", value)
        }
    }
}
