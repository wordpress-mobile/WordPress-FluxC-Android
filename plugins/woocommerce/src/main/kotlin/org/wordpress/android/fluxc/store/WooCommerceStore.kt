package org.wordpress.android.fluxc.store

import android.content.Context
import com.wellsql.generated.SiteModelTable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSSRModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.model.settings.WCSettingsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCAPISystemRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WCApiVersionResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.toDomainModel
import org.wordpress.android.fluxc.persistence.PluginSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WCProductSettingsSqlUtils
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LanguageUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
open class WooCommerceStore @Inject constructor(
    private val appContext: Context,
    dispatcher: Dispatcher,
    private val coroutineEngine: CoroutineEngine,
    private val siteStore: SiteStore,
    private val systemRestClient: WooSystemRestClient,
    private val wcCoreRestClient: WooCommerceRestClient,
    private val wcAPISystemRestClient: WCAPISystemRestClient,
    private val settingsMapper: WCSettingsMapper,
    private val siteSqlUtils: SiteSqlUtils
) : Store(dispatcher) {
    enum class WooPlugin(val pluginName: String) {
        WOO_CORE("woocommerce/woocommerce"),
        WOO_SERVICES("woocommerce-services/woocommerce-services"),
        WOO_PAYMENTS("woocommerce-payments/woocommerce-payments"),
        WOO_STRIPE_GATEWAY("woocommerce-gateway-stripe/woocommerce-gateway-stripe"),
        WOO_SHIPMENT_TRACKING("woocommerce-shipment-tracking/woocommerce-shipment-tracking")
    }
    companion object {
        const val WOO_API_NAMESPACE_V1 = "wc/v1"
        const val WOO_API_NAMESPACE_V2 = "wc/v2"
        const val WOO_API_NAMESPACE_V3 = "wc/v3"
    }

    override fun onRegister() = AppLog.d(T.API, "WooCommerceStore onRegister")

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) = Unit // Do nothing (ignore)

    suspend fun fetchWooCommerceSites(): WooResult<List<SiteModel>> {
        val fetchResult = siteStore.fetchSites(FetchSitesPayload())
        if (fetchResult.isError) {
            emitChange(fetchResult)

            return WooResult(
                WooError(
                    type = GENERIC_ERROR,
                    message = fetchResult.error.message,
                    original = UNKNOWN
                )
            )
        }

        var rowsAffected = fetchResult.rowsAffected
        // Fetch WooCommerce availability for jetpack cp sites
        siteStore.sites.filter { it.isJetpackCPConnected }
            .forEach { site ->
                val isUpdated = fetchUpdatedSiteMetaData(site)
                if (isUpdated.model == true && !fetchResult.updatedSites.contains(site)) {
                    rowsAffected++
                }
            }

        emitChange(OnSiteChanged(rowsAffected, fetchResult.updatedSites))

        return WooResult(getWooCommerceSites())
    }

    @Suppress("ReturnCount")
    suspend fun fetchWooCommerceSite(site: SiteModel): WooResult<SiteModel> {
        if (!site.isJetpackCPConnected) {
            siteStore.fetchSite(site).let {
                if (it.isError) {
                    return WooResult(
                            WooError(
                                    type = GENERIC_ERROR,
                                    message = it.error.message,
                                    original = UNKNOWN
                            )
                    )
                }
            }
        } else {
            // The endpoint used by siteStore to fetch a single site is broken, so we'll update the metadata
            // manually using the remote site directly
            val isUpdated = fetchAndUpdateMetaDataFromSiteSettings(site).let {
                if (it.isError) {
                    return WooResult(
                            WooError(
                                    type = GENERIC_ERROR,
                                    message = it.error.message,
                                    original = UNKNOWN
                            )
                    )
                }
                it.model!!
            }
            if (isUpdated) {
                emitChange(OnSiteChanged(1, listOf(site)))
            }
        }

        return WooResult(siteStore.getSiteBySiteId(site.siteId))
    }

    fun getWooCommerceSites(): MutableList<SiteModel> =
            siteSqlUtils.getSitesWith(SiteModelTable.HAS_WOO_COMMERCE, true).asModel

    /**
     * Given a [SiteModel], returns its WooCommerce site settings, or null if no settings are stored for this site.
     */
    fun getSiteSettings(site: SiteModel): WCSettingsModel? =
            WCSettingsSqlUtils.getSettingsForSite(site)

    /**
     * Given a [SiteModel], returns its WooCommerce product settings, or null if no settings are stored for this site.
     */
    open fun getProductSettings(site: SiteModel): WCProductSettingsModel? =
            WCProductSettingsSqlUtils.getProductSettingsForSite(site)

    /**
     * Given a [SiteModel], returns its WooCommerce store country name,
     * or null if no settings are stored for this site OR if country is empty/blank
     */
    fun getStoreCountryCode(site: SiteModel): String? {
        val siteSettings = WCSettingsSqlUtils.getSettingsForSite(site)
        return siteSettings?.countryCode
    }

    fun getSitePlugin(site: SiteModel, plugin: WooPlugin): SitePluginModel? {
        return PluginSqlUtils.getSitePluginByName(site, plugin.pluginName)
    }

    suspend fun getSitePlugins(site: SiteModel, plugins: List<WooPlugin>): List<SitePluginModel> {
        return coroutineEngine.withDefaultContext(T.DB, this, "getSitePlugins"){
            val pluginNames = plugins.map { it.pluginName }
            PluginSqlUtils.getSitePluginByNames(site, pluginNames)
        }
    }

    suspend fun getSitePlugins(site: SiteModel): List<SitePluginModel> {
        return coroutineEngine.withDefaultContext(T.DB, this, "getSitePlugins") {
            PluginSqlUtils.getSitePlugins(site)
        }
    }

    suspend fun fetchSitePlugins(site: SiteModel): WooResult<List<SitePluginModel>> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchWooCommerceServicesPluginInfo") {
            val response = systemRestClient.fetchInstalledPlugins(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result?.plugins != null -> {
                    val plugins = response.result.plugins.map { it.toDomainModel(site.id) }
                    PluginSqlUtils.insertOrReplaceSitePlugins(site, plugins)
                    WooResult(plugins)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchSSR(site: SiteModel): WooResult<WCSSRModel> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSSR") {
            val response = wcAPISystemRestClient.fetchSSR(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val ssr = WCSSRModel(
                            remoteSiteId = site.siteId,
                            environment = response.result.environment?.toString(),
                            database = response.result.database?.toString(),
                            activePlugins = response.result.activePlugins?.toString(),
                            theme = response.result.theme?.toString(),
                            settings = response.result.settings?.toString(),
                            security = response.result.security?.toString(),
                            pages = response.result.pages?.toString()
                    )
                    WooResult(ssr)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun fetchUpdatedSiteMetaData(site: SiteModel): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchUpdatedSiteMetaData") {
            val updateSettingsResult = fetchAndUpdateMetaDataFromSiteSettings(site)
            if (updateSettingsResult.isError) {
                return@withDefaultContext updateSettingsResult
            }
            return@withDefaultContext WooResult(
                    updateSettingsResult.model!! or fetchAndUpdateWooCommerceAvailability(site)
            )
        }
    }

    private suspend fun fetchAndUpdateMetaDataFromSiteSettings(site: SiteModel): WooResult<Boolean> {
        fun SiteModel.updateFromSiteSettings(response: WooSystemRestClient.WPSiteSettingsResponse) = apply {
            name = response.title ?: name
            description = response.description ?: description
            url = response.url ?: url
            showOnFront = response.showOnFront ?: showOnFront
            pageOnFront = response.pageOnFront ?: pageOnFront
        }

        return coroutineEngine.withDefaultContext(T.API, this, "fetchAndUpdateMetaDataFromSiteSettings") {
            val response = systemRestClient.fetchSiteSettings(site)
            return@withDefaultContext when {
                response.isError -> {
                    AppLog.w(T.API, "fetching site settings  site ${site.siteId}")
                    WooResult(response.error)
                }
                else -> {
                    WooResult(response.result?.let {
                        siteSqlUtils.insertOrUpdateSite(site.updateFromSiteSettings(it)) == 1
                    } ?: false)
                }
            }
        }
    }

    private suspend fun fetchAndUpdateWooCommerceAvailability(site: SiteModel): Boolean {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchAndUpdateWooCommerceAvailability") {
            val response = systemRestClient.checkIfWooCommerceIsAvailable(site)
            return@withDefaultContext when {
                response.isError -> {
                    AppLog.w(T.API, "Checking WooCommerce availability failed for site ${site.siteId}")
                    false
                }
                else -> {
                    response.result?.takeIf { it != site.hasWooCommerce }?.let {
                        site.hasWooCommerce = it
                        siteSqlUtils.insertOrUpdateSite(site)
                        true
                    } ?: false
                }
            }
        }
    }

    suspend fun fetchSupportedApiVersion(
        site: SiteModel,
        overrideRetryPolicy: Boolean = false
    ): WooResult<WCApiVersionResponse> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSupportedWooApiVersion") {
            val response = wcCoreRestClient.fetchSupportedWooApiVersion(site, overrideRetryPolicy)
            return@withDefaultContext when {
                response.isError -> {
                    AppLog.w(
                        T.API,
                        "Checking WooCommerce API version failed for site ${site.siteId}"
                    )
                    WooResult(response.error)
                }
                response.result != null -> {
                    val namespaces = response.result.namespaces
                    val maxWooApiVersion = namespaces?.run {
                        find { it == WOO_API_NAMESPACE_V3 }
                            ?: find { it == WOO_API_NAMESPACE_V2 }
                            ?: find { it == WOO_API_NAMESPACE_V1 }
                    }
                    WooResult(WCApiVersionResponse(
                        siteModel = site,
                        apiVersion = maxWooApiVersion
                    ))
                }
                else -> {
                    WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                }
            }
        }
    }

    suspend fun enableCoupons(site: SiteModel): Boolean {
        return coroutineEngine.withDefaultContext(T.API, this, "enableCoupons") {
            val response = wcCoreRestClient.enableCoupons(site)
            return@withDefaultContext when {
                response.isError -> {
                    AppLog.w(T.API, "Failed to enable coupons for ${site.siteId}")
                    false
                }
                else -> {
                    response.result?.let {
                        WCSettingsSqlUtils.setCouponsEnabled(site, it)
                        it
                    } ?: false
                }
            }
        }
    }

    suspend fun fetchSiteGeneralSettings(site: SiteModel): WooResult<WCSettingsModel> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSiteGeneralSettings") {
            val response = wcCoreRestClient.fetchSiteSettingsGeneral(site)
            return@withDefaultContext when {
                response.isError -> {
                    AppLog.w(
                        T.API,
                        "Failed to fetch Woo Site settings for site ${site.siteId}"
                    )
                    WooResult(response.error)
                }
                response.result != null -> {
                    val settings = settingsMapper.mapSiteSettings(response.result, site)
                    WCSettingsSqlUtils.insertOrUpdateSettings(settings)

                    WooResult(settings)
                }
                else -> {
                    WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                }
            }
        }
    }

    suspend fun fetchSiteProductSettings(site: SiteModel): WooResult<WCProductSettingsModel> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSiteProductSettings") {
            val response = wcCoreRestClient.fetchSiteSettingsProducts(site)
            return@withDefaultContext when {
                response.isError -> {
                    AppLog.w(
                        T.API,
                        "Failed to fetch Woo product settings for site ${site.siteId}"
                    )
                    WooResult(response.error)
                }
                response.result != null -> {
                    val settings = settingsMapper.mapProductSettings(response.result, site)
                    WCProductSettingsSqlUtils.insertOrUpdateProductSettings(settings)

                    WooResult(settings)
                }
                else -> {
                    WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                }
            }
        }
    }

    /**
     * Formats currency amounts for display based on the site's settings and the device locale.
     *
     * If there is no [WCSettingsModel] associated with the given [site], the [rawValue] will be returned without
     * decimal formatting, but with the appropriate currency symbol prepended to the [rawValue].
     *
     * @param rawValue the amount to be formatted
     * @param site the associated [SiteModel] - this will be used to resolve the corresponding [WCSettingsModel]
     * @param currencyCode an optional, ISO 4217 currency code to use. If not supplied, the site's currency code
     * will be used (obtained from the [WCSettingsModel] corresponding to the given [site]
     * @param applyDecimalFormatting whether or not to apply decimal formatting to the value. If `false`, only the
     * currency symbol and positioning will be applied. This is useful for values for 'pretty' display, e.g. $1.2k.
     */
    fun formatCurrencyForDisplay(
        rawValue: String,
        site: SiteModel,
        currencyCode: String? = null,
        applyDecimalFormatting: Boolean
    ): String {
        val siteSettings = getSiteSettings(site)

        // Resolve the currency code to a localized symbol
        val resolvedCurrencyCode = currencyCode?.takeIf { it.isNotEmpty() } ?: siteSettings?.currencyCode
        val currencySymbol = resolvedCurrencyCode?.let {
            WCCurrencyUtils.getLocalizedCurrencySymbolForCode(it, LanguageUtils.getCurrentDeviceLanguage(appContext))
        } ?: ""

        // Format the amount for display according to the site's currency settings
        // Use absolute values - if the value is negative, it will be handled in the next step, with the currency symbol
        val decimalFormattedValue = siteSettings?.takeIf { applyDecimalFormatting }?.let {
            WCCurrencyUtils.formatCurrencyForDisplay(rawValue.toDoubleOrNull()?.absoluteValue ?: 0.0, it)
        } ?: rawValue.removePrefix("-")

        // Append or prepend the currency symbol according to the site's settings
        with(StringBuilder()) {
            if (rawValue.startsWith("-")) { append("-") }
            append(when (siteSettings?.currencyPosition) {
                null, LEFT -> "$currencySymbol$decimalFormattedValue"
                LEFT_SPACE -> "$currencySymbol $decimalFormattedValue"
                RIGHT -> "$decimalFormattedValue$currencySymbol"
                RIGHT_SPACE -> "$decimalFormattedValue $currencySymbol"
            })
            return toString()
        }
    }

    /**
     * Fetches the currency symbol for display based on the site's settings and the device locale.
     *
     * If there is no [WCSettingsModel] associated with the given [site], the [currencyCode] will be returned if
     * available, otherwise an empty string is returned.
     *
     * @param site the associated [SiteModel] - this will be used to resolve the corresponding [WCSettingsModel]
     * @param currencyCode an optional, ISO 4217 currency code to use. If not supplied, the site's currency code
     * will be used (obtained from the [WCSettingsModel] corresponding to the given [site]
     */
    fun getSiteCurrency(
        site: SiteModel,
        currencyCode: String? = null
    ): String {
        val siteSettings = getSiteSettings(site)

        // Resolve the currency code to a localized symbol
        val resolvedCurrencyCode = currencyCode?.takeIf { it.isNotEmpty() } ?: siteSettings?.currencyCode
        return resolvedCurrencyCode?.let {
            WCCurrencyUtils.getLocalizedCurrencySymbolForCode(it, LanguageUtils.getCurrentDeviceLanguage(appContext))
        } ?: ""
    }

    fun formatCurrencyForDisplay(
        amount: Double,
        site: SiteModel,
        currencyCode: String? = null,
        applyDecimalFormatting: Boolean
    ): String {
        return formatCurrencyForDisplay(amount.toString(), site, currencyCode, applyDecimalFormatting)
    }
}
