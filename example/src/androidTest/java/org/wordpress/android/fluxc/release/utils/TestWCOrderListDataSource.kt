package org.wordpress.android.fluxc.release.utils

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload

internal class TestWCOrderUIItem

internal class TestWCOrderListDataSource(
    val dispatcher: Dispatcher
) : ListItemDataSourceInterface<WCOrderListDescriptor, RemoteId, TestWCOrderUIItem> {
    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCOrderListDescriptor,
        itemIdentifiers: List<RemoteId>
    ): List<TestWCOrderUIItem> = itemIdentifiers.map { TestWCOrderUIItem() }

    override fun getItemIdentifiers(
        listDescriptor: WCOrderListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<RemoteId> = remoteItemIds

    override fun fetchList(listDescriptor: WCOrderListDescriptor, offset: Long) {
        val fetchOrderListPayload = FetchOrderListPayload(listDescriptor, offset)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderListAction(fetchOrderListPayload))
    }
}
