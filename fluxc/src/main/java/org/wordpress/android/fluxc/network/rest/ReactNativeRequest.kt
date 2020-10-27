package org.wordpress.android.fluxc.network.rest

import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object ReactNativeRequest {
    object ResponseKey {
        const val body = "body"
        const val headers = "headers"
        const val networkTimeMs = "networkTimeMs"
        const val notModified = "notModified"
        const val statusCode = "statusCode"
    }

    fun parseNetworkResponse(
        networkResponse: NetworkResponse?,
        response: Response<JsonElement>
    ): Response<JsonElement> =
            // If response has a result, update the response to have the result under the "body" key and include
            // additional information regarding the network response under relevant keys
            if (response.result == null || networkResponse == null) {
                response
            } else {
                val json = JsonObject().apply {
                    add(ResponseKey.body, response.result)
                    add(ResponseKey.headers, parseHeaders(networkResponse.headers))
                    addProperty(ResponseKey.networkTimeMs, networkResponse.networkTimeMs)
                    addProperty(ResponseKey.notModified, networkResponse.notModified)
                    addProperty(ResponseKey.statusCode, networkResponse.statusCode)
                }
                Response.success(json, response.cacheEntry)
            }

    private fun parseHeaders(headers: Map<String, String>): JsonObject =
            Gson().toJsonTree(headers).asJsonObject
}
