package org.wordpress.android.fluxc.release.utils

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataStoreInterface
import org.wordpress.android.fluxc.model.list.datastore.PostListDataStoreHelper
import org.wordpress.android.fluxc.store.PostStore

internal class TestPostUIItem

internal class TestPostListDataStore(
    dispatcher: Dispatcher,
    postStore: PostStore
) : ListItemDataStoreInterface<PostListDescriptor, RemoteId, TestPostUIItem> {
    private val postListDataStoreHelper = PostListDataStoreHelper(dispatcher, postStore)

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
        postListDataStoreHelper.fetchList(listDescriptor, offset)
    }
}
