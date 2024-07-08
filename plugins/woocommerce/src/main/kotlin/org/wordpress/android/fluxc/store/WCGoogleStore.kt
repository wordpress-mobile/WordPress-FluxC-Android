package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaign
import org.wordpress.android.fluxc.model.google.WCGoogleAdsCampaignMapper
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
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCGoogleAdsCampaignMapper
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

/**
 * Fetches the Google Ads campaigns.
 *
 * @param excludeRemovedCampaigns Exclude removed (deleted) campaigns from being fetched.
 * @return WooResult<List<WCGoogleAdsCampaign>> a list of Google Ads campaigns. Optionally,
 * passes error, too.
 */
suspend fun fetchGoogleAdsCampaigns(
    site: SiteModel,
    excludeRemovedCampaigns: Boolean = true
): WooResult<List<WCGoogleAdsCampaign>> =
    coroutineEngine.withDefaultContext(API, this, "fetchGoogleAdsCampaigns") {
        val response = restClient.fetchGoogleAdsCampaigns(site, excludeRemovedCampaigns)
        when {
            response.isError -> WooResult(response.error)
            response.result != null -> {
                val campaigns = response.result.map { mapper.mapToModel(it) }
                WooResult(campaigns)
            }
            else -> WooResult(emptyList())
        }
    }
}
