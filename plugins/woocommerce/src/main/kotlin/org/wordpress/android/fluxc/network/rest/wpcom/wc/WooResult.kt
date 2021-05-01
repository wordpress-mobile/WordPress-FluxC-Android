package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store

data class WooPayload<T, E: WooError>(val result: T? = null) : Payload<E>() {
    constructor(error: E) : this() {
        this.error = error
    }

    fun asWooResult() = when {
        isError -> WooResult(error)
        result != null -> WooResult<T, E>(result)
        else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
    }
}

data class WooResult<T, E: WooError>(val model: T? = null) : Store.OnChanged<E>() {
    constructor(error: E) : this() {
        this.error = error
    }
}
