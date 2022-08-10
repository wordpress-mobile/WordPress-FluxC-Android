package org.wordpress.android.fluxc.network.rest.wpcom.wc

import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient.GatewayId.COD

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
    fun `given success response, when post gateway, return success`() {
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
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(
                    GatewayRestClient.GatewayResponse(
                        gatewayId = COD.toString(),
                        title = "",
                        description = "",
                        order = "",
                        enabled = false,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )

            val actualResponse = gatewayRestClient.postPaymentGateway(
                SiteModel(),
                COD,
                enabled = false
            )

            Assertions.assertThat(actualResponse).isEqualTo(
                WooPayload(
                    result = GatewayRestClient.GatewayResponse(
                        gatewayId = COD.toString(),
                        title = "",
                        description = "",
                        order = "",
                        enabled = false,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )
        }
    }

    @Test
    fun `given error response, when post gateway, return error`() {
        runBlocking {
            whenever(
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    any(),
                    any(),
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(
                    WPComGsonRequest.WPComGsonNetworkError(
                        BaseRequest.BaseNetworkError(
                            BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                        )
                    )
                )
            )

            val actualResponse = gatewayRestClient.postPaymentGateway(
                SiteModel(),
                COD,
                enabled = true
            )

            Assertions.assertThat(actualResponse).isEqualTo(
                WooPayload<WooError>(
                    error = WooError(
                        type = WooErrorType.AUTHORIZATION_REQUIRED,
                        original = BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                    ),
                )
            )
        }
    }

    @Test
    fun `given success response, when post COD title, return success`() {
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
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(
                    GatewayRestClient.GatewayResponse(
                        gatewayId = "",
                        title = "Cash on Delivery",
                        description = "",
                        order = "",
                        enabled = false,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )
            val actualResponse = gatewayRestClient.postCashOnDeliveryTitle(
                site = SiteModel(),
                title = "Cash on Delivery"
            )

            Assertions.assertThat(actualResponse).isEqualTo(
                WooPayload(
                    result = GatewayRestClient.GatewayResponse(
                        gatewayId = "",
                        title = "Cash on Delivery",
                        description = "",
                        order = "",
                        enabled = false,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )
        }
    }

    @Test
    fun `given error response, when post COD title, return error`() {
        runBlocking {
            whenever(
                jetpackTunnelGsonRequestBuilder.syncPostRequest(
                    any(),
                    any(),
                    any(),
                    any(),
                    any<Class<GatewayRestClient.GatewayResponse>>(),
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(
                    WPComGsonRequest.WPComGsonNetworkError(
                        BaseRequest.BaseNetworkError(
                            BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                        )
                    )
                )
            )

            val actualResponse = gatewayRestClient.postCashOnDeliveryTitle(
                SiteModel(),
                title = "Cash on Delivery"
            )

            Assertions.assertThat(actualResponse).isEqualTo(
                WooPayload<WooError>(
                    error = WooError(
                        type = WooErrorType.AUTHORIZATION_REQUIRED,
                        original = BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                    ),
                )
            )
        }
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
                    any()
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess(
                    GatewayRestClient.GatewayResponse(
                        gatewayId = "",
                        title = "",
                        description = "",
                        order = "",
                        enabled = false,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )

            val actualResponse = gatewayRestClient.fetchGateway(SiteModel(), "")

            Assertions.assertThat(actualResponse).isEqualTo(
                WooPayload(
                    result = GatewayRestClient.GatewayResponse(
                        gatewayId = "",
                        title = "",
                        description = "",
                        order = "",
                        enabled = false,
                        methodTitle = "",
                        methodDescription = "",
                        features = emptyList()
                    )
                )
            )
        }
    }

    @Test
    fun `given error response, when fetch gateway, return error`() {
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
                    any()
                )
            ).thenReturn(
                JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError(
                    WPComGsonRequest.WPComGsonNetworkError(
                        BaseRequest.BaseNetworkError(
                            BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                        )
                    )
                )
            )

            val actualResponse = gatewayRestClient.fetchGateway(SiteModel(), "")

            Assertions.assertThat(actualResponse).isEqualTo(
                WooPayload<WooError>(
                    error = WooError(
                        type = WooErrorType.AUTHORIZATION_REQUIRED,
                        original = BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                    ),
                )
            )
        }
    }
}
