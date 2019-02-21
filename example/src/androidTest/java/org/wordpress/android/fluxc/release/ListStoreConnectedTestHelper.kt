package org.wordpress.android.fluxc.release

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.datastore.ListDataStoreInterface
import org.wordpress.android.fluxc.release.IsEmptyValue.EMPTY
import org.wordpress.android.fluxc.release.IsEmptyValue.NOT_EMPTY
import org.wordpress.android.fluxc.release.IsFetchingFirstPageValue.FETCHING_FIRST_PAGE
import org.wordpress.android.fluxc.release.IsFetchingFirstPageValue.NOT_FETCHING_FIRST_PAGE
import org.wordpress.android.fluxc.release.IsLoadingMoreValue.LOADING_MORE
import org.wordpress.android.fluxc.release.IsLoadingMoreValue.NOT_LOADING_MORE
import org.wordpress.android.fluxc.store.ListStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val TEST_LIST_NETWORK_PAGE_SIZE = 5
private const val TEST_LIST_INITIAL_LOAD_SIZE = 10
private const val TEST_LIST_DB_PAGE_SIZE = 5
private const val TEST_LIST_PRE_FETCH_DISTANCE = TEST_LIST_DB_PAGE_SIZE * 3

/**
 * A [ListConfig] instance that has smaller values then the default config to make testing lists less demanding.
 */
internal val TEST_LIST_CONFIG = ListConfig(
        networkPageSize = TEST_LIST_NETWORK_PAGE_SIZE,
        initialLoadSize = TEST_LIST_INITIAL_LOAD_SIZE,
        dbPageSize = TEST_LIST_DB_PAGE_SIZE,
        prefetchDistance = TEST_LIST_PRE_FETCH_DISTANCE
)

/**
 * A helper class that makes writing connected tests for [ListStore] dead easy. It provides helpers for testing fetching
 * first page and loading more data. It also makes it very easy to update/improve every connected test for [ListStore]
 * from a single place.
 */
internal class ListStoreConnectedTestHelper(private val listStore: ListStore) {
    /**
     * A helper function that returns the list from [ListStore] for the given [ListDescriptor] and
     * [ListDataStoreInterface].
     *
     * It provides a default [Lifecycle] instance and skips the `transform` function to make it easier to write tests.
     * The default [Lifecycle] instance will NOT be destroyed throughout the test.
     *
     * @param listDescriptor [ListDescriptor] instance to be used to retrieve the list
     * @param dataStore [ListDataStoreInterface] instance to be used to retrieve the list
     * @param lifecycle [Lifecycle] instance to be used to retrieve the list.
     */
    fun <T> getList(
        listDescriptor: ListDescriptor,
        dataStore: ListDataStoreInterface<T>,
        lifecycle: Lifecycle = SimpleTestLifecycle().lifecycle
    ): PagedListWrapper<T> {
        return listStore.getList(
                listDescriptor = listDescriptor,
                dataStore = dataStore,
                lifecycle = lifecycle,
                transform = { x -> x }
        )
    }

    /**
     * A helper function that fetches the first page of a list and asserts that correct values are observed for the
     * given [PagedListWrapper].
     *
     * This function takes a [PagedListWrapper] generator instead of a [PagedListWrapper] instance to discourage any
     * attempt of setting up the given [PagedListWrapper] and convey the intend that the setup will be performed
     * by this helper instead.
     *
     * @param createPagedListWrapper A factory function that will create a [PagedListWrapper] instance.
     */
    fun <T> testFetchFirstPage(createPagedListWrapper: () -> PagedListWrapper<T>) {
        fetchFirstPageAndAssert(createPagedListWrapper())
    }

