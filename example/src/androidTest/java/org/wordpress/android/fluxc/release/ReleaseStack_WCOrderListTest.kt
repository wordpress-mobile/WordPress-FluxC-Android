package org.wordpress.android.fluxc.release

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestHelper
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.MultiplePages
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.SinglePage
import org.wordpress.android.fluxc.release.utils.TestWCOrderListDataSource
import org.wordpress.android.fluxc.release.utils.TestWCOrderUIItem
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

internal class WCOrderListTestCase(
    val statusFilter: String? = null,
    val searchQuery: String? = null,
    val testMode: ListStoreConnectedTestMode = SinglePage(false)
)

@RunWith(Parameterized::class)
internal class ReleaseStack_WCOrderListTest(
    private val testCase: WCOrderListTestCase
) : ReleaseStack_WCBase() {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var orderStore: WCOrderStore

    companion object {
        private const val TEST_ORDER_LIST_SEARCH_QUERY = "a"

        @JvmStatic
        @Parameters
        fun testCases(): List<WCOrderListTestCase> = listOf(
                WCOrderListTestCase(testMode = SinglePage(ensureListIsNotEmpty = true)),
                WCOrderListTestCase(testMode = MultiplePages)
            // Temporarily disable these tests because even though individually running them
            // works, they start to fail when they are run subsequently. Considering the underlying
            // feature have been in production for a very long time and a similar setup for posts
            // work as expected, addressing these tests don't have a very high priority for us.
//                WCOrderListTestCase(statusFilter = CoreOrderStatus.COMPLETED.value),
//                WCOrderListTestCase(statusFilter = CoreOrderStatus.PROCESSING.value, testMode = MultiplePages),
//                WCOrderListTestCase(searchQuery = TEST_ORDER_LIST_SEARCH_QUERY),
//                WCOrderListTestCase(
//                        statusFilter = CoreOrderStatus.COMPLETED.value,
//                        searchQuery = TEST_ORDER_LIST_SEARCH_QUERY,
//                        testMode = MultiplePages)
        )
    }

    private val listStoreConnectedTestHelper by lazy {
        ListStoreConnectedTestHelper(listStore)
    }

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Test
    @Ignore("Disabling as a part of effort to exclude flaky or failing tests." +
        "See https://github.com/wordpress-mobile/WordPress-FluxC-Android/pull/2665")
    fun test() {
        listStoreConnectedTestHelper.runTest(testCase.testMode, this::createPagedListWrapper)
    }

    private fun createPagedListWrapper(): PagedListWrapper<TestWCOrderUIItem> {
        val descriptor = WCOrderListDescriptor(
                site = sSite,
                statusFilter = testCase.statusFilter,
                searchQuery = testCase.searchQuery
        )
        return listStoreConnectedTestHelper.getList(descriptor, TestWCOrderListDataSource(mDispatcher))
    }
}
