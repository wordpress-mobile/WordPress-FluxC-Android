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
import okio.Okio
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor.InterceptorMode.ONE_TIME
import org.wordpress.android.fluxc.module.ResponseMockingInterceptor.InterceptorMode.STICKY
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import javax.inject.Singleton

@Singleton
class ResponseMockingInterceptor : Interceptor {
    /**
     * ONE_TIME: Use the last response provided for the next request handled, and then clear it.
     * STICKY: Keep using the last response provided for all requests, until a new response is provided.
     */
    enum class InterceptorMode { ONE_TIME, STICKY }

    private var nextResponseJson: String? = null
    private var nextResponseCode: Int = 200

    private var mode: InterceptorMode = ONE_TIME

    private val gson by lazy { Gson() }

    var lastRequest: Request? = null
        private set

    var lastRequestUrl: String = ""
        private set

    val lastRequestBody: HashMap<String, Any>
        get() {
            val buffer = Buffer().apply {
                lastRequest?.body()?.writeTo(this)
            }

            return gson.fromJson<HashMap<String, Any>>(buffer.readUtf8(), HashMap::class.java) ?: hashMapOf()
        }

    fun respondWith(jsonResponseFileName: String) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseCode = 200
        mode = ONE_TIME
    }

    fun respondWithSticky(jsonResponseFileName: String) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseCode = 200
        mode = STICKY
    }

    @JvmOverloads
    fun respondWithError(jsonResponseFileName: String, errorCode: Int = 404) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseCode = errorCode
    }

    fun respondWith(jsonResponse: JsonElement) {
        nextResponseJson = jsonResponse.toString()
        nextResponseCode = 200
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
        TestUtils.waitFor(1000)

        lastRequest = request
        lastRequestUrl = request.url().toString()

        nextResponseJson?.let {
            // Will be a successful response if nextResponseCode is 200, otherwise an error response
            val response = buildResponse(request, it, nextResponseCode)

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

                    override fun source(): BufferedSource? {
                        return try {
                            val stream = ByteArrayInputStream(responseJson.toByteArray(charset("UTF-8")))
                            Okio.buffer(Okio.source(stream))
                        } catch (e: UnsupportedEncodingException) {
                            null
                        }
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
