package org.wordpress.android.fluxc.network.rest.wpcom.plugin

import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient.pluginModelFromResponse
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginWPComRestResponse.FetchPluginsResponse
import org.wordpress.android.fluxc.persistence.PluginSqlUtilsWrapper
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryErrorType
import javax.inject.Inject

class PluginWPComCoroutineClient @Inject constructor(
    private val wpComNetwork: WPComNetwork,
    private val pluginsSqlUtilsWrapper: PluginSqlUtilsWrapper
) {
    suspend fun fetchSitePlugins(site: SiteModel): PluginsResponse {
        val url = WPCOMREST.sites.site(site.siteId).plugins.urlV1_2
        val request = wpComNetwork.executeGetGsonRequest(url, FetchPluginsResponse::class.java)
        return if (request is Success) {
            if (request.data.plugins != null) {
                val plugins = request.data.plugins.map { pluginModelFromResponse(site, it) }
                pluginsSqlUtilsWrapper.insertOrReplaceSitePlugins(site, plugins)
                PluginsResponse.Success
            } else {
                PluginsResponse.Error(PluginDirectoryErrorType.EMPTY_RESPONSE)
            }
        } else {
            PluginsResponse.Error(PluginDirectoryErrorType.GENERIC_ERROR)
        }
    }

    sealed interface PluginsResponse {
        object Success : PluginsResponse
        data class Error(val error: PluginDirectoryErrorType) : PluginsResponse
    }
}
