package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_ASC
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
     * returns cached customers for the given site
     */
    fun getCustomersForSite(site: SiteModel) = CustomerSqlUtils.getCustomersForSite(site)

    /**
     * returns a cached customer with provided remote id or null if not in cache
     */
    fun getCustomerByRemoteId(site: SiteModel, remoteCustomerId: Long) =
            CustomerSqlUtils.getCustomerByRemoteId(site, remoteCustomerId)

    /**
     * returns a customer with provided remote id
     */
    suspend fun fetchSingleCustomer(site: SiteModel, remoteCustomerId: Long): WooResult<WCCustomerModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchSingleCustomer") {
            val response = restClient.fetchSingleCustomer(site, remoteCustomerId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val customer = mapper.mapToModel(site, response.result)
                    CustomerSqlUtils.insertOrUpdateCustomer(customer)
                    WooResult(customer)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * returns customers from the backend
     */
    suspend fun fetchCustomers(
        site: SiteModel,
        pageSize: Int = DEFAULT_CUSTOMER_PAGE_SIZE,
        offset: Int = 0,
        sortType: CustomerSorting = DEFAULT_CUSTOMER_SORTING,
        searchQuery: String? = null,
        email: String? = null,
        role: String? = null,
        context: String? = null,
        remoteCustomerIds: List<Long>? = null,
        excludedCustomerIds: List<Long>? = null
    ): WooResult<List<WCCustomerModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchCustomers") {
            val response = restClient.fetchCustomers(
                    site = site,
                    pageSize = pageSize,
                    sortType = sortType,
                    offset = offset,
                    searchQuery = searchQuery,
                    email = email,
                    role = role,
                    context = context,
                    remoteCustomerIds = remoteCustomerIds,
                    excludedCustomerIds = excludedCustomerIds
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val isCachingNeeded = searchQuery == null &&
                            email == null &&
                            role == null &&
                            remoteCustomerIds.isNullOrEmpty() &&
                            excludedCustomerIds.isNullOrEmpty()
                    val customers = response.result.map { mapper.mapToModel(site, it) }
                    if (isCachingNeeded) {
                        // clear cache if it's the first page for the site
                        if (offset == 0) CustomerSqlUtils.deleteCustomersForSite(site)
                        CustomerSqlUtils.insertOrUpdateCustomers(customers)
                    }
                    WooResult(customers)
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * creates a customer on the backend
     */
    suspend fun createCustomer(site: SiteModel, customer: WCCustomerModel): WooResult<WCCustomerModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createCustomer") {
            val response = restClient.createCustomer(site, mapper.mapToDTO(customer))
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(mapper.mapToModel(site, response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    companion object {
        private const val DEFAULT_CUSTOMER_PAGE_SIZE = 25
        private val DEFAULT_CUSTOMER_SORTING = NAME_ASC
    }
}
