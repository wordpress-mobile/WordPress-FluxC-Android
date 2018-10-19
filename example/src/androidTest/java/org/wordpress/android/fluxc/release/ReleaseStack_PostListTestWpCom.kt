package org.wordpress.android.fluxc.release

import org.junit.Test
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite

class ReleaseStack_PostListTestWpCom : ReleaseStack_WPComBase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        init()
    }

    @Throws(InterruptedException::class)
    @Test
    fun fetchFirstPage() {
        val postListConnectedTestHelper = PostListConnectedTestHelper(mDispatcher, mReleaseStackAppComponent)
        val postListDescriptor = PostListDescriptorForRestSite(sSite)
        postListConnectedTestHelper.fetchFirstPageHelper(postListDescriptor)
    }

    @Throws(InterruptedException::class)
    @Test
    fun loadMore() {
        val postListConnectedTestHelper = PostListConnectedTestHelper(mDispatcher, mReleaseStackAppComponent)
        val postListDescriptor = PostListDescriptorForRestSite(sSite, pageSize = 10)
        postListConnectedTestHelper.loadMoreHelper(postListDescriptor)
    }
}
