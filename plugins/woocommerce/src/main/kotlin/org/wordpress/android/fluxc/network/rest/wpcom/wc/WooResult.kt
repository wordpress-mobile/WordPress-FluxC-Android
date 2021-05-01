package org.wordpress.android.fluxc.network.rest.wpcom.wc

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store

data class WooPayload<RESULT, ERROR : WooError>(val result: RESULT? = null) : Payload<ERROR>() {
    constructor(error: ERROR) : this() {
        this.error = error
    }

    fun asWooResult() = when {
        isError -> WooResult(error)
        result != null -> WooResult<RESULT, ERROR>(result)
        else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
    }
}

data class WooResult<RESULT, ERROR : WooError>(val model: RESULT? = null) : Store.OnChanged<ERROR>() {
    constructor(error: ERROR) : this() {
        this.error = error
    }
}
