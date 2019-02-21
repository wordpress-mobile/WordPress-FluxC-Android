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

internal class ListStoreConnectedTestHelper(
    private val listStore: ListStore,
    private val lifecycle: Lifecycle = SimpleTestLifecycle().lifecycle
) {
    fun <T> getList(
        listDescriptor: ListDescriptor,
        dataStore: ListDataStoreInterface<T>
    ): PagedListWrapper<T> {
        return listStore.getList(
                listDescriptor = listDescriptor,
                dataStore = dataStore,
                lifecycle = lifecycle,
                transform = { x -> x }
        )
    }

    fun <T> testFetchFirstPage(createPagedListWrapper: () -> PagedListWrapper<T>) {
        fetchFirstPageAndAssert(createPagedListWrapper())
    }

    fun <T> testLoadMore(createPagedListWrapper: () -> PagedListWrapper<T>) = runWithSimpleLifecycle { testLifecycle ->
        val pagedListWrapper = createPagedListWrapper()
        fetchFirstPageAndAssert(pagedListWrapper)
        pagedListWrapper.testObservedDistinctValues(
                lifecycle = testLifecycle,
                expectedIsEmptyValues = listOf(NOT_EMPTY),
                expectedIsFetchingFirstPageValues = listOf(NOT_FETCHING_FIRST_PAGE),
                expectedIsLoadingMoreValues = listOf(NOT_LOADING_MORE, LOADING_MORE, NOT_LOADING_MORE)
        ) {
            // TODO: Cleanup how we trigger load more data
            var alreadyObserved = false
            pagedListWrapper.data.observeForever {
                if (!alreadyObserved && it != null && it.size > 0) {
                    it.loadAround(it.size - 1)
                    alreadyObserved = true
                }
            }
        }
    }

    private fun <T> fetchFirstPageAndAssert(pagedListWrapper: PagedListWrapper<T>) =
            runWithSimpleLifecycle { testLifecycle ->
                pagedListWrapper.testObservedDistinctValues(
                        lifecycle = testLifecycle,
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

private fun runWithSimpleLifecycle(
    lifecycle: SimpleTestLifecycle = SimpleTestLifecycle(),
    runTest: (LifecycleOwner) -> Unit
) {
    runTest(lifecycle)
    lifecycle.destroy()
}

private interface ObservedValue<T> {
    val value: T
}

private enum class IsEmptyValue(override val value: Boolean) : ObservedValue<Boolean> {
    EMPTY(true),
    NOT_EMPTY(false);
}

private enum class IsFetchingFirstPageValue(override val value: Boolean) : ObservedValue<Boolean> {
    FETCHING_FIRST_PAGE(true),
    NOT_FETCHING_FIRST_PAGE(false);
}

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
 * @param setup
 *
 */
private fun <T> PagedListWrapper<T>.testObservedDistinctValues(
    lifecycle: LifecycleOwner,
    expectedIsEmptyValues: List<IsEmptyValue>,
    expectedIsFetchingFirstPageValues: List<IsFetchingFirstPageValue>,
    expectedIsLoadingMoreValues: List<IsLoadingMoreValue>,
    setup: () -> Unit
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
    setup()
    assertTrue(countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))
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
