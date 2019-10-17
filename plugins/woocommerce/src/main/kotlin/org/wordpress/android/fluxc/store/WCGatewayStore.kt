package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.GatewayMapper
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.CENSORED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.INVALID_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_AUTHENTICATED
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NO_CONNECTION
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.TIMEOUT
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.gateways.GatewayRestClient
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.store.WCGatewayStore.GatewayError
import org.wordpress.android.fluxc.store.WCGatewayStore.GatewayErrorType
import org.wordpress.android.fluxc.store.WCGatewayStore.GatewayErrorType.GENERIC_ERROR
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

    suspend fun fetchGateway(site: SiteModel, gatewayId: String): GatewayResult<WCGatewayModel> {
        return withContext(coroutineContext) {
            val response = restClient.fetchGateway(site, gatewayId)
            return@withContext when {
                response.isError -> GatewayResult(response.error)
                response.result != null -> {
                    WCGatewaySqlUtils.insertOrUpdate(site, response.result)
                    GatewayResult(mapper.map(response.result))
                }
                else -> GatewayResult(GatewayError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun getAllGateways(site: SiteModel, orderId: Long): List<WCGatewayModel> =
            WCGatewaySqlUtils.selectAllGateways(site).map { mapper.map(it) }

    suspend fun fetchAllGateways(site: SiteModel): GatewayResult<List<WCGatewayModel>> {
        return withContext(coroutineContext) {
            val response = restClient.fetchAllGateways(site)
            return@withContext when {
                response.isError -> {
                    GatewayResult(response.error)
                }
                response.result != null -> {
                    WCGatewaySqlUtils.insertOrUpdate(site, response.result.toList())
                    GatewayResult(response.result.map { mapper.map(it) })
                }
                else -> GatewayResult(GatewayError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    data class GatewayPayload<T>(
        val result: T? = null
    ) : Payload<GatewayError>() {
        constructor(error: GatewayError) : this() {
            this.error = error
        }
    }

    data class GatewayResult<T>(val model: T? = null) : Store.OnChanged<GatewayError>() {
        constructor(error: GatewayError) : this() {
            this.error = error
        }
    }

    class GatewayError(
        var type: GatewayErrorType,
        var original: GenericErrorType,
        var message: String? = null
    ) : OnChangedError

    enum class GatewayErrorType {
        TIMEOUT,
        API_ERROR,
        INVALID_GATEWAY_ID,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        AUTHORIZATION_REQUIRED
    }
}

fun WPComGsonNetworkError.toGatewayError(): GatewayError {
    val type = when (type) {
        TIMEOUT -> GatewayErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> GatewayErrorType.API_ERROR
        PARSE_ERROR,
        CENSORED,
        INVALID_RESPONSE -> GatewayErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> GatewayErrorType.AUTHORIZATION_REQUIRED
        NOT_FOUND -> GatewayErrorType.INVALID_GATEWAY_ID
        UNKNOWN,
        null -> GENERIC_ERROR
    }
    return GatewayError(type, this.type, message)
}
