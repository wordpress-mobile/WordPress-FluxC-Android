package org.wordpress.android.fluxc.module

import com.google.gson.JsonElement
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.Okio
import org.wordpress.android.fluxc.TestUtils
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import javax.inject.Singleton

@Singleton
class ResponseMockingInterceptor : Interceptor {
    private var nextResponseJson: String? = null
    private var nextResponseErrorCode: Int = 0

    fun respondWith(jsonResponseFileName: String) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseErrorCode = 0
    }

    @JvmOverloads
    fun respondWithError(jsonResponseFileName: String, errorCode: Int = 404) {
        nextResponseJson = getStringFromResourceFile(jsonResponseFileName)
        nextResponseErrorCode = errorCode
    }

    fun respondWith(jsonResponse: JsonElement) {
        nextResponseJson = jsonResponse.toString()
        nextResponseErrorCode = 0
    }

    @JvmOverloads
    fun respondWithError(jsonResponse: JsonElement, errorCode: Int = 404) {
        nextResponseJson = jsonResponse.toString()
        nextResponseErrorCode = errorCode
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Give some time to create a realistic network event
        TestUtils.waitFor(1000)

        val requestUrl = request.url().toString()

        nextResponseJson?.let {
            val response = if (nextResponseErrorCode == 0) {
                buildSuccessResponse(request, it)
            } else {
                buildErrorResponse(request, it, nextResponseErrorCode)
            }

            // Clean up for the next call
            nextResponseJson = null
            nextResponseErrorCode = 0
            return response
        }

        throw IllegalStateException("Interceptor was not given a response for this request! URL: $requestUrl")
    }

    private fun buildSuccessResponse(request: Request, responseJson: String) =
            buildResponse(request, responseJson, 200)

    private fun buildErrorResponse(request: Request, responseJson: String, errorCode: Int) =
            buildResponse(request, responseJson, errorCode)

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
