package org.wordpress.android.fluxc.wc.leaderboards

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit.DAY
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleShippingLabelApiResponse

class LeaderboardsRestClientTest {
    private lateinit var restClientUnderTest: LeaderboardsRestClient
    private lateinit var requestBuilder: JetpackTunnelGsonRequestBuilder
    private lateinit var jetpackSuccessResponse: JetpackSuccess<Array<LeaderboardsApiResponse>>
    private lateinit var jetpackErrorResponse: JetpackError<Array<LeaderboardsApiResponse>>

    private val site = SiteModel().apply { id = 321 }

    @Before
    fun setUp() {
        requestBuilder = mock()
        jetpackSuccessResponse = mock()
        jetpackErrorResponse = mock()
        restClientUnderTest = LeaderboardsRestClient(
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                requestBuilder
        )
    }

    @Test
    fun `fetch leaderboards should call syncGetRequest with correct parameters and return expected response`() = test {
        val expectedResult = generateSampleShippingLabelApiResponse()
        configureSuccessRequest(expectedResult!!)

        val response = restClientUnderTest.fetchLeaderboards(site, DAY, 1L..22L, 5)
        verify(requestBuilder, times(1)).syncGetRequest(
                restClientUnderTest,
                site,
                WOOCOMMERCE.leaderboards.pathV4Analytics,
                mapOf(
                        "before" to "22",
                        "after" to "1",
                        "per_page" to "5",
                        "interval" to "day"
                ),
                Array<LeaderboardsApiResponse>::class.java
        )
        assertThat(response).isNotNull
        assertThat(response.result).isNotNull
        assertThat(response.error).isNull()
        assertThat(response.result).isEqualTo(expectedResult)
    }

    @Test
    fun `fetch leaderboards should correctly return failure as WooError`() = test {
        configureErrorRequest()
        val response = restClientUnderTest.fetchLeaderboards(
                site,
                DAY,
                1L..22L,
                5)

        assertThat(response).isNotNull
        assertThat(response.result).isNull()
        assertThat(response.error).isNotNull
        assertThat(response.error).isExactlyInstanceOf(WooError::class.java)
    }

    private suspend fun configureSuccessRequest(expectedResult: Array<LeaderboardsApiResponse>) {
        whenever(jetpackSuccessResponse.data).thenReturn(expectedResult)
        whenever(
                requestBuilder.syncGetRequest(
                        restClientUnderTest,
                        site,
                        WOOCOMMERCE.leaderboards.pathV4Analytics,
                        mapOf(
                                "after" to "1",
                                "before" to "22",
                                "per_page" to "5",
                                "interval" to "day"
                        ),
                        Array<LeaderboardsApiResponse>::class.java
                )
        ).thenReturn(jetpackSuccessResponse)
    }

    private suspend fun configureErrorRequest() {
        whenever(jetpackErrorResponse.error).thenReturn(WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))
        whenever(
                requestBuilder.syncGetRequest(
                        restClientUnderTest,
                        site,
                        WOOCOMMERCE.leaderboards.pathV4Analytics,
                        mapOf(
                                "after" to "1",
                                "before" to "22",
                                "per_page" to "5",
                                "interval" to "day"
                        ),
                        Array<LeaderboardsApiResponse>::class.java
                )
        ).thenReturn(jetpackErrorResponse)
    }
}
