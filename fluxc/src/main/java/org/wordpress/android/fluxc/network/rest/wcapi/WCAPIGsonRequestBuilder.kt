package org.wordpress.android.fluxc.network.rest.wcapi

import com.android.volley.Request
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import javax.inject.Inject
import kotlin.coroutines.resume

class WCAPIGsonRequestBuilder @Inject constructor() {
    @Suppress("LongParameterList")
    suspend fun <T> syncGetRequest(
            restClient: BaseWCAPIRestClient,
            url: String,
            params: Map<String, String> = emptyMap(),
            body: Map<String, Any> = emptyMap(),
            clazz: Class<T>,
            enableCaching: Boolean = false,
            cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
            basicAuthKey: String? = null
    ) = suspendCancellableCoroutine<WCAPIResponse<T>> { cont ->
        callMethod(
                Request.Method.GET,
                url,
                params,
                body,
                clazz,
                cont,
                enableCaching,
                cacheTimeToLive,
                basicAuthKey,
                restClient
        )
    }

    suspend fun <T> syncPostRequest(
            restClient: BaseWCAPIRestClient,
            url: String,
            body: Map<String, Any> = emptyMap(),
            clazz: Class<T>,
            basicAuthKey: String? = null
    ) = suspendCancellableCoroutine<WCAPIResponse<T>> { cont ->
        callMethod(
                Request.Method.POST,
                url,
                null,
                body,
                clazz,
                cont,
                false,
                0,
                basicAuthKey,
                restClient
        )
    }

    suspend fun <T> syncPutRequest(
            restClient: BaseWCAPIRestClient,
            url: String,
            body: Map<String, Any> = emptyMap(),
            clazz: Class<T>,
            basicAuthKey: String? = null
    ) = suspendCancellableCoroutine<WCAPIResponse<T>> { cont ->
        callMethod(
                Request.Method.PUT,
                url,
                null,
                body,
                clazz,
                cont,
                false,
                0,
                basicAuthKey,
                restClient
        )
    }

    suspend fun <T> syncDeleteRequest(
            restClient: BaseWCAPIRestClient,
            url: String,
            params: Map<String, String>,
            clazz: Class<T>,
            basicAuthKey: String? = null
    ) = suspendCancellableCoroutine<WCAPIResponse<T>> { cont ->
        callMethod(
                Request.Method.DELETE,
                url,
                params,
                emptyMap(),
                clazz,
                cont,
                false,
                0,
                basicAuthKey,
                restClient
        )
    }

    @Suppress("LongParameterList")
    private fun <T> callMethod(
            method: Int,
            url: String,
            params: Map<String, String>?,
            body: Map<String, Any>,
            clazz: Class<T>,
            cont: CancellableContinuation<WCAPIResponse<T>>,
            enableCaching: Boolean,
            cacheTimeToLive: Int,
            authText: String?,
            restClient: BaseWCAPIRestClient
    ) {
        val request = WCAPIGsonRequest(method, url, params, body, clazz, { response ->
            cont.resume(WCAPIResponse.Success(response))
        }, { error ->
            cont.resume(WCAPIResponse.Error(error))
        })

        cont.invokeOnCancellation {
            request.cancel()
        }

        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        if(authText != null) {
            request.addHeader("Authorization", "Basic $authText")
        }

        restClient.add(request)
    }
}
