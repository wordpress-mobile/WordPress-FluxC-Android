package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.RefundsMapper
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient
import org.wordpress.android.fluxc.persistence.WCRefundsSqlUtils
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsError
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsErrorType
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundsErrorType.GENERIC_ERROR
import java.math.BigDecimal

@Singleton
class WCRefundsStore @Inject constructor(
    private val restClient: RefundsRestClient,
    private val coroutineContext: CoroutineContext,
    private val refundsMapper: RefundsMapper
) {
    suspend fun createRefund(
        site: SiteModel,
        orderId: Long,
        amount: BigDecimal,
        reason: String = "",
        autoRefund: Boolean = false
    ): RefundsResult<WCRefundModel> =
            withContext(coroutineContext) {
                val response = restClient.createRefund(site, orderId, amount.toString(), reason, autoRefund)
                return@withContext when {
                    response.isError -> RefundsResult(response.error)
                    response.result != null -> RefundsResult(refundsMapper.map(response.result))
                    else -> RefundsResult(RefundsError(GENERIC_ERROR, UNKNOWN))
                }
            }

    fun getRefund(site: SiteModel, orderId: Long, refundId: Long): WCRefundModel? {
        return WCRefundsSqlUtils.selectRefund(site, orderId, refundId)?.let { refundsMapper.map(it) }
    }

    suspend fun fetchRefund(site: SiteModel, orderId: Long, refundId: Long): RefundsResult<WCRefundModel> =
            withContext(coroutineContext) {
                val response = restClient.fetchRefund(site, orderId, refundId)
                return@withContext when {
                    response.isError -> RefundsResult(response.error)
                    response.result != null -> {
                        WCRefundsSqlUtils.insertOrUpdate(site, orderId, response.result)
                        RefundsResult(refundsMapper.map(response.result))
                    }
                    else -> RefundsResult(RefundsError(GENERIC_ERROR, UNKNOWN))
                }
            }

    fun getAllRefunds(site: SiteModel, orderId: Long): List<WCRefundModel> {
        return WCRefundsSqlUtils.selectAllRefunds(site, orderId).map { refundsMapper.map(it) }
    }

    suspend fun fetchAllRefunds(site: SiteModel, orderId: Long): RefundsResult<List<WCRefundModel>> =
            withContext(coroutineContext) {
                val response = restClient.fetchAllRefunds(site, orderId)
                return@withContext when {
                    response.isError -> {
                        RefundsResult(response.error)
                    }
                    response.result != null -> {
                        WCRefundsSqlUtils.insertOrUpdate(site, orderId, response.result.toList())
                        RefundsResult(response.result.map { refundsMapper.map(it) })
                    }
                    else -> RefundsResult(RefundsError(GENERIC_ERROR, UNKNOWN))
                }
            }

    data class RefundsPayload<T>(
        val result: T? = null
    ) : Payload<RefundsError>() {
        constructor(error: RefundsError) : this() {
            this.error = error
        }
    }

    data class RefundsResult<T>(val model: T? = null) : Store.OnChanged<RefundsError>() {
        constructor(error: RefundsError) : this() {
            this.error = error
        }
    }

    class RefundsError(
        var type: RefundsErrorType,
        var original: GenericErrorType,
        var message: String? = null
    ) : OnChangedError

    enum class RefundsErrorType {
        TIMEOUT,
        API_ERROR,
        INVALID_REFUND_ID,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        AUTHORIZATION_REQUIRED
    }
}

fun WPComGsonNetworkError.toRefundsError(): RefundsError {
    val type = when (type) {
        TIMEOUT -> RefundsErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> RefundsErrorType.API_ERROR
        PARSE_ERROR,
        CENSORED,
        INVALID_RESPONSE -> RefundsErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> RefundsErrorType.AUTHORIZATION_REQUIRED
        NOT_FOUND -> RefundsErrorType.INVALID_REFUND_ID
        UNKNOWN,
        null -> GENERIC_ERROR
    }
    return RefundsError(type, this.type, message)
}
