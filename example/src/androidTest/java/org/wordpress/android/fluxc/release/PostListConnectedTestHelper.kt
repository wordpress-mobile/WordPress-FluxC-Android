package org.wordpress.android.fluxc.release

import kotlinx.coroutines.experimental.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange.FIRST_PAGE_FETCHED
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange.LOADED_MORE
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PostListConnectedTestHelper(
    private val dispatcher: Dispatcher,
    releaseStackComponent: ReleaseStack_AppComponent
) {
    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore

    private lateinit var countDownLatch: CountDownLatch

    init {
        releaseStackComponent.inject(this)
        dispatcher.register(this)
    }

    @Throws(InterruptedException::class)
    internal fun fetchFirstPageHelper(postListDescriptor: PostListDescriptor) {
        val dataSource = listItemDataSource { listDescriptor, offset ->
            assertEquals("List should be fetched for the correct ListDescriptor", postListDescriptor, listDescriptor)
            // Fetch the post list for the given ListDescriptor and offset
            val fetchPostListPayload = FetchPostListPayload(postListDescriptor, offset)
            dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
        fetchFirstPageAndAssert(postListDescriptor, dataSource)
    }

    private fun fetchFirstPageAndAssert(
        postListDescriptor: PostListDescriptor,
        dataSource: ListItemDataSource<PostModel>
    ): ListManager<PostModel> {
        // Get the initial ListManager from ListStore and assert that everything is as expected
        val listManagerBefore = runBlocking {
            listStore.getListManager(postListDescriptor, dataSource)
        }
        assertEquals("List should be empty at first", 0, listManagerBefore.size)
        assertFalse("List shouldn't be fetching first page initially", listManagerBefore.isFetchingFirstPage)
        assertFalse("List shouldn't be loading more data initially", listManagerBefore.isLoadingMore)

        countDownLatch = CountDownLatch(1)
        // Call `refresh` on the ListManager which should trigger the state change and then fetch the list
        listManagerBefore.refresh()
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Retrieve the updated ListManager from ListStore and assert that we have data and the state is as expected
        val listManagerAfter = runBlocking {
            listStore.getListManager(postListDescriptor, dataSource)
        }
        assertFalse("List shouldn't be empty after fetch", listManagerAfter.size == 0)
        assertFalse("List shouldn't be fetching first page anymore", listManagerAfter.isFetchingFirstPage)
        assertFalse("List shouldn't be loading more data anymore", listManagerAfter.isLoadingMore)
        return listManagerAfter
    }

    @Throws(InterruptedException::class)
    internal fun loadMoreHelper(postListDescriptor: PostListDescriptor) {
        val dataSource = listItemDataSource { listDescriptor, offset ->
            assertEquals("List should be fetched for the correct ListDescriptor", postListDescriptor, listDescriptor)
            val fetchPostListPayload = FetchPostListPayload(postListDescriptor, offset)
            dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
        // Fetch the first page and get the current ListManager.
        val listManagerBefore = fetchFirstPageAndAssert(postListDescriptor, dataSource)
        assertTrue(
                "This test requires the site to have at least 2 pages of data and should return canLoadMore = true" +
                        " after the first fetch",
                listManagerBefore.canLoadMore
        )

        countDownLatch = CountDownLatch(1)
        // Requesting the last item in `ListManager` will trigger a load more if there is more data to be loaded
        listManagerBefore.getItem(listManagerBefore.size - 1, shouldLoadMoreIfNecessary = true)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Retrieve the updated ListManager from ListStore and assert that we have more data than before
        val listManagerAfter = runBlocking {
            listStore.getListManager(postListDescriptor, dataSource)
        }
        assertTrue("More data should be loaded after loadMore", listManagerAfter.size > listManagerBefore.size)
        assertFalse("List shouldn't be fetching first page anymore", listManagerAfter.isFetchingFirstPage)
        assertFalse("List shouldn't be loading more data anymore", listManagerAfter.isLoadingMore)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListChanged(event: OnListChanged) {
        event.error?.let {
            throw AssertionError("OnListChanged has error: " + it.type)
        }
        if (event.causeOfChange == FIRST_PAGE_FETCHED || event.causeOfChange == LOADED_MORE) {
            countDownLatch.countDown()
        }
    }

    private fun listItemDataSource(fetchList: (ListDescriptor, Int) -> Unit): ListItemDataSource<PostModel> =
            object : ListItemDataSource<PostModel> {
                override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
                }

                override fun fetchList(listDescriptor: ListDescriptor, offset: Int) {
                    fetchList(listDescriptor, offset)
                }

                override fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, PostModel> {
                    return emptyMap()
                }
            }
}
