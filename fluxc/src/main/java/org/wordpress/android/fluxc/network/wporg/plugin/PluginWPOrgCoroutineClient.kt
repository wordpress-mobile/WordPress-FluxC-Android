package org.wordpress.android.fluxc.network.wporg.plugin

import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.wporg.BaseWPOrgAPIClient
import org.wordpress.android.fluxc.network.wporg.WPOrgAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.wporg.WPOrgAPIResponse
import org.wordpress.android.fluxc.store.PluginStore.FetchWPOrgPluginErrorType
import org.wordpress.android.fluxc.store.PluginStore.FetchWPOrgPluginErrorType.EMPTY_RESPONSE
import org.wordpress.android.fluxc.store.PluginStore.FetchWPOrgPluginErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.PluginStore.FetchWPOrgPluginErrorType.PLUGIN_DOES_NOT_EXIST
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PluginWPOrgCoroutineClient @Inject constructor(
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    userAgent: UserAgent,
) : BaseWPOrgAPIClient(dispatcher, requestQueue, userAgent) {
    suspend fun fetchWpOrgPlugin(slug: String): PluginResponse {
        val url = WPORGAPI.plugins.info.version("1.0").slug(slug).url

        val response = WPOrgAPIGsonRequestBuilder.syncGetRequest(
            restClient = this,
            url = url,
            clazz = WPOrgPluginResponse::class.java
        )

        return when (response) {
            is WPOrgAPIResponse.Success -> {
                if (response.data == null) {
                    PluginResponse.Error(EMPTY_RESPONSE)
                } else if (!response.data.errorMessage.isNullOrEmpty()) {
                    PluginResponse.Error(PLUGIN_DOES_NOT_EXIST)
                } else {
                    PluginResponse.Success(response.data.toWPOrgPluginModel())
                }
            }
            is WPOrgAPIResponse.Error -> {
                PluginResponse.Error(GENERIC_ERROR)
            }
        }
    }

    sealed interface PluginResponse {
        data class Success(val plugin: WPOrgPluginModel) : PluginResponse
        data class Error(val error: FetchWPOrgPluginErrorType) : PluginResponse
    }
}
