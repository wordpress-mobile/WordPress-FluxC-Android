package org.wordpress.android.fluxc.model.pay

import org.wordpress.android.fluxc.FluxCError
import org.wordpress.android.fluxc.Payload

data class WCTerminalStoreLocationResult(
    val locationId: String?,
    val displayName: String?,
    val liveMode: Boolean?,
    val address: StoreAddress?
) : Payload<WCTerminalStoreLocationError?>() {
    constructor(
        error: WCTerminalStoreLocationError
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

data class WCTerminalStoreLocationError(
    val type: WCTerminalStoreLocationErrorType = WCTerminalStoreLocationErrorType.GENERIC_ERROR,
    val message: String = ""
) : FluxCError

enum class WCTerminalStoreLocationErrorType {
    GENERIC_ERROR,
    MISSING_ADDRESS,
    SERVER_ERROR,
    NETWORK_ERROR
}
