package org.wordpress.android.fluxc.release

import org.junit.Test
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite

class ReleaseStack_PostListTestXMLRPC : ReleaseStack_XMLRPCBase() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
    }

    @Throws(InterruptedException::class)
    @Test
    fun fetchFirstPage() {
        val postListConnectedTestHelper = PostListConnectedTestHelper(mDispatcher, mReleaseStackAppComponent)
        val postListDescriptor = PostListDescriptorForXmlRpcSite(sSite)
        postListConnectedTestHelper.fetchFirstPageHelper(postListDescriptor)
    }

    @Throws(InterruptedException::class)
    @Test
    fun loadMore() {
        val postListConnectedTestHelper = PostListConnectedTestHelper(mDispatcher, mReleaseStackAppComponent)
        val postListDescriptor = PostListDescriptorForXmlRpcSite(sSite, pageSize = 10)
        postListConnectedTestHelper.loadMoreHelper(postListDescriptor)
    }
}
