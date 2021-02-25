package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.persistence.CustomerSqlUtils
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCCustomerStore @Inject constructor(
    private val restClient: CustomerRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val mapper: WCCustomerMapper
) {
    /**
     * return a cached customer with provided remote id or null if not in cache
     */
    fun getCustomerByRemoteId(site: SiteModel, remoteCustomerId: Long) =
            CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)

    /**
     * returns a customer with provided remote id
     */
    suspend fun fetchSingleCustomer(site: SiteModel, remoteCustomerId: Long): WooResult<WCCustomerModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchSingleCustomer") {
            val response = restClient.fetchSingleCustomer(site, remoteCustomerId)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val customer = mapper.map(site, response.result)
                    CustomerSqlUtils.insertOrUpdateCustomer(customer)
                    WooResult(customer)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
