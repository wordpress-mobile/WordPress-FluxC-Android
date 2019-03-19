package org.wordpress.android.fluxc.release.utils

import android.arch.lifecycle.Observer
import org.junit.Assert.assertTrue
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A simple [Boolean] wrapper to represent [PagedListWrapper.isEmpty] values.
 *
 * We should't directly use listOf(false, true, false) values, because it doesn't carry intend. This wrapper makes our
 * tests more meaningful and less error prone.
 */
internal enum class IsEmptyValue(override val value: Boolean) : ObservedValue<Boolean> {
    EMPTY(true),
    NOT_EMPTY(false);
}

/**
 * A simple [Boolean] wrapper to represent [PagedListWrapper.isFetchingFirstPage] values.
 *
 * We should't directly use listOf(false, true, false) values, because it doesn't carry intend. This wrapper makes our
 * tests more meaningful and less error prone.
 */
internal enum class IsFetchingFirstPageValue(override val value: Boolean) : ObservedValue<Boolean> {
    FETCHING_FIRST_PAGE(true),
    NOT_FETCHING_FIRST_PAGE(false);
}

/**
 * A simple [Boolean] wrapper to represent [PagedListWrapper.isLoadingMore] values.
 *
 * We should't directly use listOf(false, true, false) values, because it doesn't carry intend. This wrapper makes our
 * tests more meaningful and less error prone.
 */
internal enum class IsLoadingMoreValue(override val value: Boolean) : ObservedValue<Boolean> {
    LOADING_MORE(true),
    NOT_LOADING_MORE(false);
}

/**
 * A helper function that compares the observed values with the expected ones for the given [PagedListWrapper] until
 * the given lists run out.
 *
 * @param expectedIsEmptyValues List of expected [PagedListWrapper.isEmpty] values
 * @param expectedIsFetchingFirstPageValues List of expected [PagedListWrapper.isFetchingFirstPage] values
 * @param expectedIsLoadingMoreValues List of expected [PagedListWrapper.isLoadingMore] values
 * @param testSetup The test setup to be run before waiting for all values to be observed
 *
 */
internal fun <T> PagedListWrapper<T>.testExpectedListWrapperStateChanges(
    expectedIsEmptyValues: List<IsEmptyValue>,
    expectedIsFetchingFirstPageValues: List<IsFetchingFirstPageValue>,
    expectedIsLoadingMoreValues: List<IsLoadingMoreValue>,
    testSetup: () -> Unit
) {
    val countDownLatch = CountDownLatch(3)
    val done = { countDownLatch.countDown() }
    this.isEmpty.testObservedDistinctValues(
            expectedValues = expectedIsEmptyValues.iterator(),
            assertionMessage = createAssertionMessage("IsEmpty"),
            onFinish = done
    )
    this.isFetchingFirstPage.testObservedDistinctValues(
            expectedValues = expectedIsFetchingFirstPageValues.iterator(),
            assertionMessage = createAssertionMessage("IsFetchingFirstPage"),
            onFinish = done
    )
    this.isLoadingMore.testObservedDistinctValues(
            expectedValues = expectedIsLoadingMoreValues.iterator(),
            assertionMessage = createAssertionMessage("IsLoadingMore"),
            onFinish = done
    )
    testSetup()
    assertTrue(
            "The test timed out before observing all expected values!",
            countDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
    )
}

/**
 * A helper function that triggers load more call for the [PagedListWrapper] to make it easy to test.
 *
 * It uses an internal lifecycle to observe the [PagedListWrapper.data] and calls `loadAround` on it with the last
 * item's index to trigger loading more data from remote. It'll then destroy the lifecycle so it doesn't keep
 * fetching more pages.
 */
internal fun <T> PagedListWrapper<T>.triggerLoadMore() {
    val lifecycle = SimpleTestLifecycle()
    this.data.observe(lifecycle, Observer {
        if (it != null && it.size > 0) {
            it.loadAround(it.size - 1)
            lifecycle.destroy()
        }
    })
}

/**
 * A simple helper method to create assertion messages for comparing expected and observed values.
 *
 * @param type The type of the compared values that will be used to create a more meaningful assertion message.
 */
private fun createAssertionMessage(type: String): String = "Wrong value observed for $type ->"
