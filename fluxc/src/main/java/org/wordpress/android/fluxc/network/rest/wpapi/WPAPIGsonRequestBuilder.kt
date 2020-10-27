package org.wordpress.android.fluxc.network.rest.wpapi

import com.android.volley.Request.Method
import com.google.gson.JsonElement
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Error
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse.Success
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.ReactNativeWPAPIGsonRequest
import javax.inject.Inject
import kotlin.coroutines.resume

class WPAPIGsonRequestBuilder @Inject constructor() {
    suspend fun syncReactNativeGetRequest(
        restClient: BaseWPAPIRestClient,
        url: String,
        params: Map<String, String>,
        body: Map<String, String>,
        enableCaching: Boolean = false,
        cacheTimeToLive: Int = BaseRequest.DEFAULT_CACHE_LIFETIME,
        nonce: String? = null
    ) = suspendCancellableCoroutine { cont: CancellableContinuation<WPAPIResponse<JsonElement>> ->
        val request = ReactNativeWPAPIGsonRequest(
                Method.GET,
                url,
                params,
                body,
                JsonElement::class.java,
                { response -> cont.resume(Success(response)) },
                { error -> cont.resume(Error(error)) }
        )

        cont.invokeOnCancellation {
            request.cancel()
        }

        if (enableCaching) {
            request.enableCaching(cacheTimeToLive)
        }

        if (nonce != null) {
            request.addHeader("x-wp-nonce", nonce)
        }

        restClient.add(request)
    }
}
