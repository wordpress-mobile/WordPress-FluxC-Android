package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.GatewayMapper
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class WCGatewayStore @Inject constructor(
    private val restClient: GatewayRestClient,
    private val coroutineContext: CoroutineContext,
    private val mapper: GatewayMapper
) {
    fun getGateway(site: SiteModel, gatewayId: String): WCGatewayModel? =
            WCGatewaySqlUtils.selectGateway(site, gatewayId)?.let { mapper.map(it) }

    suspend fun fetchGateway(site: SiteModel, gatewayId: String): WooResult<WCGatewayModel> {
        return withContext(coroutineContext) {
            val response = restClient.fetchGateway(site, gatewayId)
            return@withContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    WCGatewaySqlUtils.insertOrUpdate(site, response.result)
                    WooResult(mapper.map(response.result))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun getAllGateways(site: SiteModel): List<WCGatewayModel> =
            WCGatewaySqlUtils.selectAllGateways(site).map { mapper.map(it) }

    suspend fun fetchAllGateways(site: SiteModel): WooResult<List<WCGatewayModel>> {
        return withContext(coroutineContext) {
            val response = restClient.fetchAllGateways(site)
            return@withContext when {
                response.isError -> {
                    WooResult(response.error)
                }
                response.result != null -> {
                    WCGatewaySqlUtils.insertOrUpdate(site, response.result.toList())
                    WooResult(response.result.map { mapper.map(it) })
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
