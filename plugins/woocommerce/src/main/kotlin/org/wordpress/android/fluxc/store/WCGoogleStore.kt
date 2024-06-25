package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.WCGoogleRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This store is intended to be used to support the Google Listings and Ads plugin.
 * https://wordpress.org/plugins/google-listings-and-ads/
 */
@Singleton
class WCGoogleStore @Inject constructor(
    private val restClient: WCGoogleRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    /**
     * Fetches the connection status of the Google Ads account.
     *
     * @return `true` if the connection is successful, `false` otherwise.
     */
    suspend fun fetchGoogleAdsConnectionStatus(site: SiteModel): WooResult<Boolean> =
        coroutineEngine.withDefaultContext(API, this, "fetchGoogleAdsConnectionStatus") {
            val response = restClient.fetchGoogleAdsConnectionStatus(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> response.asWooResult()
                else -> WooResult(false)
            }
    }
}
