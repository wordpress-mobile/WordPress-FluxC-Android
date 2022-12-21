package org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCTaxRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun fetchTaxClassList(
        site: SiteModel
    ): WooPayload<Array<TaxClassApiResponse>> {
        val url = WOOCOMMERCE.taxes.classes.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site,
            url,
            Array<TaxClassApiResponse>::class.java
        )
        return response.toWooPayload()
    }

    data class TaxClassApiResponse(
        val name: String? = null,
        val slug: String? = null
    )
}
