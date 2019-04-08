package org.wordpress.android.fluxc.release.utils

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload

internal class TestPostUIItem

internal class TestPostListDataSource(
    val dispatcher: Dispatcher
) : ListItemDataSourceInterface<PostListDescriptor, RemoteId, TestPostUIItem> {
    override fun getItemsAndFetchIfNecessary(
        listDescriptor: PostListDescriptor,
        itemIdentifiers: List<RemoteId>
    ): List<TestPostUIItem> = itemIdentifiers.map { TestPostUIItem() }

    override fun getItemIdentifiers(
        listDescriptor: PostListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<RemoteId> = remoteItemIds

    override fun fetchList(listDescriptor: PostListDescriptor, offset: Long) {
        val fetchPostListPayload = FetchPostListPayload(listDescriptor, offset)
        dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
    }
}
