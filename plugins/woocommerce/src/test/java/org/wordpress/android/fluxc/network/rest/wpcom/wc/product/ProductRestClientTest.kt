package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@OptIn(ExperimentalCoroutinesApi::class)
class ProductRestClientTest {
    lateinit var sut: ProductRestClient

    private val productId = 5L
    private val site = SiteModel()
    private val wooNetwork: WooNetwork = mock() {
        onBlocking {
            executePostGsonRequest(
                any(), any(), eq(BatchProductApiResponse::class.java), any()
            )
        } doReturn WPAPIResponse.Success(null)
    }

    @Before fun setUp() {
        sut = ProductRestClient(mock(), mock(), mock(), mock(), mock(), wooNetwork, initCoroutineEngine(), mock())
    }

    @Test
    fun `send only updated parameters with id if products differ`() = runBlockingTest {
        // given
        val product = WCProductModel(productId.toInt()).apply {
            remoteProductId = productId
            status = "unchanged status"
        }
        val bodyCaptor = argumentCaptor<Map<String, Any>> { }

        // when
        sut.batchUpdateProducts(
            site,
            mapOf(
                product.withRegularPrice("20") to product.withRegularPrice("10")
            )
        )

        // then
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.products.batch.pathV3),
            clazz = eq(BatchProductApiResponse::class.java),
            body = bodyCaptor.capture()
        )
        assertThat(bodyCaptor.allValues).hasSize(1)
        assertThat(bodyCaptor.firstValue).isEqualTo(
            mapOf(
                ("update" to listOf(
                    mapOf<String, Any>(
                        ("id" to productId), ("regular_price" to "10")
                    )
                ))
            )
        )
    }

    @Test
    fun `do not send any properties if entities do not differ`() = runBlockingTest {
        // given
        val productA = WCProductModel(2).apply {
            remoteProductId = 2
            status = "unchanged status"
        }
        val productB = WCProductModel(3).apply {
            remoteProductId = 3
            status = "other, unchanged status"
        }
        val bodyCaptor = argumentCaptor<Map<String, Any>> { }

        // when
        sut.batchUpdateProducts(
            site,
            mapOf(
                productA to productA,
                productB to productB,
            )
        )

        // then
        verify(wooNetwork, never()).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.products.batch.pathV3),
            clazz = eq(BatchProductApiResponse::class.java),
            body = bodyCaptor.capture()
        )
    }

    private fun WCProductModel.withRegularPrice(newRegularPrice: String): WCProductModel =
        copy().apply {
            remoteProductId = this@withRegularPrice.remoteProductId
            regularPrice = newRegularPrice
        }
}
