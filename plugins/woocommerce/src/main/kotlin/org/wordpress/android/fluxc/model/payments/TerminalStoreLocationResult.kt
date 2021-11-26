package org.wordpress.android.fluxc.model.payments

import org.wordpress.android.fluxc.FluxCError
import org.wordpress.android.fluxc.Payload

data class TerminalStoreLocationResult(
    val locationId: String?,
    val displayName: String?,
    val liveMode: Boolean?,
    val address: StoreAddress?
) : Payload<TerminalStoreLocationError?>() {
    constructor(
        error: TerminalStoreLocationError
    ) : this(null, null, null, null) {
        this.error = error
    }

    data class StoreAddress(
        val city: String?,
        val country: String?,
        val line1: String?,
        val line2: String?,
        val postalCode: String?,
        val state: String?
    )
}

data class TerminalStoreLocationError(
    val type: TerminalStoreLocationErrorType = TerminalStoreLocationErrorType.GenericError,
    val message: String = ""
) : FluxCError

sealed class TerminalStoreLocationErrorType {
    object GenericError : TerminalStoreLocationErrorType()
    data class MissingAddress(val addressEditingUrl: String) : TerminalStoreLocationErrorType()
    object InvalidPostalCode : TerminalStoreLocationErrorType()
    object ServerError : TerminalStoreLocationErrorType()
    object NetworkError : TerminalStoreLocationErrorType()
}
