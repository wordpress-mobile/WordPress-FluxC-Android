package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError

fun <T> WPAPIResponse<T>.toWooPayload() =
    when (this) {
        is WPAPIResponse.Success -> WooPayload(data)
        is WPAPIResponse.Error -> WooPayload(error.toWooError())
    }
