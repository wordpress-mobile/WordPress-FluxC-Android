package org.wordpress.android.fluxc.network

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.VolleyError
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.BasicNetwork

/**
 * Enhances [BasicNetwork] by adding retries on 301 and 302 redirects according to the applied retry policy
 */
class RetryOnRedirectKeepingAuthHeadersBasicNetwork(httpStack: BaseHttpStack?) :
    BasicNetwork(httpStack) {
    @Throws(VolleyError::class)
    override fun performRequest(request: Request<*>?): NetworkResponse {
        return try {
            super.performRequest(request)
        } catch (error: ServerError) {
            if (request != null && (error.networkResponse.statusCode == HTTP_REDIRECT_PERMANENTLY
                        || error.networkResponse.statusCode == HTTP_REDIRECT_FOUND)) {
                val policy = request.retryPolicy
                policy.retry(error) // If no attempts are left an error is thrown
                error.networkResponse.headers?.get("location")?.let {
                    //val redirectRequest =
                    performRequest(request)
                }
            }
            throw error
        }
    }

    companion object {
        private const val HTTP_REDIRECT_PERMANENTLY = 301
        private const val HTTP_REDIRECT_FOUND = 302
    }
}
