package org.wordpress.android.fluxc.release.utils

import org.wordpress.android.fluxc.model.list.ListConfig

private const val TEST_LIST_NETWORK_PAGE_SIZE = 5
private const val TEST_LIST_INITIAL_LOAD_SIZE = 10
private const val TEST_LIST_DB_PAGE_SIZE = 5
private const val TEST_LIST_PRE_FETCH_DISTANCE = TEST_LIST_DB_PAGE_SIZE * 3

/**
 * A [ListConfig] instance that has smaller values than the default config to make testing lists less demanding.
 */
internal val TEST_LIST_CONFIG = ListConfig(
        networkPageSize = TEST_LIST_NETWORK_PAGE_SIZE,
        initialLoadSize = TEST_LIST_INITIAL_LOAD_SIZE,
        dbPageSize = TEST_LIST_DB_PAGE_SIZE,
        prefetchDistance = TEST_LIST_PRE_FETCH_DISTANCE
)

/**
 * A sealed class to be provided to [ListStoreConnectedTestHelper] to describe the expectations from a test.
 */
internal sealed class ListStoreConnectedTestMode {
    /**
     * Retrieving a single page of data is enough to pass a test in this test mode.
     */
    class SinglePage(val ensureListIsNotEmpty: Boolean = false) : ListStoreConnectedTestMode()

    /**
     * Multiple pages of data needs to be fetched in order to pass a test in this test mode.
     */
    object MultiplePages : ListStoreConnectedTestMode()
}