    /**
     * A helper function that initially fetches the first page of a list and asserts that correct values are observed
     * for the given [PagedListWrapper]. This is done so that it can then trigger loading more data to observe that
     * all values are changed correctly.
     *
     * This function takes a [PagedListWrapper] generator instead of a [PagedListWrapper] instance to discourage any
     * attempt of setting up the given [PagedListWrapper] and convey the intend that the setup will be performed
     * by this helper instead.
     *
     * @param createPagedListWrapper A factory function that will create a [PagedListWrapper] instance.
     */
    fun <T> testLoadMore(createPagedListWrapper: () -> PagedListWrapper<T>) = runWithSimpleLifecycle { lifecycle ->
        val pagedListWrapper = createPagedListWrapper()
        fetchFirstPageAndAssert(pagedListWrapper)
        pagedListWrapper.testObservedDistinctValues(
                lifecycle = lifecycle,
                expectedIsEmptyValues = listOf(NOT_EMPTY),
                expectedIsFetchingFirstPageValues = listOf(NOT_FETCHING_FIRST_PAGE),
                expectedIsLoadingMoreValues = listOf(NOT_LOADING_MORE, LOADING_MORE, NOT_LOADING_MORE)
        ) {
            pagedListWrapper.triggerLoadMore()
        }
    }

    /**
     * An internal helper function that fetches the first page of a list and asserts that correct values are observed
     * for the given [PagedListWrapper].
     *
     * @param pagedListWrapper [PagedListWrapper] instance that will be tested
     */
    private fun <T> fetchFirstPageAndAssert(pagedListWrapper: PagedListWrapper<T>) =
            runWithSimpleLifecycle { lifecycle ->
                pagedListWrapper.testObservedDistinctValues(
                        lifecycle = lifecycle,
                        expectedIsEmptyValues = listOf(EMPTY, NOT_EMPTY),
                        // TODO: Initial value should be NOT_FETCHING_FIRST_PAGE, but this is not posted by
                        // TODO: `PagedListWrapper`
                        expectedIsFetchingFirstPageValues = listOf(FETCHING_FIRST_PAGE, NOT_FETCHING_FIRST_PAGE),
                        expectedIsLoadingMoreValues = listOf(NOT_LOADING_MORE)
                ) {
                    pagedListWrapper.fetchFirstPage()
                }
            }
}

/**
 * A helper function to run a block using a [SimpleTestLifecycle] which will be destroyed automatically once the
 * block is completed.
 */
private fun runWithSimpleLifecycle(
    lifecycle: SimpleTestLifecycle = SimpleTestLifecycle(),
    runTest: (LifecycleOwner) -> Unit
) {
    runTest(lifecycle)
    lifecycle.destroy()
}

/**
 * A simple interface to make testing observed values easier.
 */
private interface ObservedValue<T> {
    val value: T
}

/**
 * A simple [Boolean] wrapper to represent [PagedListWrapper.isEmpty] values.
 *
 * We should't directly use listOf(false, true, false) values, because it doesn't carry intend. This wrapper makes our
 * tests more meaningful and less error prone.
 */
private enum class IsEmptyValue(override val value: Boolean) : ObservedValue<Boolean> {
    EMPTY(true),
    NOT_EMPTY(false);
}

/**
 * A simple [Boolean] wrapper to represent [PagedListWrapper.isFetchingFirstPage] values.
 *
 * We should't directly use listOf(false, true, false) values, because it doesn't carry intend. This wrapper makes our
 * tests more meaningful and less error prone.
 */
private enum class IsFetchingFirstPageValue(override val value: Boolean) : ObservedValue<Boolean> {
    FETCHING_FIRST_PAGE(true),
    NOT_FETCHING_FIRST_PAGE(false);
}

/**
 * A simple [Boolean] wrapper to represent [PagedListWrapper.isLoadingMore] values.
 *
 * We should't directly use listOf(false, true, false) values, because it doesn't carry intend. This wrapper makes our
 * tests more meaningful and less error prone.
 */
private enum class IsLoadingMoreValue(override val value: Boolean) : ObservedValue<Boolean> {
    LOADING_MORE(true),
    NOT_LOADING_MORE(false);
}

/**
 * A helper function that compares the observed values with the expected ones for the given [PagedListWrapper] until
 * the given lists run out.
 * @param lifecycle [LifecycleOwner] the [LiveData] instances will be observed on.
 * @param expectedIsEmptyValues List of expected [PagedListWrapper.isEmpty] values
 * @param expectedIsFetchingFirstPageValues List of expected [PagedListWrapper.isFetchingFirstPage] values
 * @param expectedIsLoadingMoreValues List of expected [PagedListWrapper.isLoadingMore] values
 * @param testSetup The test setup to be run before waiting for all values to be observed
 *
 */
