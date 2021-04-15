package org.wordpress.android.fluxc.ditests

import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.internal.tls.OkHostnameVerifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.module.AppContextModule
import javax.inject.Inject
import javax.inject.Named
import org.junit.Before
import org.wordpress.android.fluxc.network.BaseRequest
import java.time.Duration

open class OkHttpReleaseInjectionTest {

    @Inject
    @Named("regular")
    lateinit var regularClient: OkHttpClient

    @Inject
    @Named("no-redirects")
    lateinit var noRedirectsOkHttpClientBuilder: OkHttpClient.Builder

    @Inject
    @Named("custom-ssl")
    lateinit var customSslOkHttpClient: OkHttpClient

    @Before
    open fun setUp() {
        DaggerOkHttpReleaseTestComponent.builder()
                .appContextModule(AppContextModule(InstrumentationRegistry.getInstrumentation().context))
                .build()
                .inject(this)
    }

    @Test
    fun assertRegularOkHttpClientBuilderHasExpectedSettings() {
        regularClient.apply {
            assertThat(cookieJar).isNotEqualTo(DEFAULT_COOKIE_JAR)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isEqualTo(OkHostnameVerifier)

            assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        }
    }

    @Test
    fun assertNoRedirectsOkHttpClientBuilderHasExpectedSettings() {
        noRedirectsOkHttpClientBuilder.build().apply {
            assertThat(cookieJar).isNotEqualTo(DEFAULT_COOKIE_JAR)
            assertThat(followRedirects).isFalse
            assertThat(hostnameVerifier).isEqualTo(OkHostnameVerifier)

            assertThat(connectTimeoutMillis).isEqualTo(DEFAULT_CONNECT_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(DEFAULT_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(DEFAULT_WRITE_TIMEOUT)
        }
    }

    @Test
    fun assertCustomSslOkHttpClientBuilderHasExpectedSettings() {
        customSslOkHttpClient.apply {
            assertThat(cookieJar).isNotEqualTo(DEFAULT_COOKIE_JAR)
            assertThat(followRedirects).isTrue
            assertThat(hostnameVerifier).isNotEqualTo(OkHostnameVerifier)

            assertThat(connectTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
            assertThat(readTimeoutMillis).isEqualTo(BaseRequest.UPLOAD_REQUEST_READ_TIMEOUT)
            assertThat(writeTimeoutMillis).isEqualTo(BaseRequest.DEFAULT_REQUEST_TIMEOUT)
        }
    }

    companion object {
        private val DEFAULT_COOKIE_JAR = CookieJar.NO_COOKIES

        private val DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10).toMillis()
        private val DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10).toMillis()
        private val DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(10).toMillis()
    }
}