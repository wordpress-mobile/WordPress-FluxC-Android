package org.wordpress.android.fluxc.network.rest.wpcom.wc

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCCoreAction
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.discovery.RootWPAPIRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCProductSettingsResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCSiteSettingsResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore.WCSiteSettingsError
import org.wordpress.android.fluxc.store.WooCommerceStore.WCSiteSettingsErrorType
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class WooCommerceRestClient @Inject constructor(
    appContext: Context,
    private val dispatcher: Dispatcher,
    private val jetpackTunnelGsonRequestBuilder: JetpackTunnelGsonRequestBuilder,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to the root wp-json endpoint (`/`) via the Jetpack tunnel (see [JetpackTunnelGsonRequest])
     * for the given [SiteModel], and parses through the `namespaces` field in the result for supported versions
     * of the Woo API.
     *
     */
    suspend fun fetchSupportedWooApiVersion(site: SiteModel): WooPayload<RootWPAPIRestResponse> {
        val url = "/"
        val response = jetpackTunnelGsonRequestBuilder.syncGetRequest(
            this,
            site,
            url,
            mapOf("_fields" to "authentication,namespaces"),
            RootWPAPIRestResponse::class.java
        )
        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * Makes a GET call to `/wc/v3/settings/general` via the Jetpack tunnel (see [JetpackTunnelGsonRequest]),
     * retrieving site settings for the given WooCommerce [SiteModel].
     *
     * Dispatches a [WCCoreAction.FETCHED_SITE_SETTINGS] action with a selected subset of the response values,
     * converted to a [WCSettingsModel].
     */
    fun getSiteSettingsGeneral(site: SiteModel) {
        val url = WOOCOMMERCE.settings.general.pathV3
        val responseType = object : TypeToken<List<SiteSettingsResponse>>() {}.type
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, emptyMap(), responseType,
                { response: List<SiteSettingsResponse>? ->
                    response?.let {
                        val currencyCode = getValueForSettingsField(it, "woocommerce_currency")
                        val currencyPosition = getValueForSettingsField(it, "woocommerce_currency_pos")
                        val currencyThousandSep = getValueForSettingsField(it, "woocommerce_price_thousand_sep")
                        val currencyDecimalSep = getValueForSettingsField(it, "woocommerce_price_decimal_sep")
                        val currencyNumDecimals = getValueForSettingsField(it, "woocommerce_price_num_decimals")
                        val address = getValueForSettingsField(it, "woocommerce_store_address")
                        val address2 = getValueForSettingsField(it, "woocommerce_store_address_2")
                        val city = getValueForSettingsField(it, "woocommerce_store_city")
                        val postalCode = getValueForSettingsField(it, "woocommerce_store_postcode")
                        val countryAndState = getValueForSettingsField(it, "woocommerce_default_country")
                                ?.split(":")
                        val country = countryAndState?.firstOrNull()
                        val state = countryAndState?.getOrNull(1)

                        val settings = WCSettingsModel(
                                localSiteId = site.id,
                                currencyCode = currencyCode ?: "",
                                currencyPosition = CurrencyPosition.fromString(currencyPosition),
                                currencyThousandSeparator = currencyThousandSep ?: "",
                                currencyDecimalSeparator = currencyDecimalSep ?: "",
                                currencyDecimalNumber = currencyNumDecimals?.toIntOrNull() ?: 2,
                                countryCode = country ?: "",
                                stateCode = state ?: "",
                                address = address ?: "",
                                address2 = address2 ?: "",
                                city = city ?: "",
                                postalCode = postalCode ?: ""
                        )

                        val payload = FetchWCSiteSettingsResponsePayload(site, settings)
                        dispatcher.dispatch(WCCoreActionBuilder.newFetchedSiteSettingsAction(payload))
                    } ?: run {
                        val wcSiteSettingsError = WCSiteSettingsError(WCSiteSettingsErrorType.INVALID_RESPONSE)
                        val payload = FetchWCSiteSettingsResponsePayload(wcSiteSettingsError, site)
                        dispatcher.dispatch(WCCoreActionBuilder.newFetchedSiteSettingsAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val payload = FetchWCSiteSettingsResponsePayload(networkErrorToSettingsError(networkError), site)
                    dispatcher.dispatch(WCCoreActionBuilder.newFetchedSiteSettingsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    fun getSiteSettingsProducts(site: SiteModel) {
        val url = WOOCOMMERCE.settings.products.pathV3
        val responseType = object : TypeToken<List<SiteSettingsResponse>>() {}.type
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, emptyMap(), responseType,
                { response: List<SiteSettingsResponse>? ->
                    response?.let {
                        val weightUnit = getValueForSettingsField(it, "woocommerce_weight_unit")
                        val dimensionUnit = getValueForSettingsField(it, "woocommerce_dimension_unit")

                        val settings = WCProductSettingsModel().apply {
                            localSiteId = site.id
                            this.dimensionUnit = dimensionUnit ?: ""
                            this.weightUnit = weightUnit ?: ""
                        }

                        val payload = FetchWCProductSettingsResponsePayload(site, settings)
                        dispatcher.dispatch(WCCoreActionBuilder.newFetchedProductSettingsAction(payload))
                    } ?: run {
                        val wcSiteSettingsError = WCSiteSettingsError(WCSiteSettingsErrorType.INVALID_RESPONSE)
                        val payload = FetchWCProductSettingsResponsePayload(wcSiteSettingsError, site)
                        dispatcher.dispatch(WCCoreActionBuilder.newFetchedProductSettingsAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val payload = FetchWCProductSettingsResponsePayload(networkErrorToSettingsError(networkError), site)
                    dispatcher.dispatch(WCCoreActionBuilder.newFetchedProductSettingsAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
    }

    private fun getValueForSettingsField(settingsResponse: List<SiteSettingsResponse>, field: String): String? {
        return settingsResponse.find { it.id != null && it.id == field }?.value?.asString
    }

    private fun networkErrorToSettingsError(wpComError: WPComGsonNetworkError): WCSiteSettingsError {
        val wcSiteSettingsErrorType = WCSiteSettingsErrorType.fromString(wpComError.apiError)
        return WCSiteSettingsError(wcSiteSettingsErrorType, wpComError.message)
    }
}
