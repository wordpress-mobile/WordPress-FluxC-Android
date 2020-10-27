package org.wordpress.android.fluxc.network.rest.wpcom.reactnative

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.rest.ReactNativeRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest

class ReactNativeWPComGsonRequest(
    method: Int,
    url: String,
    params: Map<String, String>,
    body: Map<String, Any>,
    listener: (JsonElement) -> Unit,
    errorListener: (WPComGsonNetworkError) -> Unit
) : WPComGsonRequest<JsonElement>(
        method,
        url,
        params,
        body,
        JsonElement::class.java,
        null,
        listener,
        wrapInBaseListener(errorListener)
) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<JsonElement> =
            ReactNativeRequest.parseNetworkResponse(response, super.parseNetworkResponse(response))
}
