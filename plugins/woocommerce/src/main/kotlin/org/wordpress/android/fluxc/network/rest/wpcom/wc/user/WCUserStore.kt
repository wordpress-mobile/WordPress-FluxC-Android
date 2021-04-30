package org.wordpress.android.fluxc.network.rest.wpcom.wc.user

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCUserStore @Inject constructor(
    private val restClient: WCUserRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCUserMapper
) {
    suspend fun fetchUserRole(site: SiteModel): WooResult<UserRole> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchUserInfo") {
            val response = restClient.fetchUserInfo(site)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    WooResult(mapper.map(response.result))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
