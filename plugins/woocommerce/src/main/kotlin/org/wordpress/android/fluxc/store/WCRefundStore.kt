package org.wordpress.android.fluxc.store

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.persistence.WCRefundSqlUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import java.math.BigDecimal

@Singleton
class WCRefundStore @Inject constructor(
    private val restClient: RefundRestClient,
    private val coroutineContext: CoroutineContext,
    private val refundsMapper: RefundMapper
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    suspend fun createAmountRefund(
        site: SiteModel,
        orderId: Long,
        amount: BigDecimal,
        reason: String = "",
        autoRefund: Boolean = false
    ): WooResult<WCRefundModel> {
        return withContext(coroutineContext) {
            val response = restClient.createRefundByAmount(
                    site,
                    orderId,
                    amount.toString(),
                    reason,
                    autoRefund
            )
            return@withContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(refundsMapper.map(response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun createItemsRefund(
        site: SiteModel,
        orderId: Long,
        reason: String = "",
        restockItems: Boolean = true,
        autoRefund: Boolean = false,
        items: List<WCRefundModel.WCRefundItem>
    ): WooResult<WCRefundModel> {
        return withContext(coroutineContext) {
            val response = restClient.createRefundByItems(
                    site,
                    orderId,
                    reason,
                    autoRefund,
                    items,
                    restockItems
            )
            return@withContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(refundsMapper.map(response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun getRefund(site: SiteModel, orderId: Long, refundId: Long): WCRefundModel? =
            WCRefundSqlUtils.selectRefund(site, orderId, refundId)?.let { refundsMapper.map(it) }

    suspend fun fetchRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): WooResult<WCRefundModel> {
        return withContext(coroutineContext) {
            val response = restClient.fetchRefund(site, orderId, refundId)
            return@withContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    WCRefundSqlUtils.insertOrUpdate(site, orderId, response.result)
                    WooResult(refundsMapper.map(response.result))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    fun getAllRefunds(site: SiteModel, orderId: Long): List<WCRefundModel> =
            WCRefundSqlUtils.selectAllRefunds(site, orderId).map { refundsMapper.map(it) }

    suspend fun fetchAllRefunds(
        site: SiteModel,
        orderId: Long,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<List<WCRefundModel>> {
        return withContext(coroutineContext) {
            val response = restClient.fetchAllRefunds(
                    site,
                    orderId,
                    page,
                    pageSize
            )
            return@withContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    WCRefundSqlUtils.insertOrUpdate(site, orderId, response.result.toList())
                    WooResult(response.result.map { refundsMapper.map(it) })
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
