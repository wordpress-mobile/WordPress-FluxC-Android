package org.wordpress.android.fluxc.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import kotlin.coroutines.resume

/**
 * Extends JetpackTunnelGsonRequestBuilder to make available a new JSON-formatted PUT and DELETE requests,
 * triggers it and awaits results synchronously.
 * @param restClient rest client that handles the request
 * @param url the request URL
 * @param body the content body, which will be converted to JSON using [Gson][com.google.gson.Gson]
 * @param clazz the class defining the expected response
 */

suspend fun <T : Any> JetpackTunnelGsonRequestBuilder.syncPutRequest(
    restClient: BaseWPComRestClient,
    site: SiteModel,
    url: String,
    body: Map<String, Any>,
    clazz: Class<T>
) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
    val request = JetpackTunnelGsonRequest.buildPutRequest<T>(url, site.siteId, body, clazz, {
        cont.resume(JetpackSuccess(it))
    }, {
        cont.resume(JetpackError(it))
    })
    cont.invokeOnCancellation {
        request?.cancel()
    }
    restClient.add(request)
}

suspend fun <T : Any> JetpackTunnelGsonRequestBuilder.syncDeleteRequest(
    restClient: BaseWPComRestClient,
    site: SiteModel,
    url: String,
    clazz: Class<T>
) = suspendCancellableCoroutine<JetpackResponse<T>> { cont ->
    val request = JetpackTunnelGsonRequest.buildDeleteRequest<T>(url, site.siteId, mapOf(), clazz, {
        cont.resume(JetpackSuccess(it))
    }, {
        cont.resume(JetpackError(it))
    })
    cont.invokeOnCancellation {
        request?.cancel()
    }
    restClient.add(request)
}

fun <T> JetpackResponse<T>.handleResult() =
        when (this) {
            is JetpackSuccess -> {
                WooPayload(data)
            }
            is JetpackError -> {
                WooPayload(error.toWooError())
            }
        }
