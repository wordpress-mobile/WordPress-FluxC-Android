package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import com.android.volley.Response.Listener
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder.Response.Success
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object WPAPIGsonRequestBuilder {
    suspend fun <T> syncGetRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String>,
        body: Map<String, String>,
        clazz: Class<T>
    ) = suspendCoroutine<Response<T>> { cont ->
        val request = WPAPIGsonRequest(Method.GET, url, params, body, clazz, Listener {
            response -> cont.resume(Success(response))
        }, BaseErrorListener {
            error -> cont.resume(Response.Error(error))
        })

        restClient.add(request)
    }

    sealed class Response<T> {
        data class Success<T>(val data: T) : Response<T>()
        data class Error<T>(val error: BaseNetworkError) : Response<T>()
    }
}
