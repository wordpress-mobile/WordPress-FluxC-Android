package org.wordpress.android.fluxc.network.rest.wpcom.wc.customer

import android.content.Context
import com.android.volley.RequestQueue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.INCLUDE_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.INCLUDE_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.NAME_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.REGISTERED_DATE_ASC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerSorting.REGISTERED_DATE_DESC
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import javax.inject.Singleton

@Singleton
class CustomerRestClient(
    appContext: Context,
    private val requestBuilder: JetpackTunnelGsonRequestBuilder,
    dispatcher: Dispatcher,
    requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    /**
     * Makes a GET call to `/wc/v3/customers/[remoteCustomerId]` to fetch a single customer
     *
     * @param [remoteCustomerId] Unique server id of the customer to fetch
     */
    suspend fun fetchSingleCustomer(site: SiteModel, remoteCustomerId: Long): WooPayload<CustomerApiResponse> {
        val url = WOOCOMMERCE.customers.id(remoteCustomerId).pathV3

        val response = requestBuilder.syncGetRequest(
                restClient = this,
                site = site,
                url = url,
                params = emptyMap(),
                clazz = CustomerApiResponse::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * Makes a GET call to `/wc/v3/customers/` to fetch customers
     *
     */
    suspend fun fetchCustomers(
        site: SiteModel,
        pageSize: Int,
        sortType: CustomerSorting = NAME_ASC,
        offset: Int = 0,
        searchQuery: String? = null,
        email: String? = null,
        role: String? = null,
        context: String? = null,
        remoteCustomerIds: List<Long>? = null,
        excludedCustomerIds: List<Long>? = null
    ): WooPayload<Array<CustomerApiResponse>> {
        val url = WOOCOMMERCE.customers.pathV3

        val orderBy = when (sortType) {
            NAME_ASC, NAME_DESC -> "name"
            INCLUDE_ASC, INCLUDE_DESC -> "include"
            REGISTERED_DATE_ASC, REGISTERED_DATE_DESC -> "registered_date"
        }
        val sortOrder = when (sortType) {
            NAME_ASC, INCLUDE_ASC, REGISTERED_DATE_ASC -> "asc"
            INCLUDE_DESC, NAME_DESC, REGISTERED_DATE_DESC -> "desc"
        }

        val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "orderby" to orderBy,
                "order" to sortOrder,
                "offset" to offset.toString()
        ).run {
            putIfNotEmpty("search" to searchQuery)
            putIfNotEmpty("email" to email)
            putIfNotEmpty("role" to role)
            putIfNotEmpty("context" to context)
        }

        remoteCustomerIds?.let { ids ->
            params.put("include", ids.map { it }.joinToString())
        }

        excludedCustomerIds?.let { excludedIds ->
            params.put("exclude", excludedIds.map { it }.joinToString())
        }

        val response = requestBuilder.syncGetRequest(
                restClient = this,
                site = site,
                url = url,
                params = params,
                Array<CustomerApiResponse>::class.java
        )

        return when (response) {
            is JetpackSuccess -> WooPayload(response.data)
            is JetpackError -> WooPayload(response.error.toWooError())
        }
    }
}
