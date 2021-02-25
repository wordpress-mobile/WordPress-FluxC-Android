package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error

import org.wordpress.android.fluxc.store.Store.OnChangedError

data class CustomerError(
    val type: CustomerErrorType = CustomerErrorType.GENERIC_ERROR,
    val message: String = ""
) : OnChangedError
