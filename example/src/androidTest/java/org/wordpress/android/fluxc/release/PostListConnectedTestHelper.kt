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
    internal enum class TestEvent {
        NONE,
        FETCHED_FIRST_PAGE,
        LIST_STATE_CHANGED,
        LOADED_MORE
    }

    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore

    private var nextEvent: TestEvent = TestEvent.NONE
    private lateinit var countDownLatch: CountDownLatch

    init {
        releaseStackComponent.inject(this)
        dispatcher.register(this)
    }

    @Throws(InterruptedException::class)
    internal fun fetchFirstPageHelper(postListDescriptor: PostListDescriptor) {
        val dataSource = listItemDataSource { listDescriptor, offset ->
            assertEquals("First event should be for state change", TestEvent.LIST_STATE_CHANGED, nextEvent)
            assertEquals("List should be fetched for the correct ListDescriptor", postListDescriptor, listDescriptor)
            // We will do an actual fetch now, update the event so OnListChanged can check for it
            nextEvent = TestEvent.FETCHED_FIRST_PAGE
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

        // The first expected event is the list change
        nextEvent = TestEvent.LIST_STATE_CHANGED
        // 2 events should happen in total:
        // First event will be for list state change and the second will be for completed fetch
        countDownLatch = CountDownLatch(2)
        // Call `refresh` on the ListManager which should trigger the state change and then fetch the list
        listManagerBefore.refresh()
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(
                "Assert that the expected event is updated within `ListItemDataSource.fetchList`",
                TestEvent.FETCHED_FIRST_PAGE,
                nextEvent
        )
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
        var fetchedFirstPage = false
        val dataSource = listItemDataSource { listDescriptor, offset ->
            assertEquals("First event should be for state change", TestEvent.LIST_STATE_CHANGED, nextEvent)
            assertEquals("List should be fetched for the correct ListDescriptor", postListDescriptor, listDescriptor)
            // Set the expected event depending on which fetch this is
            nextEvent = if (fetchedFirstPage) TestEvent.LOADED_MORE else TestEvent.FETCHED_FIRST_PAGE
            val fetchPostListPayload = FetchPostListPayload(postListDescriptor, offset)
            dispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
        // Fetch the first page and get the current ListManager.
        val listManagerBefore = fetchFirstPageAndAssert(postListDescriptor, dataSource)
        fetchedFirstPage = true
        assertTrue(
                "This test requires the site to have at least 2 pages of data and should return canLoadMore = true" +
                        " after the first fetch",
                listManagerBefore.canLoadMore
        )

        // The first expected event is the list change
        nextEvent = TestEvent.LIST_STATE_CHANGED
        // 2 events should happen in total:
        // First event will be for list state change and the second will be for completed fetch
        countDownLatch = CountDownLatch(2)
        // Requesting the last item in `ListManager` will trigger a load more if there is more data to be loaded
        listManagerBefore.getItem(listManagerBefore.size - 1, shouldLoadMoreIfNecessary = true)
        assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        assertEquals(
                "Assert that the expected event is updated within `ListItemDataSource.fetchList`",
                TestEvent.LOADED_MORE,
                nextEvent
        )
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
        when {
            event.causeOfChange == FIRST_PAGE_FETCHED -> assertEquals(
                    "Expected event was fetching the first page",
                    TestEvent.FETCHED_FIRST_PAGE,
                    nextEvent
            )
            event.causeOfChange == LOADED_MORE -> assertEquals(
                    "Expected event was the loading more pages",
                    TestEvent.LOADED_MORE,
                    nextEvent
            )
            else -> {
                assertEquals("Expected event was state change", TestEvent.LIST_STATE_CHANGED, nextEvent)
            }
        }
        countDownLatch.countDown()
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
