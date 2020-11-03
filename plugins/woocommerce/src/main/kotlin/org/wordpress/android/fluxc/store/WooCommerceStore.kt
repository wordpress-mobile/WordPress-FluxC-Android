package org.wordpress.android.fluxc.store

import android.content.Context
import com.wellsql.generated.SiteModelTable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCCoreAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.plugins.WooPluginRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils.WCPluginModel
import org.wordpress.android.fluxc.persistence.WCProductSettingsSqlUtils
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.LanguageUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
open class WooCommerceStore @Inject constructor(
    private val appContext: Context,
    dispatcher: Dispatcher,
    private val coroutineEngine: CoroutineEngine,
    private val pluginRestClient: WooPluginRestClient,
    private val wcCoreRestClient: WooCommerceRestClient
) : Store(dispatcher) {
    companion object {
        const val WOO_API_NAMESPACE_V1 = "wc/v1"
        const val WOO_API_NAMESPACE_V2 = "wc/v2"
        const val WOO_API_NAMESPACE_V3 = "wc/v3"

        private const val WOO_SERVICES_PLUGIN_NAME = "woocommerce-services"
    }

    class FetchApiVersionResponsePayload(
        var site: SiteModel,
        var version: String
    ) : Payload<ApiVersionError>() {
        constructor(error: ApiVersionError, site: SiteModel) : this(site, "") { this.error = error }
    }

    class ApiVersionError(
        val type: ApiVersionErrorType = ApiVersionErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class ApiVersionErrorType {
        GENERIC_ERROR,
        NO_WOO_API;

        companion object {
            private val reverseMap = ApiVersionErrorType.values().associateBy(ApiVersionErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: ApiVersionErrorType.GENERIC_ERROR
        }
    }

    class FetchWCSiteSettingsResponsePayload(
        val site: SiteModel,
        val settings: WCSettingsModel?
    ) : Payload<WCSiteSettingsError>() {
        constructor(error: WCSiteSettingsError, site: SiteModel) : this(site, null) { this.error = error }
    }

    class WCSiteSettingsError(
        val type: WCSiteSettingsErrorType = WCSiteSettingsErrorType.GENERIC_ERROR,
        val message: String = ""
    ) : OnChangedError

    enum class WCSiteSettingsErrorType {
        GENERIC_ERROR,
        INVALID_RESPONSE;

        companion object {
            private val reverseMap = WCSiteSettingsErrorType.values().associateBy(WCSiteSettingsErrorType::name)
            fun fromString(type: String) =
                    reverseMap[type.toUpperCase(Locale.US)] ?: WCSiteSettingsErrorType.GENERIC_ERROR
        }
    }

    class FetchWCProductSettingsResponsePayload(
        val site: SiteModel,
        val settings: WCProductSettingsModel?
    ) : Payload<WCSiteSettingsError>() {
        constructor(error: WCSiteSettingsError, site: SiteModel) : this(site, null) { this.error = error }
    }

    // OnChanged events
    class OnApiVersionFetched(val site: SiteModel, val apiVersion: String) : OnChanged<ApiVersionError>()

    class OnWCSiteSettingsChanged(val site: SiteModel) : OnChanged<WCSiteSettingsError>()

    class OnWCProductSettingsChanged(val site: SiteModel) : OnChanged<WCSiteSettingsError>()

    override fun onRegister() = AppLog.d(T.API, "WooCommerceStore onRegister")

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCCoreAction ?: return
        when (actionType) {
            // Remote actions
            WCCoreAction.FETCH_SITE_API_VERSION -> getApiVersion(action.payload as SiteModel)
            WCCoreAction.FETCH_SITE_SETTINGS -> fetchSiteSettings(action.payload as SiteModel)
            WCCoreAction.FETCH_PRODUCT_SETTINGS -> fetchProductSettings(action.payload as SiteModel)
            // Remote responses
            WCCoreAction.FETCHED_SITE_API_VERSION ->
                handleGetApiVersionCompleted(action.payload as FetchApiVersionResponsePayload)
            WCCoreAction.FETCHED_SITE_SETTINGS ->
                handleFetchSiteSettingsCompleted(action.payload as FetchWCSiteSettingsResponsePayload)
            WCCoreAction.FETCHED_PRODUCT_SETTINGS ->
                handleFetchProductSettingsCompleted(action.payload as FetchWCProductSettingsResponsePayload)
        }
    }

    fun getWooCommerceSites(): MutableList<SiteModel> =
            SiteSqlUtils.getSitesWith(SiteModelTable.HAS_WOO_COMMERCE, true).asModel

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

    fun getWooCommerceServicesPluginInfo(site: SiteModel): WCPluginModel? {
        return WCPluginSqlUtils.selectSingle(site, WOO_SERVICES_PLUGIN_NAME)
    }

    suspend fun fetchWooCommerceServicesPluginInfo(
        site: SiteModel
    ): WooResult<WCPluginModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchWooCommerceServicesPluginInfo") {
            val response = pluginRestClient.fetchInstalledPlugins(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val plugins = response.result.plugins.map { WCPluginModel(site, it) }
                    WCPluginSqlUtils.insertOrUpdate(plugins)
                    val plugin = WCPluginSqlUtils.selectSingle(site, WOO_SERVICES_PLUGIN_NAME)
                    WooResult(plugin)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
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

    fun formatCurrencyForDisplay(
        amount: Double,
        site: SiteModel,
        currencyCode: String? = null,
        applyDecimalFormatting: Boolean
    ): String {
        return formatCurrencyForDisplay(amount.toString(), site, currencyCode, applyDecimalFormatting)
    }

    private fun getApiVersion(site: SiteModel) = wcCoreRestClient.getSupportedWooApiVersion(site)

    private fun fetchSiteSettings(site: SiteModel) = wcCoreRestClient.getSiteSettingsGeneral(site)

    private fun fetchProductSettings(site: SiteModel) = wcCoreRestClient.getSiteSettingsProducts(site)

    private fun handleFetchSiteSettingsCompleted(payload: FetchWCSiteSettingsResponsePayload) {
        val onWCSiteSettingsChanged = OnWCSiteSettingsChanged(payload.site)
        if (payload.isError || payload.settings == null) {
            onWCSiteSettingsChanged.error =
                    payload.error ?: WCSiteSettingsError(WCSiteSettingsErrorType.INVALID_RESPONSE)
        } else {
            WCSettingsSqlUtils.insertOrUpdateSettings(payload.settings)
        }

        emitChange(onWCSiteSettingsChanged)
    }

    private fun handleFetchProductSettingsCompleted(payload: FetchWCProductSettingsResponsePayload) {
        val onWCProductSettingsChanged = OnWCProductSettingsChanged(payload.site)
        if (payload.isError || payload.settings == null) {
            onWCProductSettingsChanged.error =
                    payload.error ?: WCSiteSettingsError(WCSiteSettingsErrorType.INVALID_RESPONSE)
        } else {
            WCProductSettingsSqlUtils.insertOrUpdateProductSettings(payload.settings)
        }

        emitChange(onWCProductSettingsChanged)
    }

    private fun handleGetApiVersionCompleted(payload: FetchApiVersionResponsePayload) {
        val onApiVersionFetched: OnApiVersionFetched

        if (payload.isError) {
            onApiVersionFetched = OnApiVersionFetched(payload.site, "").also { it.error = payload.error }
        } else {
            onApiVersionFetched = OnApiVersionFetched(payload.site, payload.version)
        }

        emitChange(onApiVersionFetched)
    }
}
