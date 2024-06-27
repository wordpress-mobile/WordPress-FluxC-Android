package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.google.GoogleAdsProgramsResponse
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
     * Checks the connection status of the Google Ads account used in the plugin.
     *
     * @return WooResult<Boolean> true if the account is connected, false otherwise. Optionally,
     * passes error, too.
     */
    suspend fun isGoogleAdsAccountConnected(site: SiteModel): WooResult<Boolean> =
        coroutineEngine.withDefaultContext(API, this, "fetchGoogleAdsConnectionStatus") {
            val response = restClient.fetchGoogleAdsConnectionStatus(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> response.asWooResult()
                else -> WooResult(false)
            }
    }

    suspend fun fetchAllPrograms(site: SiteModel): WooResult<GoogleAdsProgramsResponse> =
        coroutineEngine.withDefaultContext(API, this, "fetchAllPrograms") {
            val response = restClient.fetchAllPrograms(site)
            when {
                response.isError -> WooResult(response.error)
                else -> WooResult(response.result)
            }
        }
}
