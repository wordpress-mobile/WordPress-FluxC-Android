package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.AddOnsRestClient
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupWithAddons
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.usecase.CacheGlobalAddonsGroups
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCAddonsStore @Inject internal constructor(
    private val restClient: AddOnsRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val cacheGlobalAddonsGroupsUseCase: CacheGlobalAddonsGroups,
    private val addonsDao: AddonsDao
) {
    suspend fun fetchAllGlobalAddonsGroups(site: SiteModel): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchGlobalAddonsGroups") {
            val response = restClient.fetchGlobalAddOnGroups(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val globalAddonGroups = response.result
                    cacheGlobalAddonsGroupsUseCase.invoke(globalAddonGroups, site.siteId)
                    WooResult(Unit)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun observeGlobalAddonsGroups(remoteSiteId: Long): Flow<List<GlobalAddonGroupWithAddons>> {
        return addonsDao.getGlobalAddonsForSite(remoteSiteId)
    }
}
