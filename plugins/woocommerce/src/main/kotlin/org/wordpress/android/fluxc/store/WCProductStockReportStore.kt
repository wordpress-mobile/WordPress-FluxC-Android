package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ProductStockItemApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.reports.ReportsRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCProductStockReportStore @Inject constructor(
    private val reportsRestClient: ReportsRestClient,
    private val coroutineEngine: CoroutineEngine,
) {
    companion object {
        const val DEFAULT_PAGE_SIZE = 3
        const val DEFAULT_PAGE = 1
    }

    suspend fun fetchProductStockReport(
        site: SiteModel,
        stockStatus: CoreProductStockStatus,
        page: Int = DEFAULT_PAGE,
        quantity: Int = DEFAULT_PAGE_SIZE
    ): WooResult<ProductStockItems> {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductStockReport") {
            val response = reportsRestClient.fetchProductStockReport(
                site = site,
                stockStatus = stockStatus.value,
                page = page,
                quantity = quantity
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(response.result)
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}

typealias ProductStockItems = Array<ProductStockItemApiResponse>
