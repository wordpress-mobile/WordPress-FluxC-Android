package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundsMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundsRestClient.RefundResponse
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
//                if (!forced && sqlUtils.hasFreshRequest(siteModel, limitMode.limit)) {
//                    return@withContext OnStatsFetched(getTags(siteModel, limitMode), cached = true)
//                }
                val response = restClient.createRefund(site, orderId, amount.toString())
                return@withContext when {
                    response.isError -> {
                        RefundResult(response.error)
                    }
                    response.result != null -> {
//                        sqlUtils.insert(siteModel, response.response, requestedItems = limitMode.limit)
                        RefundResult(
                                refundsMapper.map(response.result)
                        )
                    }
                    else -> RefundResult(RefundsError(GENERIC_ERROR, UNKNOWN))
                }
            }

//    fun getTags(site: SiteModel, cacheMode: LimitMode): TagsModel? {
//        return sqlUtils.select(site)?.let { insightsMapper.map(it, cacheMode) }
//    }

    suspend fun fetchRefund(site: SiteModel, orderId: Long, refundId: Long) =
        withContext(coroutineContext) {
            //                if (!forced && sqlUtils.hasFreshRequest(siteModel, limitMode.limit)) {
//                    return@withContext OnStatsFetched(getTags(siteModel, limitMode), cached = true)
//                }
            val response = restClient.fetchRefund(site, orderId, refundId)
            return@withContext when {
                response.isError -> {
                    RefundResult(response.error)
                }
                response.result != null -> {
//                        sqlUtils.insert(siteModel, response.response, requestedItems = limitMode.limit)
                    RefundResult(
                            refundsMapper.map(response.result)
                    )
                }
                else -> RefundResult(RefundsError(GENERIC_ERROR, UNKNOWN))
            }
        }

    suspend fun fetchAllRefund(site: SiteModel, orderId: Long) =
            withContext(coroutineContext) {
                //                if (!forced && sqlUtils.hasFreshRequest(siteModel, limitMode.limit)) {
//                    return@withContext OnStatsFetched(getTags(siteModel, limitMode), cached = true)
//                }
                val response = restClient.fetchAllRefunds(site, orderId)
                return@withContext when {
                    response.isError -> {
                        RefundResult(response.error)
                    }
                    response.result != null -> {
//                        sqlUtils.insert(siteModel, response.response, requestedItems = limitMode.limit)
                        RefundResult(
                                response.result.map { refundsMapper.map(it) }
                        )
                    }
                    else -> RefundResult(RefundsError(GENERIC_ERROR, UNKNOWN))
                }
            }

    data class RefundsPayload<T>(
        val result: T? = null
    ) : Payload<RefundsError>() {
        constructor(error: RefundsError) : this() {
            this.error = error
        }
    }

    data class RefundResult<T>(val model: T? = null, val cached: Boolean = false) : Store.OnChanged<RefundsError>() {
        constructor(error: RefundsError) : this() {
            this.error = error
        }
    }

    class RefundsError(var type: RefundsErrorType, var original: GenericErrorType, var message: String? = null) : OnChangedError

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
