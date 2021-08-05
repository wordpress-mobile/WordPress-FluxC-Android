package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.AddOnsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.dto.AddOnGroupDto
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCAddonsStore @Inject constructor(
    private val restClient: AddOnsRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchAllGlobalAddonsGroups(site: SiteModel): WooResult<List<AddOnGroupDto>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchGlobalAddonsGroups") {
            restClient.fetchGlobalAddOnGroups(site).asWooResult()
        }
    }
}
