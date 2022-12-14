package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Woo app supports connecting to sites using either Jetpack or the site credentials (technically application
 * passwords). This class allows this by supporting multiple networking implementations depending on the type of site:
 * - Jetpack Sites: the API call will use Jetpack Tunnel using [JetpackTunnelWPAPINetwork]
 * - Non-Jetpack Sites: the API call will use Application Passwords using [ApplicationPasswordNetwork]
 */
@Singleton
class WooNetwork @Inject constructor(
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork,
    private val applicationPasswordNetwork: ApplicationPasswordNetwork
) {
    suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executeGetGsonRequest(site, path, clazz, params)
                .toWPAPIResponse()
            else -> applicationPasswordNetwork.executeGetGsonRequest(site, path, clazz, params)
        }
    }

    suspend fun <T : Any> executePostGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap(),
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executePostGsonRequest(site, path, clazz, body)
                .toWPAPIResponse()
            else -> applicationPasswordNetwork.executePostGsonRequest(site, path, clazz, body)
        }
    }

    suspend fun <T : Any> executePutGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        body: Map<String, Any> = emptyMap()
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executePutGsonRequest(site, path, clazz, body)
                .toWPAPIResponse()
            else -> applicationPasswordNetwork.executePutGsonRequest(site, path, clazz, body)
        }
    }

    suspend fun <T : Any> executeDeleteGsonRequest(
        site: SiteModel,
        path: String,
        clazz: Class<T>,
        params: Map<String, String> = emptyMap()
    ): WPAPIResponse<T> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> jetpackTunnelWPAPINetwork.executeDeleteGsonRequest(site, path, clazz, params)
                .toWPAPIResponse()
            else -> applicationPasswordNetwork.executeDeleteGsonRequest(site, path, clazz, params)
        }
    }
}

private fun <T> JetpackResponse<T>.toWPAPIResponse(): WPAPIResponse<T> {
    return when (this) {
        is JetpackSuccess -> WPAPIResponse.Success(data)
        is JetpackError -> WPAPIResponse.Error(WPAPINetworkError(error, errorCode = error.apiError))
    }
}
