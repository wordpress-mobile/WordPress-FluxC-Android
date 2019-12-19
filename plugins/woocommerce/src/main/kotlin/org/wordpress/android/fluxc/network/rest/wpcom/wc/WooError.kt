package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.store.Store.OnChangedError

class WooError(
    var type: WooErrorType,
    var original: GenericErrorType,
    var message: String? = null
) : OnChangedError

enum class WooErrorType {
    TIMEOUT,
    API_ERROR,
    INVALID_ID,
    GENERIC_ERROR,
    INVALID_RESPONSE,
    AUTHORIZATION_REQUIRED
}

fun WPComGsonNetworkError.toWooError(): WooError {
    val type = when (type) {
        TIMEOUT -> WooErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> WooErrorType.API_ERROR
        PARSE_ERROR,
        CENSORED,
        INVALID_RESPONSE -> WooErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> WooErrorType.AUTHORIZATION_REQUIRED
        NOT_FOUND -> WooErrorType.INVALID_ID
        UNKNOWN,
        null -> WooErrorType.GENERIC_ERROR
    }
    return WooError(type, this.type, message)
}
