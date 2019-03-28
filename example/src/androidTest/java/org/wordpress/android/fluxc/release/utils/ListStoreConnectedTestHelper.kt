package org.wordpress.android.fluxc.release.utils

import android.arch.lifecycle.Lifecycle
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.datastore.ListItemDataStoreInterface
import org.wordpress.android.fluxc.release.utils.IsEmptyValue.EMPTY
import org.wordpress.android.fluxc.release.utils.IsEmptyValue.NOT_EMPTY
import org.wordpress.android.fluxc.release.utils.IsFetchingFirstPageValue.FETCHING_FIRST_PAGE
import org.wordpress.android.fluxc.release.utils.IsFetchingFirstPageValue.NOT_FETCHING_FIRST_PAGE
import org.wordpress.android.fluxc.release.utils.IsLoadingMoreValue.LOADING_MORE
import org.wordpress.android.fluxc.release.utils.IsLoadingMoreValue.NOT_LOADING_MORE
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.MultiplePages
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.SinglePage
import org.wordpress.android.fluxc.store.ListStore

/**
 * A helper class that makes writing connected tests for [ListStore] dead easy. It also makes it very easy to
 * update/improve every connected test for [ListStore] from a single place.
 */
internal class ListStoreConnectedTestHelper(private val listStore: ListStore) {
    /**
     * A helper function that returns the list from [ListStore] for the given [ListDescriptor] and
     * [ListItemDataStoreInterface].
     *
     * It uses a default [Lifecycle] instance which will NOT be destroyed throughout the test.
     *
     * @param listDescriptor [ListDescriptor] instance to be used to retrieve the list
     * @param dataStore [ListItemDataStoreInterface] instance to be used to retrieve the list
     * @param lifecycle [Lifecycle] instance to be used to retrieve the list.
     */
    fun <LD : ListDescriptor, ID, T> getList(
        listDescriptor: LD,
        dataStore: ListItemDataStoreInterface<LD, ID, T>,
        lifecycle: Lifecycle = SimpleTestLifecycle().lifecycle
    ): PagedListWrapper<T> {
        return listStore.getList(
                listDescriptor = listDescriptor,
                dataStore = dataStore,
                lifecycle = lifecycle
        )
    }

    /**
     * A helper function that tests the given [PagedListWrapper] depending on the given [ListStoreConnectedTestMode].
     *
     * This function takes a [PagedListWrapper] generator instead of a [PagedListWrapper] instance to discourage any
     * attempt of setting up the given [PagedListWrapper] and convey the intend that the setup will be performed
     * by this helper instead.
     *
     * @param createPagedListWrapper A factory function that will create a [PagedListWrapper] instance.
     */
    fun <T> runTest(
        testMode: ListStoreConnectedTestMode,
        createPagedListWrapper: () -> PagedListWrapper<T>
    ) {
        val pagedListWrapper = createPagedListWrapper()
        when (testMode) {
            is SinglePage -> testFirstPage(pagedListWrapper, testMode.ensureListIsNotEmpty)
            MultiplePages -> testLoadMore(pagedListWrapper)
        }
    }

    /**
     * A helper function that initially fetches the first page of a list and then triggers loading more data to assert
     * that correct values are observed for the given [PagedListWrapper].
     */
    private fun <T> testLoadMore(pagedListWrapper: PagedListWrapper<T>) {
        testFirstPage(pagedListWrapper, true)
        pagedListWrapper.testExpectedListWrapperStateChanges(
                expectedIsEmptyValues = listOf(NOT_EMPTY),
                expectedIsFetchingFirstPageValues = listOf(NOT_FETCHING_FIRST_PAGE),
                expectedIsLoadingMoreValues = listOf(NOT_LOADING_MORE, LOADING_MORE, NOT_LOADING_MORE)
        ) {
            pagedListWrapper.triggerLoadMore()
        }
    }

    /**
     * A helper function that fetches the first page of a list and asserts that correct values are observed for the
     * given [PagedListWrapper].
     *
     * @param pagedListWrapper [PagedListWrapper] instance that will be tested
     */
    private fun <T> testFirstPage(pagedListWrapper: PagedListWrapper<T>, ensureListIsNotEmpty: Boolean) {
        val isEmptyValues = if (ensureListIsNotEmpty) listOf(EMPTY, NOT_EMPTY) else listOf(EMPTY)
        pagedListWrapper.testExpectedListWrapperStateChanges(
                expectedIsEmptyValues = isEmptyValues,
                // TODO: Initial value should be NOT_FETCHING_FIRST_PAGE, but this is not posted by
                // TODO: `PagedListWrapper`
                expectedIsFetchingFirstPageValues = listOf(FETCHING_FIRST_PAGE, NOT_FETCHING_FIRST_PAGE),
                expectedIsLoadingMoreValues = listOf(NOT_LOADING_MORE)
        ) {
            pagedListWrapper.fetchFirstPage()
        }
    }
}
