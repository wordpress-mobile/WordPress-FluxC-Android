package org.wordpress.android.fluxc.network.rest.wpapi.reactnative

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.rest.ReactNativeRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest

class ReactNativeWPAPIGsonRequest(
    method: Int,
    url: String,
    body: Map<String, Any> = emptyMap(),
    params: Map<String, String> = emptyMap(),
    clazz: Class<JsonElement>,
    responseListener: Response.Listener<JsonElement>,
    errorListener: BaseErrorListener
) : WPAPIGsonRequest<JsonElement>(method, url, params, body, clazz, responseListener, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<JsonElement> =
        ReactNativeRequest.parseNetworkResponse(response, super.parseNetworkResponse(response))
}
