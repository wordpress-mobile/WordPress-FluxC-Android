package org.wordpress.android.fluxc.network.rest.wpcom.jitm

import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JitmRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMContent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMCta

class JitmRestClientTest {
    private lateinit var jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
    private lateinit var requestQueue: RequestQueue
    private lateinit var accessToken: AccessToken
    private lateinit var userAgent: UserAgent
    private lateinit var jitmRestClient: JitmRestClient

    @Before
    fun setup() {
        jetpackTunnelGsonRequestBuilder = mock()
        requestQueue = mock()
        accessToken = mock()
        userAgent = mock()
        jitmRestClient = JitmRestClient(
            mock(),
            jetpackTunnelGsonRequestBuilder,
            mock(),
            requestQueue,
            accessToken,
            userAgent
        )
    }

    private fun provideJitmApiResponse(
        content: JITMContent = provideJitmContent(),
        jitmCta: JITMCta = provideJitmCta(),
        timeToLive: Int = 0,
        id: String = "",
        featureClass: String = "",
        expires: Long = 0L,
        maxDismissal: Int = 2,
        isDismissible: Boolean = false,
        url: String = "",
        jitmStatsUrl: String = ""
    ) = JITMApiResponse(
        content = content,
        cta = jitmCta,
        timeToLive = timeToLive,
        id = id,
        featureClass = featureClass,
        expires = expires,
        maxDismissal = maxDismissal,
        isDismissible = isDismissible,
        url = url,
        jitmStatsUrl = jitmStatsUrl
    )

    private fun provideJitmContent(
        message: String = "",
        description: String = "",
        icon: String = "",
        title: String = ""
    ) = JITMContent(
        message = message,
        description = description,
        icon = icon,
        title = title
    )

    private fun provideJitmCta(
        message: String = "",
        link: String = ""
    ) = JITMCta(
        message = message,
        link = link
    )

    @Test
    fun `given success response, when fetch jitm, return success`() {
        runBlocking {
            val site = SiteModel().apply { siteId = 1234 }
            whenever(
                jetpackTunnelGsonRequestBuilder.syncGetRequest(
                    jitmRestClient,
                    site,
                    WPCOMREST.jetpack_blogs.site(1234).rest_api.jitmPath,
                    mapOf(
                        "message_path" to ""
                    ),
                    Array<JITMApiResponse>::class.java,
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(
                    arrayOf(provideJitmApiResponse())
                )
            )

            val actualResponse = jitmRestClient.fetchJitmMessage(
                site,
                ""
            )

            assertThat(actualResponse.isError).isFalse
            assertThat(actualResponse.result).isNotNull
        }
    }

    @Test
    fun `given error response, when fetch jitm, return error`() {
        val site = SiteModel().apply { siteId = 1234 }
        runBlocking {
            val expectedError = mock<WPComGsonRequest.WPComGsonNetworkError>().apply {
                type = mock()
            }
            whenever(
                jetpackTunnelGsonRequestBuilder.syncGetRequest(
                    jitmRestClient,
                    site,
                    WPCOMREST.jetpack_blogs.site(1234).rest_api.jitmPath,
                    mapOf(
                        "message_path" to ""
                    ),
                    Array<JITMApiResponse>::class.java,
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(expectedError)
            )

            val actualResponse = jitmRestClient.fetchJitmMessage(site, "")

            assertThat(actualResponse.isError).isTrue
            assertThat(actualResponse.error).isNotNull
        }
    }

    @Test
    fun `given success response, when dismiss jitm, return success`() {
        runBlocking {
            val site = SiteModel().apply { siteId = 1234 }
            whenever(
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    jitmRestClient,
                    site,
                    WPCOMREST.jetpack_blogs.site(1234).rest_api.jitmPath,
                    mapOf(
                        "id" to "",
                        "feature_class" to ""
                    ),
                    Any::class.java,
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(
                    arrayOf(provideJitmApiResponse())
                )
            )

            val actualResponse = jitmRestClient.dismissJitmMessage(
                site,
                "",
                ""
            )

            Assertions.assertThat(actualResponse.isError).isFalse
            Assertions.assertThat(actualResponse.result).isTrue
        }
    }

    @Test
    fun `given error response, when dismiss jitm, return error`() {
        runBlocking {
            val site = SiteModel().apply { siteId = 1234 }
            val expectedError = mock<WPComGsonRequest.WPComGsonNetworkError>().apply {
                type = mock()
            }
            whenever(
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    jitmRestClient,
                    site,
                    WPCOMREST.jetpack_blogs.site(1234).rest_api.jitmPath,
                    mapOf(
                        "id" to "",
                        "feature_class" to ""
                    ),
                    Any::class.java,
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(expectedError)
            )

            val actualResponse = jitmRestClient.dismissJitmMessage(
                site,
                "",
                ""
            )

            Assertions.assertThat(actualResponse.isError).isTrue
            Assertions.assertThat(actualResponse.result).isNull()
        }
    }
}
