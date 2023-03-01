package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.subscription.WCSubscriptionMapper
import org.wordpress.android.fluxc.model.subscription.WCSubscriptionModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.subscription.SubscriptionRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCSubscriptionStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: SubscriptionRestClient,
    private val mapper: WCSubscriptionMapper
) {
    suspend fun fetchSubscriptionsByOrderId(
        site: SiteModel,
        orderId: Long
    ): WooResult<List<WCSubscriptionModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchSubscriptionsByOrderId") {
            val response = restClient.fetchSubscriptionsByOrderId(site, orderId)
            return@withDefaultContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    val subscriptions = response.result.map { dto -> mapper.map(dto, orderId, site) }
                    WooResult(subscriptions)
                }
                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }
}