private fun <T> PagedListWrapper<T>.testObservedDistinctValues(
    lifecycle: LifecycleOwner,
    expectedIsEmptyValues: List<IsEmptyValue>,
    expectedIsFetchingFirstPageValues: List<IsFetchingFirstPageValue>,
    expectedIsLoadingMoreValues: List<IsLoadingMoreValue>,
    testSetup: () -> Unit
) {
    val countDownLatch = CountDownLatch(3)
    val done = { countDownLatch.countDown() }
    this.isEmpty.testObservedDistinctValues(
            lifecycle = lifecycle,
            expectedValues = expectedIsEmptyValues.iterator(),
            assertionMessage = createAssertionMessage("IsEmpty"),
            done = done
    )
    this.isFetchingFirstPage.testObservedDistinctValues(
            lifecycle = lifecycle,
            expectedValues = expectedIsFetchingFirstPageValues.iterator(),
            assertionMessage = createAssertionMessage("IsFetchingFirstPage"),
            done = done
    )
    this.isLoadingMore.testObservedDistinctValues(
            lifecycle = lifecycle,
            expectedValues = expectedIsLoadingMoreValues.iterator(),
            assertionMessage = createAssertionMessage("IsLoadingMore"),
            done = done
    )
    testSetup()
    assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
}

private fun <T> PagedListWrapper<T>.triggerLoadMore() {
    val lifecycle = SimpleTestLifecycle()
    this.data.observe(lifecycle, Observer {
        if (it != null && it.size > 0) {
            // `PagedList.loadAround` is used with the final item index to trigger loading more data from remote
            it.loadAround(it.size - 1)
            lifecycle.destroy()
        }
    })
}

/**
 * A helper function that compares the observed values with the expected ones until the given list runs out.
 *
 * If the observed event is the same as the last one, it'll be ignored. See [ignoreIfSame] for details.
 *
 * @param lifecycle [LifecycleOwner] the [LiveData] will be observed on.
 * @param expectedValues List of expected values
 * @param assertionMessage Assertion message that'll be used during comparison to generate meaningful errors
 * @param done Callback to be called when all events in the [expectedValues] are observed
 */
private fun <T, OV : ObservedValue<T>> LiveData<T>.testObservedDistinctValues(
    lifecycle: LifecycleOwner,
    expectedValues: Iterator<OV>,
    assertionMessage: String,
    done: () -> Unit
) {
    this.ignoreIfSame().observe(lifecycle, Observer { actual ->
        val expected = expectedValues.next().value
        assertEquals(assertionMessage, expected, actual)
        if (!expectedValues.hasNext()) {
            done()
        }
    })
}

/**
 * A helper function that filters out an event if it's the same as the last one.
 */
private fun <T> LiveData<T>.ignoreIfSame(): LiveData<T> {
    val mediatorLiveData: MediatorLiveData<T> = MediatorLiveData()
    var lastValue: T? = null
    mediatorLiveData.addSource(this) {
        // TODO: What happens if the equals() is not implemented correctly for type T
        if (it != lastValue) {
            lastValue = it
            mediatorLiveData.postValue(it)
        }
    }
    return mediatorLiveData
}

/**
 * A simple helper [LifecycleOwner] implementation to be used in tests.
 *
 * It marks it's state as `RESUMED` as soon as it's created.
 */
private class SimpleTestLifecycle : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
        lifecycleRegistry.markState(Lifecycle.State.RESUMED)
    }

    /**
     * A function that marks the lifecycle state to `DESTROYED`.
     */
    fun destroy() {
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
    }
}

/**
 * A simple helper method to create assertion messages for comparing expected and observed values.
 *
 * @param type The type of the compared values that will be used to create a more meaningful assertion message.
 */
private fun createAssertionMessage(type: String): String = "Wrong value observed for $type ->"
