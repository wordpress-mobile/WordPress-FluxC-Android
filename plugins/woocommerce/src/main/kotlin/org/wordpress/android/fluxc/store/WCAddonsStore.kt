package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.AddOnsRestClient
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions
import org.wordpress.android.fluxc.persistence.entity.GlobalAddonGroupEntity
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

    // TODO: 18/08/2021 @wzieba add tests for this method 
    fun observeAddonsForProduct(siteRemoteId: Long, product: WCProductModel): Flow<List<AddonWithOptions>> {
        return dao.observeGlobalAddonsForSite(siteRemoteId = siteRemoteId)
                .map { globalGroups ->
                    globalGroups.filter { globalGroup ->
                        globalGroup.group.appliesToEveryProduct() || globalGroup.group.appliesToProduct(product)
                    }.flatMap { it.addons }
                }
                .combine(
                        dao.observeSingleProductAddons(
                                siteRemoteId = siteRemoteId,
                                productRemoteId = product.remoteProductId
                        )
                ) { fromGlobalAddons, fromSingleProductAddons ->
                    fromGlobalAddons + fromSingleProductAddons
                }
    }

    private fun GlobalAddonGroupEntity.appliesToEveryProduct(): Boolean {
        return this.restrictedCategoriesIds.isEmpty()
    }

    private fun GlobalAddonGroupEntity.appliesToProduct(product: WCProductModel): Boolean {
        val productCategoriesIds = product.getCategoryList().map { it.id }

        return restrictedCategoriesIds.intersect(productCategoriesIds).isNotEmpty()
    }
}
