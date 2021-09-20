package org.wordpress.android.fluxc.model.pay

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.store.Store.OnChangedError

data class WCTerminalStoreLocationResult(
    val locationId: String
) : Payload<WCTerminalStoreLocationError>()

class WCTerminalStoreLocationError : OnChangedError