package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error

import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError

// Source of truth:
// https://github.com/woocommerce/woocommerce/blob/master/includes/rest-api/Controllers/Version1/class-wc-rest-customers-v1-controller.php
fun networkErrorToCustomerError(wpComError: WPComGsonNetworkError): CustomerError {
    val errorType = when (wpComError.apiError) {
        "woocommerce_rest_invalid_id" -> CustomerErrorType.INVALID_CUSTOMER_ID
        "woocommerce_rest_cannot_create",
        "woocommerce_rest_cannot_view",
        "woocommerce_rest_cannot_edit",
        "woocommerce_rest_cannot_delete",
        "woocommerce_rest_cannot_batch" -> CustomerErrorType.PERMISSION_MISSING
        else -> CustomerErrorType.fromString(wpComError.apiError)
    }
    return CustomerError(errorType, wpComError.message)
}
