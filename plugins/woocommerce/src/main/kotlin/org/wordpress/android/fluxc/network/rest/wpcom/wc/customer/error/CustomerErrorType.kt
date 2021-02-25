package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.error

import java.util.Locale

enum class CustomerErrorType {
    INVALID_CUSTOMER_ID,

    GENERIC_ERROR;

    companion object {
        private val reverseMap = values().associateBy(CustomerErrorType::name)
        fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
    }
}
