package org.wordpress.android.fluxc.release

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestHelper
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode
import org.wordpress.android.fluxc.release.utils.ListStoreConnectedTestMode.SinglePage
import org.wordpress.android.fluxc.release.utils.TestWCOrderListDataSource
import org.wordpress.android.fluxc.release.utils.TestWCOrderUIItem
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

internal class WCOrderListTestCase(
    val statusFilter: String? = null,
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
        @JvmStatic
        @Parameters
        fun testCases(): List<WCOrderListTestCase> = listOf(
                WCOrderListTestCase(testMode = SinglePage(ensureListIsNotEmpty = true))
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
    fun test() {
        listStoreConnectedTestHelper.runTest(testCase.testMode, this::createPagedListWrapper)
    }

    private fun createPagedListWrapper(): PagedListWrapper<TestWCOrderUIItem> {
        val descriptor = WCOrderListDescriptor(
                site = sSite
        )
        return listStoreConnectedTestHelper.getList(descriptor, TestWCOrderListDataSource(mDispatcher))
    }
}
