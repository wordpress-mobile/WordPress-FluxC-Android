package org.wordpress.android.fluxc.example.ui.customer.search

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.customer.WCCustomerListDescriptor
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.WCCustomerStore
import javax.inject.Inject

class WooCustomersListItemDataSource
@Inject
constructor(val store: WCCustomerStore) : ListItemDataSourceInterface<WCCustomerListDescriptor, Long, CustomerListItemType> {
    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCCustomerListDescriptor,
        itemIdentifiers: List<Long>
    ): List<CustomerListItemType> {
        TODO("Not yet implemented")
    }

    override fun getItemIdentifiers(
        listDescriptor: WCCustomerListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<Long> {
        TODO("Not yet implemented")
    }

    override fun fetchList(listDescriptor: WCCustomerListDescriptor, offset: Long) {
//        store.fetchCustomers(
//                site = listDescriptor.site,
//                searchQuery = listDescriptor.searchQuery,
//                email = listDescriptor.email,
//                role = listDescriptor.role,
//                remoteCustomerIds = listDescriptor.remoteCustomerIds,
//                excludedCustomerIds = listDescriptor.excludedCustomerIds
//        )
    }
}
