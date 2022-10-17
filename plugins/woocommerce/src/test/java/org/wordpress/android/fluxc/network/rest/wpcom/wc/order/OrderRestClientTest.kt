package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import android.content.Context
import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.order.OrderAddress.Billing
import org.wordpress.android.fluxc.model.order.OrderAddress.Shipping
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload

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
        val createRequest = listOf(
            buildOrderRequest("create1"), buildOrderRequest("create2")
        )
        val updateRequest = listOf(
            buildOrderRequest("update1"), buildOrderRequest("update2")
        )
        val deleteRequest = listOf(
            1.toLong(), 2.toLong(), 3.toLong()
        )

        whenever(
            jetpackTunnelGsonRequestBuilder.syncPostRequest(
                eq(orderRestClient),
                site = eq(site),
                url = eq(WOOCOMMERCE.orders.batch.pathV3),
                body = any(),
                clazz = eq(OrdersBatchDto::class.java)
            )
        ).thenReturn(
            JetpackSuccess(OrdersBatchDto())
        )

        val response = orderRestClient.updateOrdersBatch(
            site = site,
            createRequest = createRequest,
            updateRequest = updateRequest,
            deleteRequest = deleteRequest
        )

        Assertions.assertThat(
            response
        ).isEqualTo(
            WooPayload(
                OrdersDatabaseBatch(
                    createdEntities = emptyList(),
                    updatedEntities = emptyList(),
                    deletedEntities = emptyList()
                )
            )
        )
    }

    private fun buildOrderRequest(orderId: String) = UpdateOrderRequest(
        status = WCOrderStatusModel("$orderId mock-status-key"),
        lineItems = mutableListOf<LineItem>().apply {
            add(buildLineItem(1))
        },
        shippingAddress = buildAddressModel(),
        billingAddress = buildBillingModel(),
        customerNote = "customerNote",
        shippingLines = null
    )

    private fun buildLineItem(id: Int) = LineItem(
        id = id.toLong(),
        name = "$id",
        productId = id.toLong(),
        variationId = id.toLong(),
        quantity = 1f
    )

    private fun buildAddressModel() = Shipping(
        firstName = "firstName",
        lastName = "lastName",
        company = "company",
        address1 = "address1",
        address2 = "address2",
        city = "city",
        state = "state",
        postcode = "postcode",
        country = "countryCode",
        phone = "phone"
    )

    private fun buildBillingModel() = Billing(
        email = "email@email.com",
        firstName = "firstName",
        lastName = "lastName",
        company = "company",
        address1 = "address1",
        address2 = "address2",
        city = "city",
        state = "state",
        postcode = "postcode",
        country = "countryCode",
        phone = "phone"
    )
}
