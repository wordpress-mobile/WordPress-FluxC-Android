package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundModel
import org.wordpress.android.fluxc.model.refunds.RefundsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient
import org.wordpress.android.fluxc.persistence.RefundsSqlUtils
import org.wordpress.android.fluxc.store.RefundsStore.RefundsError
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import org.wordpress.android.fluxc.store.RefundsStore.RefundsErrorType.GENERIC_ERROR
import java.math.BigDecimal

@Singleton
class RefundsStore @Inject constructor(
    private val restClient: RefundsRestClient,
    private val coroutineContext: CoroutineContext,
    private val refundsMapper: RefundsMapper
) {
    suspend fun createRefund(site: SiteModel, orderId: Long, amount: BigDecimal) =
            withContext(coroutineContext) {
                val response = restClient.createRefund(site, orderId, amount.toString())
                return@withContext when {
                    response.isError -> RefundsResult(response.error)
                    response.result != null -> RefundsResult(refundsMapper.map(response.result))
                    else -> RefundsResult(RefundsError(GENERIC_ERROR, UNKNOWN))
                }
            }

    fun getRefund(site: SiteModel, orderId: Long, refundId: Long): RefundModel? {
        return RefundsSqlUtils.selectRefund(site, orderId, refundId)?.let { refundsMapper.map(it) }
    }

    suspend fun fetchRefund(site: SiteModel, orderId: Long, refundId: Long) =
        withContext(coroutineContext) {
            val response = restClient.fetchRefund(site, orderId, refundId)
            return@withContext when {
                response.isError -> RefundsResult(response.error)
                response.result != null -> {
                    RefundsSqlUtils.insert(site, orderId, response.result)
                    RefundsResult(refundsMapper.map(response.result))
                }
                else -> RefundsResult(RefundsError(GENERIC_ERROR, UNKNOWN))
            }
        }

    fun getAllRefunds(site: SiteModel, orderId: Long): List<RefundModel> {
        return RefundsSqlUtils.selectAllRefunds(site, orderId).map { refundsMapper.map(it) }
    }

    suspend fun fetchAllRefunds(site: SiteModel, orderId: Long) =
            withContext(coroutineContext) {
                val response = restClient.fetchAllRefunds(site, orderId)
                return@withContext when {
                    response.isError -> {
                        RefundsResult(response.error)
                    }
                    response.result != null -> {
                        RefundsSqlUtils.insert(site, orderId, response.result.toList())
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
        INVALID_PARAM,
        INVALID_ID,
        ORDER_STATUS_NOT_FOUND,
        PLUGIN_NOT_ACTIVE,
        GENERIC_ERROR
    }
}

fun WPComGsonNetworkError.toRefundsError(): RefundsError {
    val type = when (type) {
        GenericErrorType.TIMEOUT,
        GenericErrorType.NO_CONNECTION,
        GenericErrorType.INVALID_SSL_CERTIFICATE,
        GenericErrorType.NETWORK_ERROR,
        GenericErrorType.PARSE_ERROR,
        GenericErrorType.NOT_FOUND,
        GenericErrorType.CENSORED,
        GenericErrorType.INVALID_RESPONSE,
        GenericErrorType.HTTP_AUTH_ERROR,
        GenericErrorType.AUTHORIZATION_REQUIRED,
        GenericErrorType.NOT_AUTHENTICATED,
        GenericErrorType.UNKNOWN,
        GenericErrorType.SERVER_ERROR,
        null -> GENERIC_ERROR
    }
    return RefundsError(type, this.type, message)
}
