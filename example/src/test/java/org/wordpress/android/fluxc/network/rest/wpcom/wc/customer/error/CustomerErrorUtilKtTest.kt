package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error

import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import kotlin.test.assertEquals

class CustomerErrorUtilKtTest {
    @Test
    fun `invalid id should maps to invalid customer id error`() {
        // given
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "woocommerce_rest_invalid_id"
            message = "message"
        }

        // when & then
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.INVALID_CUSTOMER_ID,
                        message = "message"
                )
        )
    }

    @Test
    fun `rest cannot create maps to permission missing error`() {
        // given
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "woocommerce_rest_cannot_create"
            message = "message"
        }

        // when & then
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.PERMISSION_MISSING,
                        message = "message"
                )
        )
    }

    @Test
    fun `rest cannot view maps to permission missing error`() {
        // given
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "woocommerce_rest_cannot_view"
            message = "message"
        }

        // when & then
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.PERMISSION_MISSING,
                        message = "message"
                )
        )
    }

    @Test
    fun `rest cannot edit maps to permission missing error`() {
        // given
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "woocommerce_rest_cannot_edit"
            message = "message"
        }

        // when & then
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.PERMISSION_MISSING,
                        message = "message"
                )
        )
    }

    @Test
    fun `rest cannot delete maps to permission missing error`() {
        // GIVEN
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "woocommerce_rest_cannot_delete"
            message = "message"
        }

        // WHEN & THEN
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.PERMISSION_MISSING,
                        message = "message"
                )
        )
    }

    @Test
    fun `rest cannot batch to permission missing error`() {
        // GIVEN
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "woocommerce_rest_cannot_batch"
            message = "message"
        }

        // when & then
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.PERMISSION_MISSING,
                        message = "message"
                )
        )
    }

    @Test
    fun `not supported error maps to generic error`() {
        // given
        val error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)).apply {
            apiError = "not_supported_message"
            message = "message"
        }

        // when & then
        assertEquals(
                networkErrorToCustomerError(error),
                CustomerError(
                        type = CustomerErrorType.GENERIC_ERROR,
                        message = "message"
                )
        )
    }
}
