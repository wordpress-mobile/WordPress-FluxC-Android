package org.wordpress.android.fluxc.release

import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.ServerError
import com.android.volley.toolbox.StringRequest
import okhttp3.OkHttpClient
import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.network.OkHttpStack
import org.wordpress.android.fluxc.network.RetryOnRedirectBasicNetwork
import org.wordpress.android.fluxc.network.RetryOnRedirectBasicNetwork.HTTP_TEMPORARY_REDIRECT
import javax.inject.Inject
import javax.inject.Named

private const val URL = "http://do.wpmt.co/307-redirect-test/timer.php"
private const val RETRIES = 2
private const val TIMEOUT = 100
private const val BACKOFF_MULTIPLIER = 1f

class ReleaseStack_NoRedirectsTest : ReleaseStack_Base() {
    @Inject @Named("no-redirects") lateinit var okHttpClientBuilder: OkHttpClient.Builder

    @Throws(Exception::class) override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
    }

    @Test
    fun testRetriesOccurOnRedirect() {
        val network = RetryOnRedirectBasicNetwork(OkHttpStack(okHttpClientBuilder))
        val request = StringRequest(Request.Method.GET, URL, null, null)
        request.retryPolicy = DefaultRetryPolicy(TIMEOUT, RETRIES, BACKOFF_MULTIPLIER)
        val response = try {
            network.performRequest(request)
        } catch (error: ServerError) {
            Assert.assertEquals(HTTP_TEMPORARY_REDIRECT, error.networkResponse.statusCode)
            null
        }
        // [DefaultRetryPolicy] increments the retry count before throwing an error
        Assert.assertEquals(RETRIES + 1, request.retryPolicy.currentRetryCount)
        Assert.assertNull(response)
    }
}
