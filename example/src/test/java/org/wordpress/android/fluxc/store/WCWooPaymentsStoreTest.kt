package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsDepositsOverviewApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.payments.woo.WooPaymentsRestClient
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

class WCWooPaymentsStoreTest {
    private val restClient = mock<WooPaymentsRestClient>()

    private val store = WCWooPaymentsStore(
        initCoroutineEngine(),
        restClient
    )

    @Test
    fun `given rest returns error, when fetchConnectionToken, then error returned`() = test {
        // GIVEN
        val error = mock<WooError>()
        val restResult = WooPayload<WooPaymentsDepositsOverviewApiResponse>(error)
        whenever(restClient.fetchDepositsOverview(any())).thenReturn(restResult)

        // WHEN
        val result = store.fetchConnectionToken(mock())

        // THEN
        assertThat(result).isEqualTo(restResult)
    }

    @Test
    fun `given rest returns success, when fetchConnectionToken, then success returned`() = test {
        // GIVEN
        val model = mock<WooPaymentsDepositsOverviewApiResponse>()
        val restResult = WooPayload(model)
        whenever(restClient.fetchDepositsOverview(any())).thenReturn(restResult)

        // WHEN
        val result = store.fetchConnectionToken(mock())

        // THEN
        assertThat(result).isEqualTo(restResult)
    }
}