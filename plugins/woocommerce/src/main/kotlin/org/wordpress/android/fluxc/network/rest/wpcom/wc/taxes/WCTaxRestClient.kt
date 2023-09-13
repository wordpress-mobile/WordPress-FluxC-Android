package org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
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

    suspend fun fetchTaxRateList(
        site: SiteModel,
        page: Int,
        pageSize: Int,
    ): WooPayload<Array<TaxRateModel>> {
        val url = WOOCOMMERCE.taxes.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site,
            url,
            Array<TaxRateModel>::class.java,
            mutableMapOf<String, String>().apply {
                put("page", page.toString())
                put("per_page", pageSize.toString())
            }
        )
        return when (response) {
            is WPAPIResponse.Success -> {
                WooPayload(response.data)
            }
            is WPAPIResponse.Error -> {
                WooPayload(response.error.toWooError())
            }
        }
    }

    data class TaxClassApiResponse(
        val name: String? = null,
        val slug: String? = null
    )

    data class TaxRateModel(
        val id: Int,
        val country: String? = null,
        val state: String? = null,
        val postcode: String? = null,
        val city: String? = null,
        @SerializedName("postcodes") val postCodes: List<String>? = null,
        val cities: List<String>? = null,
        val rate: String? = null,
        val name: String? = null,
        val priority: Int? = null,
        val compound: Boolean? = null,
        val shipping: Boolean? = null,
        val order: Int? = null,
        @SerializedName("class") val taxClass: String? = null,
    )
}
