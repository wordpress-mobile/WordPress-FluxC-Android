package org.wordpress.android.fluxc.store

import android.content.Context
import com.wellsql.generated.SiteModelTable
import kotlinx.coroutines.flow.Flow
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCCoreAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSSRModel
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.system.WooSystemRestClient
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils.WCPluginModel
import org.wordpress.android.fluxc.persistence.WCProductSettingsSqlUtils
import org.wordpress.android.fluxc.persistence.WCSettingsSqlUtils
import org.wordpress.android.fluxc.persistence.dao.SSRDao
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
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
    private val siteStore: SiteStore,
    private val systemRestClient: WooSystemRestClient,
    private val wcCoreRestClient: WooCommerceRestClient,
    private val siteSqlUtils: SiteSqlUtils,
    private val ssrDao: SSRDao
) : Store(dispatcher) {
    enum class WooPlugin(val displayName: String) {
        WOO_SERVICES("WooCommerce Shipping &amp; Tax"),
        WOO_PAYMENTS("WooCommerce Payments");
    }
    companion object {
        const val WOO_API_NAMESPACE_V1 = "wc/v1"
        const val WOO_API_NAMESPACE_V2 = "wc/v2"
        const val WOO_API_NAMESPACE_V3 = "wc/v3"
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
            private val reverseMap = values().associateBy(ApiVersionErrorType::name)
            fun fromString(type: String) = reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
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
            private val reverseMap = values().associateBy(WCSiteSettingsErrorType::name)
            fun fromString(type: String) =
                    reverseMap[type.toUpperCase(Locale.US)] ?: GENERIC_ERROR
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

    fun getSitePlugin(site: SiteModel, plugin: WooPlugin): WCPluginModel? {
        return WCPluginSqlUtils.selectSingle(site, plugin.displayName)
    }

    suspend fun getSitePlugins(site: SiteModel): List<WCPluginModel> {
        return coroutineEngine.withDefaultContext(T.DB, this, "getSitePlugins") {
            WCPluginSqlUtils.selectAll(site)
        }
    }

    suspend fun fetchSitePlugins(site: SiteModel): WooResult<List<WCPluginModel>> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchWooCommerceServicesPluginInfo") {
            val response = systemRestClient.fetchInstalledPlugins(site)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result?.plugins != null -> {
                    val plugins = response.result.plugins.map { WCPluginModel(site, it) }
                    WCPluginSqlUtils.insertOrUpdate(plugins)
                    WooResult(plugins)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchSSR(site: SiteModel): WooResult<WCSSRModel> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSSR") {
            val response = systemRestClient.fetchSSR(site)
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
                    ssrDao.insertSSR(ssr.mapToEntity())
                    WooResult(ssr)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeSSRForSite(remoteSiteId: Long): Flow<WCSSRModel> {
        return ssrDao.observeSSRForSite(remoteSiteId)
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
