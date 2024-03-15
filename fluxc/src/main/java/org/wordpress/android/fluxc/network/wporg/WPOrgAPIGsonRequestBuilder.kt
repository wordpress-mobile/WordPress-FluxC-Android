package org.wordpress.android.fluxc.network.wporg

import com.android.volley.Request.Method
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.wporg.WPOrgAPIResponse.Success
import org.wordpress.android.fluxc.network.wporg.WPOrgAPIResponse.Error
import kotlin.coroutines.resume

object WPOrgAPIGsonRequestBuilder {
    suspend fun <T> syncGetRequest(
        restClient: BaseWPOrgAPIClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
    ) = suspendCancellableCoroutine<WPOrgAPIResponse<T?>> { cont ->
        callMethod(Method.GET, url, params, body, clazz, cont, enableCaching, cacheTimeToLive, restClient)
    }
    suspend fun <T> syncPostRequest(
        restClient: BaseWPOrgAPIClient,
        url: String,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
    ) = suspendCancellableCoroutine<WPOrgAPIResponse<T?>> { cont ->
        callMethod(Method.POST, url, null, body, clazz, cont, false, 0, restClient)
    }

    suspend fun <T> syncPutRequest(
        restClient: BaseWPOrgAPIClient,
        url: String,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
    ) = suspendCancellableCoroutine<WPOrgAPIResponse<T?>> { cont ->
        callMethod(Method.PUT, url, null, body, clazz, cont, false, 0, restClient)
    }

    suspend fun <T> syncDeleteRequest(
        restClient: BaseWPOrgAPIClient,
        url: String,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
    ) = suspendCancellableCoroutine<WPOrgAPIResponse<T?>> { cont ->
        callMethod(Method.DELETE, url, null, body, clazz, cont, false, 0, restClient)
    }

    @Suppress("LongParameterList")
    private fun <T> callMethod(
        method: Int,
        url: String,
        params: Map<String, String>? = null,
        body: Map<String, Any> = emptyMap(),
        clazz: Class<T>,
        cont: CancellableContinuation<WPOrgAPIResponse<T?>>,
        enableCaching: Boolean,
        cacheTimeToLive: Int,
        restClient: BaseWPOrgAPIClient
    ) {
        val request = WPOrgAPIGsonRequest(method, url, params, body, clazz, { response ->
            cont.resume(Success(response))
        }, { error ->
            cont.resume(Error(error))
        })

        cont.invokeOnCancellation {
            request.cancel()
        }

        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        restClient.add(request)
    }
}
