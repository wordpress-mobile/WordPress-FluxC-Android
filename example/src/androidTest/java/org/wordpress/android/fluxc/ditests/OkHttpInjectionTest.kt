package org.wordpress.android.fluxc.ditests

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest
import javax.inject.Inject
import javax.inject.Named

class OkHttpInjectionTest {
    @Inject @Named("regular")
    lateinit var regularClient: OkHttpClient

    @Inject @Named("no-redirects")
    lateinit var noRedirectsOkHttpClient: OkHttpClient

    @Inject @Named("custom-ssl")
    lateinit var customSslOkHttpClient: OkHttpClient

    @Inject lateinit var customCookieJar: CookieJar

    @Before
    fun setUp() {
        DaggerOkHttpTestComponent.builder()
                .build()
                .inject(this)
    }

    @Test
    fun assertRegularOkHttpClientBuilderHasExpectedSettings() {
        regularClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isEqualTo(DEFAULT_HOSTNAME_VERIFIER)

            assertDefaultRequestTimeouts()
            assertInterceptorsPresence()
        }
    }

    @Test
    @Ignore("Disabling as a part of effort to exclude flaky or failing tests. pdcxQM-1St-p2")
    fun assertNoRedirectsOkHttpClientBuilderHasExpectedSettings() {
        noRedirectsOkHttpClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isFalse
            assertThat(hostnameVerifier).isEqualTo(DEFAULT_HOSTNAME_VERIFIER)

            assertDefaultRequestTimeouts()
            assertInterceptorsPresence()
        }
    }

    @Test
    fun assertCustomSslOkHttpClientBuilderHasExpectedSettings() {
        customSslOkHttpClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isNotEqualTo(DEFAULT_HOSTNAME_VERIFIER)

            assertDefaultRequestTimeouts()
            assertInterceptorsPresence()
        }
    }

    private fun OkHttpClient.assertDefaultRequestTimeouts() {
        assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
        assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
    }

    private fun OkHttpClient.assertInterceptorsPresence() {
        assertThat(interceptors).containsOnly(DummyInterceptor)
        assertThat(networkInterceptors).containsOnly(DummyNetworkInterceptor)
    }

    companion object {
        val DEFAULT_HOSTNAME_VERIFIER = OkHostnameVerifier
    }
}
