package org.wordpress.android.fluxc.module

import com.google.gson.Gson
import com.google.gson.JsonElement
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor.InterceptorMode.ONE_TIME
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor.InterceptorMode.STICKY
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor.InterceptorMode.STICKY_SUBSTITUTION
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Singleton

@Singleton
class ResponseMockingInterceptor : Interceptor {
    companion object {
        private val SUBSTITUTION_DEFAULT = { string: String -> string }
        const val NETWORK_DELAY_MS = 500L
    }

    /**
     * ONE_TIME: Use the last response provided for the next request handled, and then clear it.
     * STICKY: Keep using the last response provided for all requests, until a new response is provided.
     * STICKY_SUBSTITUTION: Like STICKY, but also run a transformation function on the response.
     */
    enum class InterceptorMode { ONE_TIME, STICKY, STICKY_SUBSTITUTION }

    private var nextResponseJson: String? = null
    private var nextResponseCode: Int = 200
    private var nextResponseDelay: Long = NETWORK_DELAY_MS

    private var mode: InterceptorMode = ONE_TIME
    private var transformStickyResponse = SUBSTITUTION_DEFAULT

    private val gson by lazy { Gson() }

    var lastRequest: Request? = null
        private set

    var lastRequestUrl: String = ""
        private set

    val lastRequestBody: HashMap<String, Any>
        get() {
            val buffer = Buffer().apply {
                lastRequest?.body?.writeTo(this)
            }

            return gson.fromJson<HashMap<String, Any>>(buffer.readUtf8(), HashMap::class.java) ?: hashMapOf()
        }

    fun respondWith(jsonResponseFileName: String) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseCode = 200
        nextResponseDelay = NETWORK_DELAY_MS
        mode = ONE_TIME
        transformStickyResponse = SUBSTITUTION_DEFAULT
    }

    fun respondWithSticky(
        jsonResponseFileName: String,
        responseDelay: Long = NETWORK_DELAY_MS,
        transformResponse: ((String) -> String)? = null
    ) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseCode = 200
        nextResponseDelay = responseDelay
        transformResponse?.let {
            transformStickyResponse = it
        }
        mode = if (transformResponse == null) STICKY else STICKY_SUBSTITUTION
    }

    @JvmOverloads
    fun respondWithError(jsonResponseFileName: String, errorCode: Int = 404) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseCode = errorCode
        nextResponseDelay = NETWORK_DELAY_MS
    }

    fun respondWith(jsonResponse: JsonElement) {
        nextResponseJson = jsonResponse.toString()
        nextResponseCode = 200
        nextResponseDelay = NETWORK_DELAY_MS
    }

    @JvmOverloads
    fun respondWithError(jsonResponse: JsonElement, errorCode: Int = 404) {
        nextResponseJson = jsonResponse.toString()
        nextResponseCode = errorCode
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Give some time to create a realistic network event
        TestUtils.waitFor(NETWORK_DELAY_MS)

        lastRequest = request
        lastRequestUrl = request.url.toString()

        nextResponseJson?.let {
            // Will be a successful response if nextResponseCode is 200, otherwise an error response
            val response = if (mode == STICKY_SUBSTITUTION) {
                buildResponse(request, transformStickyResponse(it), nextResponseCode)
            } else {
                buildResponse(request, it, nextResponseCode)
            }

            if (mode == ONE_TIME) {
                // Clean up for the next call
                nextResponseJson = null
                nextResponseCode = 200
            }
            return response
        }

        throw IllegalStateException("Interceptor was not given a response for this request! URL: $lastRequestUrl")
    }

    private fun buildResponse(request: Request, responseJson: String, responseCode: Int): Response {
        return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("")
                .body(object : ResponseBody() {
                    override fun contentType(): MediaType? {
                        return null
                    }

                    override fun contentLength(): Long {
                        return -1
                    }

                    override fun source(): BufferedSource {
                        val stream = ByteArrayInputStream(responseJson.toByteArray(charset("UTF-8")))
                        return stream.source().buffer()
                    }
                })
                .code(responseCode)
                .build()
    }

    private fun getStringFromResourceFile(filename: String): String {
        try {
            val inputStream = this.javaClass.classLoader.getResourceAsStream(filename)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            val buffer = StringBuilder()
            bufferedReader.forEachLine { buffer.append(it) }

            bufferedReader.close()
            return buffer.toString()
        } catch (e: IOException) {
            throw IllegalStateException("Could not load response JSON file: $filename")
        }
    }
}
