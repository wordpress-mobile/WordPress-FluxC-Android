package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import android.content.Context
import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError

@RunWith(MockitoJUnitRunner::class)
class OrderRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder
    @Mock private lateinit var orderDtoMapper: OrderDtoMapper
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var context: Context

    @Mock private lateinit var site: SiteModel

    private lateinit var orderRestClient: OrderRestClient

    @Before
    fun setup() {
        orderRestClient = OrderRestClient(
            dispatcher = dispatcher,
            appContext = context,
            requestQueue = requestQueue,
            jetpackTunnelGsonRequestBuilder = jetpackTunnelGsonRequestBuilder,
            orderDtoMapper = orderDtoMapper,
            accessToken = accessToken,
            userAgent = userAgent
        )
    }

    @Test
    fun `updateOrdersBatch should call gson builder request with expected params`(): Unit = runBlocking {
        val deleteRequest = listOf(1L, 2L, 3L)

        whenever(
            jetpackTunnelGsonRequestBuilder.syncPostRequest(
                any(), any(), any(), any(), any<Class<*>>()
            )
        ).thenReturn(
            JetpackSuccess(OrdersBatchDto())
        )

        orderRestClient.updateOrdersBatch(
            site = site,
            createRequest = emptyList(),
            updateRequest = emptyList(),
            deleteRequest = deleteRequest
        )

        verify(
            jetpackTunnelGsonRequestBuilder, times(1)
        ).syncPostRequest(
            restClient = eq(orderRestClient),
            site = eq(site),
            url = eq(WOOCOMMERCE.orders.batch.pathV3),
            body = eq(mapOf("delete" to deleteRequest)),
            clazz = eq(OrdersBatchDto::class.java)
        )
    }

    @Test
    fun `when updateOrdersBatch fails should result into expected`(): Unit = runBlocking {
        val jetpackErrorResponse = mock<JetpackError<OrdersBatchDto>>()
        whenever(jetpackErrorResponse.error).thenReturn(
            WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR))
        )
        whenever(
            jetpackTunnelGsonRequestBuilder.syncPostRequest(
                any(), any(), any(), any(), any<Class<*>>()
            )
        ).thenReturn(
            jetpackErrorResponse
        )

        val response = orderRestClient.updateOrdersBatch(
            site = site,
            createRequest = emptyList(),
            updateRequest = emptyList(),
            deleteRequest = emptyList()
        )

        assertThat(response).isNotNull
        assertThat(response.result).isNull()
        assertThat(response.error).isNotNull
        assertThat(response.error).isExactlyInstanceOf(WooError::class.java)
    }
}
