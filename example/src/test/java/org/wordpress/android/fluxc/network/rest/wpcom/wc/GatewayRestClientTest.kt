package org.wordpress.android.fluxc.network.rest.wpcom.wc

import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayId.CASH_ON_DELIVERY

class GatewayRestClientTest {
    private lateinit var jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
    private lateinit var requestQueue: RequestQueue
    private lateinit var accessToken: AccessToken
    private lateinit var userAgent: UserAgent
    private lateinit var gatewayRestClient: GatewayRestClient

    @Before
    fun setup() {
        jetpackTunnelGsonRequestBuilder = mock()
        requestQueue = mock()
        accessToken = mock()
        userAgent = mock()
        gatewayRestClient = GatewayRestClient(
            mock(),
            jetpackTunnelGsonRequestBuilder,
            mock(),
            requestQueue,
            accessToken,
            userAgent
        )
    }

    @Test
    fun `given success response, when fetch gateway, return success`() {
        runBlocking {
            whenever(
                jetpackTunnelGsonRequestBuilder.syncGetRequest(
                    any(),
                    any(),
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                    any(),
                    any(),
                    any(),
                    anyOrNull()
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(mock())
            )

            val actualResponse = gatewayRestClient.fetchGateway(
                SiteModel(),
                ""
            )

            Assertions.assertThat(actualResponse.isError).isFalse
            Assertions.assertThat(actualResponse.result).isNotNull
        }
    }

    @Test
    fun `given error response, when fetch gateway, return error`() {
        runBlocking {
            val expectedError = mock<WPComGsonNetworkError>().apply {
                type = mock()
            }
            whenever(
                jetpackTunnelGsonRequestBuilder.syncGetRequest(
                    any(),
                    any(),
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                    any(),
                    any(),
                    any(),
                    anyOrNull()
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(expectedError)
            )

            val actualResponse = gatewayRestClient.fetchGateway(SiteModel(), "")

            Assertions.assertThat(actualResponse.isError).isTrue
            Assertions.assertThat(actualResponse.error).isNotNull
        }
    }

    @Test
    fun `given success response, when update gateway, return success`() {
        runBlocking {
            whenever(
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    any(),
                    any(),
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>()
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(mock())
            )

            val actualResponse = gatewayRestClient.updatePaymentGateway(
                SiteModel(),
                CASH_ON_DELIVERY,
                enabled = false,
                title = "title"
            )

            Assertions.assertThat(actualResponse.isError).isFalse
            Assertions.assertThat(actualResponse.result).isNotNull
        }
    }

    @Test
    fun `given error response, when update gateway, return error`() {
        runBlocking {
            val expectedError = mock<WPComGsonNetworkError>().apply {
                type = mock()
            }
            whenever(
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    any(),
                    any(),
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(expectedError)
            )

            val actualResponse = gatewayRestClient.updatePaymentGateway(
                SiteModel(),
                CASH_ON_DELIVERY,
                enabled = true,
                title = "title"
            )

            Assertions.assertThat(actualResponse.isError).isTrue
            Assertions.assertThat(actualResponse.error).isNotNull
        }
    }
}
