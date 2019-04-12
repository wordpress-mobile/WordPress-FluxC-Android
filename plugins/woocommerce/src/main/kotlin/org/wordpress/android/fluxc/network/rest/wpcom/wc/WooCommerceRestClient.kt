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
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.ApiVersionError
import org.wordpress.android.fluxc.store.WooCommerceStore.ApiVersionErrorType
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchApiVersionResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCProductSettingsResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCSiteSettingsResponsePayload
import org.wordpress.android.fluxc.store.WooCommerceStore.WCSiteSettingsError
import org.wordpress.android.fluxc.store.WooCommerceStore.WCSiteSettingsErrorType
import javax.inject.Singleton

@Singleton
class WooCommerceRestClient(
    appContext: Context,
    private val dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to the root wp-json endpoint (`/`) via the Jetpack tunnel (see [JetpackTunnelGsonRequest])
     * for the given [SiteModel], and parses through the `namespaces` field in the result for supported versions
     * of the Woo API.
     *
     * Dispatches a [WCCoreAction.FETCHED_SITE_API_VERSION] action with the highest version of the Woo API supported
     * by the site (but no newer than the latest supported by FluxC).
     */
    fun getSupportedWooApiVersion(site: SiteModel) {
        val url = "/"
        val params = mapOf("_fields" to "authentication,namespaces")
        val request = JetpackTunnelGsonRequest.buildGetRequest(url, site.siteId, params,
                RootWPAPIRestResponse::class.java,
                { response: RootWPAPIRestResponse? ->
                    val namespaces = response?.namespaces

                    val maxWooApiVersion = namespaces?.run {
                        find { it == WooCommerceStore.WOO_API_NAMESPACE_V3 }
                                ?: find { it == WooCommerceStore.WOO_API_NAMESPACE_V2 }
                                ?: find { it == WooCommerceStore.WOO_API_NAMESPACE_V1 }
                    }

                    maxWooApiVersion?.let { maxApiVersion ->
                        val payload = FetchApiVersionResponsePayload(site, maxApiVersion)
                        dispatcher.dispatch(WCCoreActionBuilder.newFetchedSiteApiVersionAction(payload))
                    } ?: run {
                        val apiVersionError = ApiVersionError(ApiVersionErrorType.NO_WOO_API)
                        val payload = FetchApiVersionResponsePayload(apiVersionError, site)
                        dispatcher.dispatch(WCCoreActionBuilder.newFetchedSiteApiVersionAction(payload))
                    }
                },
                WPComErrorListener { networkError ->
                    val payload = FetchApiVersionResponsePayload(networkErrorToApiVersionError(networkError), site)
                    dispatcher.dispatch(WCCoreActionBuilder.newFetchedSiteApiVersionAction(payload))
                },
                { request: WPComGsonRequest<*> -> add(request) })
        add(request)
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
                        val settings = WCSettingsModel(
                                localSiteId = site.id,
                                currencyCode = currencyCode ?: "",
                                currencyPosition = CurrencyPosition.fromString(currencyPosition),
                                currencyThousandSeparator = currencyThousandSep ?: "",
                                currencyDecimalSeparator = currencyDecimalSep ?: "",
                                currencyDecimalNumber = currencyNumDecimals?.toIntOrNull() ?: 2
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

    private fun networkErrorToApiVersionError(wpComError: WPComGsonNetworkError): ApiVersionError {
        val apiVersionErrorErrorType = ApiVersionErrorType.fromString(wpComError.apiError)
        return ApiVersionError(apiVersionErrorErrorType, wpComError.message)
    }

    private fun networkErrorToSettingsError(wpComError: WPComGsonNetworkError): WCSiteSettingsError {
        val wcSiteSettingsErrorType = WCSiteSettingsErrorType.fromString(wpComError.apiError)
        return WCSiteSettingsError(wcSiteSettingsErrorType, wpComError.message)
    }
}
