package org.wordpress.android.fluxc.wc.leaderboards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.generateSampleLeaderboardsApiResponse
import org.wordpress.android.fluxc.wc.leaderboards.WCLeaderboardsTestFixtures.stubSite

class LeaderboardsRestClientTest {
    private lateinit var restClientUnderTest: LeaderboardsRestClient
    private lateinit var requestBuilder: JetpackTunnelGsonRequestBuilder
    private lateinit var jetpackSuccessResponse: JetpackSuccess<Array<LeaderboardsApiResponse>>
    private lateinit var jetpackErrorResponse: JetpackError<Array<LeaderboardsApiResponse>>

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
        val expectedResult = generateSampleLeaderboardsApiResponse()
        configureSuccessRequest(expectedResult!!)
        val response = restClientUnderTest.fetchLeaderboards(
            site = stubSite,
            startDate = "10-10-2022",
            endDate = "22-10-2022",
            quantity = 5,
            forceRefresh = false,
            interval = "day"
        )

        verify(requestBuilder, times(1)).syncGetRequest(
            restClientUnderTest,
            stubSite,
            WOOCOMMERCE.leaderboards.pathV4Analytics,
            mapOf(
                "before" to "22-10-2022",
                "after" to "10-10-2022",
                "per_page" to "5",
                "interval" to "day",
                "force_cache_refresh" to "false",
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
            site = stubSite,
            startDate = "10-10-2022",
            endDate = "22-10-2022",
            forceRefresh = false,
            quantity = 5,
            interval = "day"
        )

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
                stubSite,
                WOOCOMMERCE.leaderboards.pathV4Analytics,
                mapOf(
                    "after" to "10-10-2022",
                    "before" to "22-10-2022",
                    "per_page" to "5",
                    "interval" to "day",
                    "force_cache_refresh" to "false",
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
                stubSite,
                WOOCOMMERCE.leaderboards.pathV4Analytics,
                mapOf(
                    "after" to "10-10-2022",
                    "before" to "22-10-2022",
                    "per_page" to "5",
                    "interval" to "day",
                    "force_cache_refresh" to "false",
                ),
                Array<LeaderboardsApiResponse>::class.java
            )
        ).thenReturn(jetpackErrorResponse)
    }
}
