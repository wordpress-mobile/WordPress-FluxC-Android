package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error

import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError

// TO-DO find out what error code should be handled
fun networkErrorToCustomerError(wpComError: WPComGsonNetworkError): CustomerError {
    val errorType = when (wpComError.apiError) {
        "woocommerce_rest_invalid_id" -> CustomerErrorType.INVALID_CUSTOMER_ID
        else -> CustomerErrorType.fromString(wpComError.apiError)
    }
    return CustomerError(errorType, wpComError.message)
}
