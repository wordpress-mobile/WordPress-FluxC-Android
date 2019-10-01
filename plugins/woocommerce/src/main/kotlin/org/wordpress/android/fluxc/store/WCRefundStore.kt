package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.persistence.WCRefundSqlUtils
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundError
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundErrorType
import org.wordpress.android.fluxc.store.Store.OnChangedError
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import org.wordpress.android.fluxc.store.WCRefundsStore.RefundErrorType.GENERIC_ERROR
import java.math.BigDecimal

@Singleton
class WCRefundsStore @Inject constructor(
    private val restClient: RefundRestClient,
    private val coroutineContext: CoroutineContext,
    private val refundsMapper: RefundMapper
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    suspend fun createRefund(
        site: SiteModel,
        orderId: Long,
        amount: BigDecimal,
        reason: String = "",
        autoRefund: Boolean = false
    ): RefundResult<WCRefundModel> =
            withContext(coroutineContext) {
                val response = restClient.createRefund(
                        site,
                        orderId,
                        amount.toString(),
                        reason,
                        autoRefund
                )
                return@withContext when {
                    response.isError -> RefundResult(response.error)
                    response.result != null -> RefundResult(refundsMapper.map(response.result))
                    else -> RefundResult(RefundError(GENERIC_ERROR, UNKNOWN))
                }
            }

    fun getRefund(site: SiteModel, orderId: Long, refundId: Long): WCRefundModel? {
        return WCRefundSqlUtils.selectRefund(site, orderId, refundId)
                ?.let { refundsMapper.map(it) }
    }

    suspend fun fetchRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): RefundResult<WCRefundModel> =
            withContext(coroutineContext) {
                val response = restClient.fetchRefund(site, orderId, refundId)
                return@withContext when {
                    response.isError -> RefundResult(response.error)
                    response.result != null -> {
                        WCRefundSqlUtils.insertOrUpdate(site, orderId, response.result)
                        RefundResult(refundsMapper.map(response.result))
                    }
                    else -> RefundResult(RefundError(GENERIC_ERROR, UNKNOWN))
                }
            }

    fun getAllRefunds(site: SiteModel, orderId: Long): List<WCRefundModel> {
        return WCRefundSqlUtils.selectAllRefunds(site, orderId).map { refundsMapper.map(it) }
    }

    suspend fun fetchAllRefunds(
        site: SiteModel,
        orderId: Long,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): RefundResult<List<WCRefundModel>> =
            withContext(coroutineContext) {
                val response = restClient.fetchAllRefunds(
                        site,
                        orderId,
                        page,
                        pageSize
                )
                return@withContext when {
                    response.isError -> {
                        RefundResult(response.error)
                    }
                    response.result != null -> {
                        WCRefundSqlUtils.insertOrUpdate(site, orderId, response.result.toList())
                        RefundResult(response.result.map { refundsMapper.map(it) })
                    }
                    else -> RefundResult(RefundError(GENERIC_ERROR, UNKNOWN))
                }
            }

    data class RefundPayload<T>(
        val result: T? = null
    ) : Payload<RefundError>() {
        constructor(error: RefundError) : this() {
            this.error = error
        }
    }

    data class RefundResult<T>(val model: T? = null) : Store.OnChanged<RefundError>() {
        constructor(error: RefundError) : this() {
            this.error = error
        }
    }

    class RefundError(
        var type: RefundErrorType,
        var original: GenericErrorType,
        var message: String? = null
    ) : OnChangedError

    enum class RefundErrorType {
        TIMEOUT,
        API_ERROR,
        INVALID_REFUND_ID,
        GENERIC_ERROR,
        INVALID_RESPONSE,
        AUTHORIZATION_REQUIRED
    }
}

fun WPComGsonNetworkError.toRefundError(): RefundError {
    val type = when (type) {
        TIMEOUT -> RefundErrorType.TIMEOUT
        NO_CONNECTION,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        NETWORK_ERROR -> RefundErrorType.API_ERROR
        PARSE_ERROR,
        CENSORED,
        INVALID_RESPONSE -> RefundErrorType.INVALID_RESPONSE
        HTTP_AUTH_ERROR,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED -> RefundErrorType.AUTHORIZATION_REQUIRED
        NOT_FOUND -> RefundErrorType.INVALID_REFUND_ID
        UNKNOWN,
        null -> GENERIC_ERROR
    }
    return RefundError(type, this.type, message)
}
