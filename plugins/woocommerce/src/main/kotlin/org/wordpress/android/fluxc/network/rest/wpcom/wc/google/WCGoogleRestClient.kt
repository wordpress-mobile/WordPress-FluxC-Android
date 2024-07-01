package org.wordpress.android.fluxc.network.rest.wpcom.wc.google

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCGoogleRestClient  @Inject constructor(private val wooNetwork: WooNetwork) {
    companion object {
        const val GOOGLE_ADS_CONNECTED_STATUS = "connected"
    }

    suspend fun fetchGoogleAdsConnectionStatus(
        site: SiteModel
    ): WooPayload<Boolean> {
        val url = WOOCOMMERCE.gla.ads.connection.pathNoVersion
        val result = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = GoogleAdsConnectionStatusResponse::class.java
        ).toWooPayload()

        return when {
            result.isError -> WooPayload(result.error)
            result.result != null -> WooPayload(result.result.status == GOOGLE_ADS_CONNECTED_STATUS)
            else -> WooPayload(false)
        }
    }

    suspend fun fetchGoogleAdsCampaigns(site: SiteModel): WooPayload<List<WCGoogleAdsCampaignDTO>> {
        val url = WOOCOMMERCE.gla.ads.campaigns.pathNoVersion
        val result = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<WCGoogleAdsCampaignDTO>::class.java
        ).toWooPayload()

        return when {
            result.isError -> WooPayload(result.error)
            result.result != null -> WooPayload(result.result.toList())
            else -> WooPayload(emptyList())
        }
    }
}

/**
 * Response model for the Google Ads connection status.
 * The full response has more fields, but for now we only care about the status.
 */
data class GoogleAdsConnectionStatusResponse(
    @SerializedName("status")
    val status: String
)

