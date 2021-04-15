package org.wordpress.android.fluxc.ditests

import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.module.OkHttpConstants
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
            assertThat(hostnameVerifier).isEqualTo(OkHttpConstants.DEFAULT_HOSTNAME_VERIFIER)

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
            assertThat(hostnameVerifier).isEqualTo(OkHttpConstants.DEFAULT_HOSTNAME_VERIFIER)

            assertThat(connectTimeoutMillis).isEqualTo(OkHttpConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS)
            assertThat(readTimeoutMillis).isEqualTo(OkHttpConstants.DEFAULT_READ_TIMEOUT_MILLIS)
            assertThat(writeTimeoutMillis).isEqualTo(OkHttpConstants.DEFAULT_WRITE_TIMEOUT_MILLIS)
        }
    }

    @Test
    fun assertCustomSslOkHttpClientBuilderHasExpectedSettings() {
        customSslOkHttpClient.apply {
            assertThat(cookieJar).isEqualTo(customCookieJar)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isNotEqualTo(OkHttpConstants.DEFAULT_HOSTNAME_VERIFIER)

            assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        }
    }
}
