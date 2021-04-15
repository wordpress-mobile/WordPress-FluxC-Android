package org.wordpress.android.fluxc.ditests

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest
import javax.inject.Inject
import javax.inject.Named

class OkHttpReleaseInjectionTest {
    @Inject @Named("regular")
    lateinit var regularClient: OkHttpClient

    @Inject @Named("no-redirects")
    lateinit var noRedirectsOkHttpClient: OkHttpClient

    @Inject @Named("custom-ssl")
    lateinit var customSslOkHttpClient: OkHttpClient

    @Inject lateinit var customCookieJar: CookieJar

    @Before
    fun setUp() {
        DaggerOkHttpReleaseTestComponent.builder()
                .build()
                .inject(this)
    }

    @Test
    fun assertRegularOkHttpClientBuilderHasExpectedSettings() {
        regularClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isEqualTo(DEFAULT_HOSTNAME_VERIFIER)

            assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        }
    }

    @Test
    fun assertNoRedirectsOkHttpClientBuilderHasExpectedSettings() {
        noRedirectsOkHttpClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isFalse
            assertThat(hostnameVerifier).isEqualTo(DEFAULT_HOSTNAME_VERIFIER)

            assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        }
    }

    @Test
    fun assertCustomSslOkHttpClientBuilderHasExpectedSettings() {
        customSslOkHttpClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isNotEqualTo(DEFAULT_HOSTNAME_VERIFIER)

            assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        }
    }

    companion object {
        val DEFAULT_HOSTNAME_VERIFIER = OkHostnameVerifier
    }
}
