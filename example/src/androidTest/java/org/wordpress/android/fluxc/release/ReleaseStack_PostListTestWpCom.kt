package org.wordpress.android.fluxc.release

import org.junit.Test
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy
import org.wordpress.android.fluxc.model.post.PostStatus

class ReleaseStack_PostListTestWpCom : ReleaseStack_WPComBase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchFirstPageForDefaultDescriptor() {
        fetchFirstPage(PostListDescriptorForRestSite(sSite))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testLoadMoreForDefaultDescriptor() {
        loadMore(PostListDescriptorForRestSite(sSite, pageSize = 10))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchPublishedPosts() {
        fetchFirstPage(PostListDescriptorForRestSite(sSite, statusList = listOf(PostStatus.PUBLISHED)))
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchDraftsOrderedByTitle() {
        fetchFirstPage(
                PostListDescriptorForRestSite(
                        site = sSite,
                        statusList = listOf(PostStatus.DRAFT),
                        orderBy = PostListOrderBy.TITLE
                )
        )
    }

    @Throws(InterruptedException::class)
    @Test
    fun testFetchTrashedPostsOrderedByIdInDescendingOrder() {
        fetchFirstPage(
                PostListDescriptorForRestSite(
                        site = sSite,
                        statusList = listOf(PostStatus.TRASHED),
                        orderBy = PostListOrderBy.ID,
                        order = ListOrder.DESC
                )
        )
    }

    private fun fetchFirstPage(listDescriptor: PostListDescriptor) {
        val postListConnectedTestHelper = PostListConnectedTestHelper(mDispatcher, mReleaseStackAppComponent)
        postListConnectedTestHelper.fetchFirstPageHelper(listDescriptor)
    }

    private fun loadMore(listDescriptor: PostListDescriptor) {
        val postListConnectedTestHelper = PostListConnectedTestHelper(mDispatcher, mReleaseStackAppComponent)
        postListConnectedTestHelper.loadMoreHelper(listDescriptor)
    }
}
