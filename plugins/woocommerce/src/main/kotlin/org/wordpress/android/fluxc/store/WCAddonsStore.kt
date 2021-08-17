package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.AddOnsRestClient
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCAddonsStore @Inject internal constructor(
    private val restClient: AddOnsRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val dao: AddonsDao
) {
    suspend fun fetchAllGlobalAddonsGroups(site: SiteModel): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchGlobalAddonsGroups") {
            val response = restClient.fetchGlobalAddOnGroups(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val globalAddonGroups = response.result
                    dao.cacheGroups(globalAddonGroups, site.siteId)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
