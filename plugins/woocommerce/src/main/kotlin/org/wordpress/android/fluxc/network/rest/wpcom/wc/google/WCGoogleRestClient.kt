package org.wordpress.android.fluxc.network.rest.wpcom.wc.google

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import javax.inject.Singleton

@Singleton
class WCGoogleRestClient {
    fun fetchGoogleAdsConnectionStatus(): WooPayload<Boolean> {
        return WooPayload(false)
    }
}
