package org.wordpress.android.fluxc.wc.subscription

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.subscription.SubscriptionRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.subscription.SubscriptionRestClient.SubscriptionDto
import org.wordpress.android.fluxc.store.WCSubscriptionStore
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCSubscriptionStoreTest {
    private val subscriptionRestClient: SubscriptionRestClient = mock()
    private val store = WCSubscriptionStore(
        coroutineEngine = initCoroutineEngine(),
        restClient = subscriptionRestClient,
        mapper = mock()
    )

    private val subscription = mock<SubscriptionDto>()

    @Test
    fun `fetch subscription by order id should return positive when a subscription is found`() =
        runBlocking {
            // given
            val orderId = 234L
            val site = SiteModel()
            // when
            whenever(
                subscriptionRestClient.fetchSubscriptionsByOrderId(
                    orderId = orderId,
                    site = site
                )
            ) doReturn WooPayload(arrayOf(subscription))
            val result = store.fetchSubscriptionsByOrderId(
                site = site,
                orderId = orderId
            )

            // then
            Assertions.assertThat(result.isError).isFalse
            Assertions.assertThat(result.model).isNotNull
            Unit
        }

    @Test
    fun `fetch subscription by order id should return error when subscription is null`() =
        runBlocking {
            // given
            val orderId = 234L
            val site = SiteModel()
            // when
            whenever(
                subscriptionRestClient.fetchSubscriptionsByOrderId(
                    orderId = orderId,
                    site = site
                )
            ) doReturn WooPayload(null)
            val result = store.fetchSubscriptionsByOrderId(
                site = site,
                orderId = orderId
            )

            // then
            Assertions.assertThat(result.isError).isTrue
            Assertions.assertThat(result.model).isNull()
            Assertions.assertThat(result.error.type).isEqualTo(WooErrorType.GENERIC_ERROR)
            Assertions.assertThat(result.error.original)
                .isEqualTo(BaseRequest.GenericErrorType.UNKNOWN)
            Unit
        }

    @Test
    fun `fetch subscription by order id should return error when the request fails`() =
        runBlocking {
            // given
            val orderId = 234L
            val site = SiteModel()
            val error = WooError(
                WooErrorType.GENERIC_ERROR,
                BaseRequest.GenericErrorType.PARSE_ERROR
            )
            // when
            whenever(
                subscriptionRestClient.fetchSubscriptionsByOrderId(
                    orderId = orderId,
                    site = site
                )
            ) doReturn WooPayload(error)
            val result = store.fetchSubscriptionsByOrderId(
                site = site,
                orderId = orderId
            )

            // then
            Assertions.assertThat(result.isError).isTrue
            Assertions.assertThat(result.model).isNull()
            Assertions.assertThat(result.error.type).isEqualTo(WooErrorType.GENERIC_ERROR)
            Assertions.assertThat(result.error.original)
                .isEqualTo(BaseRequest.GenericErrorType.PARSE_ERROR)
            Unit
        }
}
