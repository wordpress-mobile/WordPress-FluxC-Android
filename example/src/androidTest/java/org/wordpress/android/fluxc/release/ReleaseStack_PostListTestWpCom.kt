package org.wordpress.android.fluxc.release

import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy
import org.wordpress.android.fluxc.model.list.datastore.PostListDataStore
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.DEFAULT_POST_STATUS_LIST
import javax.inject.Inject

internal class RestPostListTestCase(
    val statusList: List<PostStatus> = DEFAULT_POST_STATUS_LIST,
    val order: ListOrder = ListOrder.DESC,
    val orderBy: PostListOrderBy = PostListOrderBy.DATE
)

@RunWith(Parameterized::class)
internal class ReleaseStack_PostListTestWpCom(
    private val testCase: RestPostListTestCase
) : ReleaseStack_WPComBase() {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): List<RestPostListTestCase> = listOf(
                RestPostListTestCase()
        )
    }

    private val listStoreConnectedTestHelper by lazy {
        ListStoreConnectedTestHelper(listStore)
    }

    private val postListDataStore by lazy {
        PostListDataStore(mDispatcher, postStore, sSite)
    }

    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Test
    fun testFetchFirstPageForDefaultDescriptor() {
        listStoreConnectedTestHelper.testFetchFirstPage(this::createPagedListWrapper)
    }

    @Test
    fun testLoadMoreForDefaultDescriptor() {
        listStoreConnectedTestHelper.testLoadMore(this::createPagedListWrapper)
    }

    private fun createPagedListWrapper(): PagedListWrapper<PostModel> {
        val descriptor = PostListDescriptorForRestSite(
                site = sSite,
                statusList = testCase.statusList,
                order = testCase.order,
                orderBy = testCase.orderBy,
                config = TEST_LIST_CONFIG
        )
        return listStoreConnectedTestHelper.getList(descriptor, postListDataStore)
    }
}
