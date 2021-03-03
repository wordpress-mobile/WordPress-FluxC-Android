package org.wordpress.android.fluxc.example.ui.customer.search

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerListDescriptor
import org.wordpress.android.fluxc.model.list.ListConfig

class SearchCustomerListDescriptor(
    customerSite: SiteModel,
    customerSearchQuery: String? = null,
    customerEmail: String? = null,
    customerRole: String? = null,
    customerRemoteCustomerIds: List<Long>? = null,
    customerExcludedCustomerIds: List<Long>? = null
) : WCCustomerListDescriptor(
        customerSite,
        customerSearchQuery,
        customerEmail,
        customerRole,
        customerRemoteCustomerIds,
        customerExcludedCustomerIds
) {
    override val config = ListConfig(
            networkPageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE,
            dbPageSize = PAGE_SIZE,
            prefetchDistance = 3
    )
}
